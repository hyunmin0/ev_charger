import json
import logging
from sqlalchemy import text
from db import AsyncSessionLocal, DB_ERRORS

logger = logging.getLogger(__name__)

# charge.charger_type은 코드(01~11)가 아니라 '완속' / '급속' / '초급속' 문자열이다.
# LLM에는 완속/급속만 노출하고, 급속은 초급속(GT 모델 등)도 같이 조회한다.
SPEED_TO_DB_TYPES = {"급속": ["급속", "초급속"], "완속": ["완속"]}

# charge.minutes의 측정 구간 (low%→high%): 급속(초급속 포함) 10→80%, 완속 10→100%
REFERENCE_BAND = {"급속": (10, 80), "완속": (10, 100)}

GET_CHARGING_TIME_SCHEMA = {
    "type": "function",
    "function": {
        "name": "get_charging_time",
        "description": (
            "차량이 목표 충전량까지 도달하는 데 걸리는 시간, 또는 주어진 충전 가능 시간 동안 "
            "도달 가능한 충전량을 계산합니다. target_percent와 available_minutes 중 정확히 하나만 넘기세요. "
            "급속은 충전기 출력(kW)별로 결과가 여러 개 나올 수 있습니다."
        ),
        "parameters": {
            "type": "object",
            "properties": {
                "brand": {"type": "string", "description": "차량 제조사 (시스템 프롬프트의 차량 정보 참고)"},
                "model": {"type": "string", "description": "모델명만 (배터리 타입·연식 제외). 예: '아이오닉 5'"},
                "battery_type": {
                    "type": "string",
                    "description": "배터리 타입 (스탠다드, 롱레인지, None 등). 모델명에 섞지 말고 여기에 따로",
                },
                "model_year": {"type": "integer", "description": "연식"},
                "charger_speed": {
                    "type": "string",
                    "description": "충전 방식. 사용자가 말하지 않았으면 추측하지 말고 먼저 되물을 것",
                    "enum": ["완속", "급속"],
                },
                "current_percent": {"type": "number", "description": "현재 배터리 잔량 (%)"},
                "target_percent": {
                    "type": "number",
                    "description": "목표 충전량 (%). '완충까지 얼마나 걸려?' 같은 질문일 때 사용",
                },
                "available_minutes": {
                    "type": "number",
                    "description": "충전 가능한 시간 (분). '30분 있는데 얼마나 충전돼?' 같은 질문일 때 사용",
                },
            },
            "required": ["brand", "model", "battery_type", "model_year", "charger_speed", "current_percent"],
        },
    },
}


async def _available_options(session, brand: str) -> dict:
    # 일치하는 행이 없을 때 LLM이 표기 차이를 스스로 고쳐 재호출할 수 있게 실제 값 목록을 같이 돌려줌
    result = await session.execute(
        text("""
            SELECT DISTINCT model, battery_type, model_year
            FROM charge WHERE brand = :brand
            ORDER BY model, model_year, battery_type
        """),
        {"brand": brand},
    )
    models = [dict(r) for r in result.mappings().all()]
    if models:
        return {"available_models": models}
    result = await session.execute(text("SELECT DISTINCT brand FROM charge ORDER BY brand"))
    return {"available_brands": [r["brand"] for r in result.mappings().all()]}


async def get_charging_time(
    brand: str,
    model: str,
    battery_type: str,
    model_year: int,
    charger_speed: str,
    current_percent: float,
    target_percent: float | None = None,
    available_minutes: float | None = None,
) -> str:
    if charger_speed not in SPEED_TO_DB_TYPES:
        return json.dumps({"error": "charger_speed는 '완속' 또는 '급속'이어야 합니다"}, ensure_ascii=False)
    if (target_percent is None) == (available_minutes is None):
        return json.dumps(
            {"error": "target_percent와 available_minutes 중 정확히 하나만 넘겨야 합니다"},
            ensure_ascii=False,
        )

    query = text("""
        SELECT charger_output, minutes
        FROM charge
        WHERE brand = :brand
          AND model = :model
          AND battery_type = :battery_type
          AND model_year = :model_year
          AND charger_type = ANY(:db_types)
        ORDER BY charger_output DESC NULLS LAST
    """)
    params = {
        "brand": brand,
        "model": model,
        "battery_type": battery_type,
        "model_year": model_year,
        "db_types": SPEED_TO_DB_TYPES[charger_speed],
    }

    try:
        async with AsyncSessionLocal() as session:
            result = await session.execute(query, params)
            rows = result.mappings().all()
            if not rows:
                options_hint = await _available_options(session, brand)
    except DB_ERRORS:
        logger.exception("get_charging_time DB 조회 실패 (%s %s %s)", brand, model, model_year)
        return json.dumps(
            {"error": "충전 시간 정보를 조회하지 못했습니다. 잠시 후 다시 시도해 주세요."},
            ensure_ascii=False,
        )

    if not rows:
        return json.dumps({"found": False, **options_hint}, ensure_ascii=False)

    low, high = REFERENCE_BAND[charger_speed]
    span = high - low  # 기준% : 급속 70, 완속 90

    options = []
    end_percents = []
    for row in rows:
        reference_minutes = row["minutes"]
        if target_percent is not None:
            # 소요시간 = (목표% - 현재%) / 기준% * 기준시간
            value = max((target_percent - current_percent) / span * reference_minutes, 0)
            key, end = "minutes_needed", target_percent
        else:
            # 도달 SOC = 현재% + (기준% / 기준시간) * 충전가능시간
            value = min(current_percent + span / reference_minutes * available_minutes, 100)
            key, end = "reachable_percent", value
        options.append({"charger_output_kw": row["charger_output"], key: round(value, 1)})
        end_percents.append(end)

    # 측정 구간(예: 급속 10~80%) 밖은 선형 공식을 그대로 늘린 추정치라 실제와 다를 수 있음
    outside_reference_range = current_percent < low or max(end_percents) > high

    return json.dumps(
        {
            "found": True,
            "charger_speed": charger_speed,
            "reference_band_percent": [low, high],
            "outside_reference_range": outside_reference_range,
            "options": options,
        },
        ensure_ascii=False,
    )
