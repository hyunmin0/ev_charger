import json
import logging
from datetime import timedelta
from sqlalchemy import text
from db import AsyncSessionLocal, DB_ERRORS

logger = logging.getLogger(__name__)

# congestion 테이블에 들어있는 예측 시점(targetTime, 시간 단위)의 상한. 3시간 모델이 올라오면 3으로 맞춰 둔 값
MAX_PREDICTION_HOURS = 3

# 한 번에 조회할 수 있는 충전소 수 (충전소 검색 tool의 결과 상한과 같음)
MAX_STATIONS = 10

# 예측 배치가 멈춰서 오래된 값이 나가는 걸 알아채기 위한 기준. 배치 주기를 확인하면 조정
STALE_AFTER_MINUTES = 180

GET_CONGESTION_FORECAST_SCHEMA = {
    "type": "function",
    "function": {
        "name": "get_congestion_forecast",
        "description": (
            f"충전소의 {MAX_PREDICTION_HOURS}시간 이내 미래 혼잡도(여유/보통/혼잡) 예측을 조회합니다. "
            "충전소 ID(statId)가 필요하며, 모르면 먼저 search_stations로 찾으세요. "
            f"{MAX_PREDICTION_HOURS}시간보다 먼 시점은 예측하지 않습니다."
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
                    "description": f"몇 시간 뒤의 혼잡도인지 (1~{MAX_PREDICTION_HOURS})",
                },
            },
            "required": ["station_ids", "hours_ahead"],
        },
    },
}


def _fmt(dt) -> str:
    return dt.strftime("%Y-%m-%d %H:%M")


async def get_congestion_forecast(station_ids: list[str], hours_ahead: int) -> str:
    if not station_ids:
        return json.dumps({"error": "station_ids가 비어 있습니다"}, ensure_ascii=False)
    if len(station_ids) > MAX_STATIONS:
        return json.dumps(
            {"error": f"한 번에 최대 {MAX_STATIONS}개 충전소까지만 조회할 수 있습니다"},
            ensure_ascii=False,
        )
    if not 1 <= hours_ahead <= MAX_PREDICTION_HOURS:
        return json.dumps(
            {
                "supported": False,
                "max_hours_ahead": MAX_PREDICTION_HOURS,
                "message": f"혼잡도 예측은 {MAX_PREDICTION_HOURS}시간 이내까지만 제공합니다.",
            },
            ensure_ascii=False,
        )

    # 배치가 쌓이더라도 충전소별 가장 최근 예측만 사용
    # predictedAt은 한국 시간(KST)으로 저장돼 있어서 UTC인 now()를 KST로 바꿔 비교
    query = text("""
        SELECT DISTINCT ON (c."statId")
            c."statId",
            s."statNm",
            c."congestionLevel",
            c."congestionScore",
            c."predictedAt",
            EXTRACT(EPOCH FROM ((now() AT TIME ZONE 'Asia/Seoul') - c."predictedAt")) / 60 AS age_minutes
        FROM congestion c
        JOIN station s ON s."statId" = c."statId"
        WHERE c."statId" = ANY(:station_ids)
          AND c."targetTime" = :hours_ahead
        ORDER BY c."statId", c."predictedAt" DESC
    """)

    try:
        async with AsyncSessionLocal() as session:
            result = await session.execute(query, {"station_ids": station_ids, "hours_ahead": hours_ahead})
            rows = result.mappings().all()
    except DB_ERRORS:
        logger.exception("get_congestion_forecast DB 조회 실패 (%s, %sh)", station_ids, hours_ahead)
        return json.dumps(
            {"error": "혼잡도 예측을 조회하지 못했습니다. 잠시 후 다시 시도해 주세요."},
            ensure_ascii=False,
        )

    forecasts = [
        {
            "statId": row["statId"],
            "statNm": row["statNm"],
            "congestion_level": row["congestionLevel"],
            "congestion_score": round(row["congestionScore"], 2) if row["congestionScore"] is not None else None,
            "predicted_at": _fmt(row["predictedAt"]),
            "target_at": _fmt(row["predictedAt"] + timedelta(hours=hours_ahead)),
        }
        for row in rows
    ]
    found_ids = {f["statId"] for f in forecasts}

    return json.dumps(
        {
            "hours_ahead": hours_ahead,
            "forecasts": forecasts,
            "missing_station_ids": [sid for sid in station_ids if sid not in found_ids],
            "stale": bool(rows) and max(row["age_minutes"] for row in rows) > STALE_AFTER_MINUTES,
        },
        ensure_ascii=False,
    )
