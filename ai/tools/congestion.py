import json
import logging
from datetime import datetime, timedelta, timezone
from sqlalchemy import text
from db import AsyncSessionLocal, DB_ERRORS

logger = logging.getLogger(__name__)

# congestion 테이블에 들어있는 예측 시점(targetTime, 시간 단위)의 상한. 프롬프트의 "3시간 이내" 문구와 같이 고칠 것
MAX_PREDICTION_HOURS = 3

# 예측 범위를 넘는 시점은 congestion_avg(같은 요일·시간대의 평균)로 답한다. 평균은 일주일 안의 시점까지만 의미가 있음
MAX_AVERAGE_HOURS = 24 * 7

# congestion_avg를 만드는 배치가 쓰는 기간(주). 배치 기간을 바꾸면 이 값과 프롬프트의 "4주"를 같이 고칠 것
AVERAGE_WINDOW_WEEKS = 4

# 한 번에 조회할 수 있는 충전소 수 (충전소 검색 tool의 결과 상한과 같음)
MAX_STATIONS = 10

# 예측 배치가 멈춰서 오래된 값이 나가는 걸 알아채기 위한 기준. 배치 주기를 확인하면 조정
STALE_AFTER_MINUTES = 180

# 한국은 서머타임이 없어서 고정 오프셋으로 충분 (서버 환경의 시간대나 tzdata에 의존하지 않음)
KST = timezone(timedelta(hours=9))

# congestion_avg.levels의 문자 → 등급
AVG_LEVELS = {"0": "여유", "1": "보통", "2": "혼잡"}
WEEKDAY_KO = ["월", "화", "수", "목", "금", "토", "일"]

GET_CONGESTION_FORECAST_SCHEMA = {
    "type": "function",
    "function": {
        "name": "get_congestion_forecast",
        "description": (
            f"충전소의 미래 혼잡도(여유/보통/혼잡)를 조회합니다. 지금부터 {MAX_PREDICTION_HOURS}시간 이내는 예측값(source=prediction), "
            f"{MAX_PREDICTION_HOURS + 1}시간~{MAX_AVERAGE_HOURS}시간(일주일) 뒤는 그 시점과 같은 요일·시간대의 최근 {AVERAGE_WINDOW_WEEKS}주 평균(source=average)을 돌려줍니다. "
            f"{MAX_PREDICTION_HOURS}시간 이내라도 예측이 없는 충전소는 평균을 대신 돌려줍니다(average_fallback). "
            "충전소 ID(statId)가 필요하며, 모르면 먼저 search_stations로 찾으세요."
        ),
        "parameters": {
            "type": "object",
            "properties": {
                "station_ids": {
                    "type": "array",
                    "description": f"충전소 ID(statId) 목록, 최대 {MAX_STATIONS}개. 검색 결과의 statId를 그대로 사용",
                    "items": {"type": "string"},
                },
                "hours_ahead": {
                    "type": "integer",
                    "description": (
                        f"지금부터 몇 시간 뒤의 혼잡도인지 (1~{MAX_AVERAGE_HOURS}). 시스템 프롬프트의 현재 시각과 사용자가 말한 시각의 차이로 계산. "
                        f"사용자가 시점을 말하지 않았거나 '나중에'처럼 모호하면 비워 두면 1~{MAX_PREDICTION_HOURS}시간 예측을 모두 돌려줍니다"
                    ),
                },
            },
            "required": ["station_ids"],
        },
    },
}

# 평균 한 칸 읽기: (시점 h, 칸 위치 idx) 쌍마다 충전소들의 scores[idx], levels[idx]를 꺼낸다
_AVERAGE_SQL = text("""
    SELECT a."statId", s."statNm", a.n_weeks, u.h AS hours_ahead,
           a.scores[u.idx] AS score, substr(a.levels, u.idx, 1) AS level_code
    FROM congestion_avg a
    JOIN station s ON s."statId" = a."statId"
    CROSS JOIN unnest(CAST(:hours AS int[]), CAST(:idxs AS int[])) AS u(h, idx)
    WHERE a."statId" = ANY(:station_ids)
    ORDER BY a."statId", u.h
""")


def _fmt(dt) -> str:
    return dt.strftime("%Y-%m-%d %H:%M")


def _targets(hours: list[int]) -> dict[int, dict]:
    """지금(한국 시간)에서 N시간 뒤의 목표 시각과 congestion_avg 칸 위치.
    칸 위치 = (요일-1)*24 + 시 + 1 (요일 월=1…일=7, 시 0~23). 배치가 이 규칙으로 만든다"""
    now = datetime.now(KST)
    result = {}
    for h in hours:
        ts = now + timedelta(hours=h)
        result[h] = {
            "idx": (ts.isoweekday() - 1) * 24 + ts.hour + 1,
            "target_at": f"{ts:%Y-%m-%d} {ts:%H}시",
            "target_weekday": WEEKDAY_KO[ts.weekday()],
        }
    return result


async def _average_rows(session, station_ids: list[str], targets: dict[int, dict]):
    hours = list(targets)
    result = await session.execute(
        _AVERAGE_SQL,
        {"station_ids": station_ids, "hours": hours, "idxs": [targets[h]["idx"] for h in hours]},
    )
    return result.mappings().all()


def _average_entry(row, target: dict) -> dict:
    return {
        "hours_ahead": row["hours_ahead"],
        "target_at": target["target_at"],
        "target_weekday": target["target_weekday"],
        "congestion_level": AVG_LEVELS.get(row["level_code"]),  # '-'(칸이 비어 있음)이면 None
        "congestion_score": round(row["score"], 2) if row["score"] is not None else None,
    }


