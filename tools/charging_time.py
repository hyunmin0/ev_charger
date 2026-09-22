import json
import logging
from sqlalchemy import text
from db import AsyncSessionLocal, DB_ERRORS

logger = logging.getLogger(__name__)

# 기준 SOC 구간 10% -> 80% 고정 (charge.minutes가 이 구간 기준이라고 가정)
# TODO: DB 실데이터 확인 후 차종별로 기준 구간이 다르면 이 상수 대신 DB 컬럼으로 교체
REFERENCE_SOC_PERCENT = 70

GET_CHARGING_TIME_SCHEMA = {
    "type": "function",
    "function": {
        "name": "get_charging_time",
        "description": (
            "차량이 목표 충전량까지 도달하는 데 걸리는 시간, 또는 주어진 충전 가능 시간 동안 "
            "도달 가능한 충전량을 계산합니다. target_percent와 available_minutes 중 정확히 하나만 넘기세요."
        ),
        "parameters": {
            "type": "object",
            "properties": {
                "brand": {"type": "string", "description": "차량 제조사 (시스템 프롬프트의 차량 정보 참고)"},
                "model": {"type": "string", "description": "차량 모델명"},
                "battery_type": {"type": "string", "description": "배터리 타입"},
                "model_year": {"type": "integer", "description": "연식"},
                "charger_type": {
                    "type": "string",
                    "description": "충전기 타입 코드 (get_nearby_stations와 동일 코드 체계)",
                    "enum": ["01","02","03","04","05","06","07","08","09","10","11"],
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
            "required": ["brand", "model", "battery_type", "model_year", "charger_type", "current_percent"],
        },
    },
}


async def get_charging_time(
    brand: str,
    model: str,
    battery_type: str,
    model_year: int,
    charger_type: str,
    current_percent: float,
    target_percent: float | None = None,
    available_minutes: float | None = None,
) -> str:
    # charger_output이 여러 개(예: 50kW/100kW 급속)면 가장 빠른(출력 큰) 쪽을 기준으로 사용
    query = text("""
        SELECT minutes
        FROM charge
        WHERE brand = :brand
          AND model = :model
          AND battery_type = :battery_type
          AND model_year = :model_year
          AND charger_type = :charger_type
        ORDER BY (charger_output IS NULL), charger_output DESC
        LIMIT 1
    """)
    params = {
        "brand": brand,
        "model": model,
        "battery_type": battery_type,
        "model_year": model_year,
        "charger_type": charger_type,
    }

    try:
        async with AsyncSessionLocal() as session:
            result = await session.execute(query, params)
            row = result.mappings().first()
    except DB_ERRORS:
        logger.exception("get_charging_time DB 조회 실패 (%s %s %s)", brand, model, model_year)
        return json.dumps(
            {"error": "충전 시간 정보를 조회하지 못했습니다. 잠시 후 다시 시도해 주세요."},
            ensure_ascii=False,
        )

    if row is None:
        return json.dumps({"found": False}, ensure_ascii=False)

    reference_minutes = row["minutes"]

    # 소요시간 = (목표% - 현재%) / 기준% * 기준시간
    if target_percent is not None:
        minutes_needed = (target_percent - current_percent) / REFERENCE_SOC_PERCENT * reference_minutes
        return json.dumps(
            {"found": True, "minutes_needed": round(max(minutes_needed, 0), 1)},
            ensure_ascii=False,
        )

    # 도달 SOC = 현재% + (기준% / 기준시간) * 충전가능시간
    if available_minutes is not None:
        charge_rate = REFERENCE_SOC_PERCENT / reference_minutes  # %/분
        reachable_percent = current_percent + charge_rate * available_minutes
        return json.dumps(
            {"found": True, "reachable_percent": round(min(reachable_percent, 100), 1)},
            ensure_ascii=False,
        )

    return json.dumps(
        {"found": False, "error": "target_percent 또는 available_minutes 중 하나가 필요합니다"},
        ensure_ascii=False,
    )