def _average_station(row) -> dict:
    return {
        "statId": row["statId"],
        "statNm": row["statNm"],
        # 충전소마다 이력이 짧을 수 있어서 기준값(4주)이 아니라 실제로 평균에 쓴 주 수를 내려준다
        "weeks_used": row["n_weeks"],
        "short_history": row["n_weeks"] < AVERAGE_WINDOW_WEEKS,
    }


async def get_congestion_forecast(station_ids: list[str], hours_ahead: int | None = None) -> str:
    if not station_ids:
        return json.dumps({"error": "station_ids가 비어 있습니다"}, ensure_ascii=False)
    if len(station_ids) > MAX_STATIONS:
        return json.dumps(
            {"error": f"한 번에 최대 {MAX_STATIONS}개 충전소까지만 조회할 수 있습니다"},
            ensure_ascii=False,
        )

    if hours_ahead is None:
        return await _predictions(station_ids, list(range(1, MAX_PREDICTION_HOURS + 1)))
    if 1 <= hours_ahead <= MAX_PREDICTION_HOURS:
        return await _predictions(station_ids, [hours_ahead])
    if MAX_PREDICTION_HOURS < hours_ahead <= MAX_AVERAGE_HOURS:
        return await _averages(station_ids, hours_ahead)
    return json.dumps(
        {
            "supported": False,
            "max_hours_ahead": MAX_AVERAGE_HOURS,
            "message": f"혼잡도는 지금부터 {MAX_AVERAGE_HOURS}시간(일주일) 이내 시점까지만 안내할 수 있습니다.",
        },
        ensure_ascii=False,
    )


async def _predictions(station_ids: list[str], hours: list[int]) -> str:
    # 배치가 쌓이더라도 충전소·시점별 가장 최근 예측만 사용
    # predictedAt은 한국 시간(KST)으로 저장돼 있어서 UTC인 now()를 KST로 바꿔 비교
    query = text("""
        SELECT DISTINCT ON (c."statId", c."targetTime")
            c."statId",
            s."statNm",
            c."targetTime",
            c."congestionLevel",
            c."congestionScore",
            c."predictedAt",
            EXTRACT(EPOCH FROM ((now() AT TIME ZONE 'Asia/Seoul') - c."predictedAt")) / 60 AS age_minutes
        FROM congestion c
        JOIN station s ON s."statId" = c."statId"
        WHERE c."statId" = ANY(:station_ids)
          AND c."targetTime" = ANY(:hours)
        ORDER BY c."statId", c."targetTime", c."predictedAt" DESC
    """)

    try:
        async with AsyncSessionLocal() as session:
            result = await session.execute(query, {"station_ids": station_ids, "hours": hours})
            rows = result.mappings().all()

            # 예측이 하나도 없는 충전소는 같은 시점의 평균으로 대신 답한다 (평균이 있는 충전소만)
            predicted_ids = {row["statId"] for row in rows}
            fallback_ids = [sid for sid in station_ids if sid not in predicted_ids]
            targets = _targets(hours)
            avg_rows = await _average_rows(session, fallback_ids, targets) if fallback_ids else []
    except DB_ERRORS:
        logger.exception("get_congestion_forecast DB 조회 실패 (%s, %s)", station_ids, hours)
        return json.dumps(
            {"error": "혼잡도 예측을 조회하지 못했습니다. 잠시 후 다시 시도해 주세요."},
            ensure_ascii=False,
        )

    by_station: dict[str, dict] = {}
    for row in rows:
        station = by_station.setdefault(
            row["statId"],
            {"statId": row["statId"], "statNm": row["statNm"], "predicted_at": _fmt(row["predictedAt"]), "predictions": []},
        )
        station["predictions"].append(
            {
                "hours_ahead": row["targetTime"],
                "congestion_level": row["congestionLevel"],
                "congestion_score": round(row["congestionScore"], 2) if row["congestionScore"] is not None else None,
                "target_at": _fmt(row["predictedAt"] + timedelta(hours=row["targetTime"])),
            }
        )

    fallback: dict[str, dict] = {}
    for row in avg_rows:
        entry = fallback.setdefault(row["statId"], {**_average_station(row), "averages": []})
        entry["averages"].append(_average_entry(row, targets[row["hours_ahead"]]))

    return json.dumps(
        {
            "source": "prediction",
            "forecasts": [by_station[sid] for sid in station_ids if sid in by_station],
            # 예측은 없지만 평균은 있는 충전소. 평균이라는 점을 밝혀서 안내해야 함
            "average_fallback": [fallback[sid] for sid in station_ids if sid in fallback],
            "missing_station_ids": [sid for sid in station_ids if sid not in by_station and sid not in fallback],
            "stale": bool(rows) and max(row["age_minutes"] for row in rows) > STALE_AFTER_MINUTES,
        },
        ensure_ascii=False,
    )


async def _averages(station_ids: list[str], hours_ahead: int) -> str:
    targets = _targets([hours_ahead])
    try:
        async with AsyncSessionLocal() as session:
            rows = await _average_rows(session, station_ids, targets)
    except DB_ERRORS:
        logger.exception("get_congestion_forecast 평균 조회 실패 (%s, %sh)", station_ids, hours_ahead)
        return json.dumps(
            {"error": "혼잡도 정보를 조회하지 못했습니다. 잠시 후 다시 시도해 주세요."},
            ensure_ascii=False,
        )

    target = targets[hours_ahead]
    by_id = {row["statId"]: {**_average_station(row), **_average_entry(row, target)} for row in rows}

    return json.dumps(
        {
            "source": "average",
            "hours_ahead": hours_ahead,
            "target_at": target["target_at"],
            "target_weekday": target["target_weekday"],
            "forecasts": [by_id[sid] for sid in station_ids if sid in by_id],
            "missing_station_ids": [sid for sid in station_ids if sid not in by_id],
        },
        ensure_ascii=False,
    )
