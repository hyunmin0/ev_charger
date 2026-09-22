import json
import logging
from sqlalchemy import text
from db import AsyncSessionLocal, DB_ERRORS
from tools.station_filters import (
    CHARGER_TYPES_PROPERTY,
    DISTANCE_KM_SQL,
    PARKING_FREE_PROPERTY,
    SPEED_PROPERTY,
    build_charger_clause,
    build_keyword_clause,
    build_parking_free_clause,
    build_radius_clause,
)

logger = logging.getLogger(__name__)

# 조회 결과가 통째로 LLM 컨텍스트에 들어가기 때문에, 도심에서 수백 건이 실리지 않도록 상한을 둔다
# (조건에 맞는 전체 건수는 total_count로 따로 내려서 "총 N곳 중 가까운 10곳" 안내가 가능하게 함)
MAX_STATIONS = 10


# ---------------------------------------------------------------- OpenAI function calling용 JSON schema
# LLM이 이 schema를 보고, 파라미터를 작성함
# 공통 필터 파라미터(충전기 타입/속도/무료주차)는 station_filters.py에서 가져와 두 tool이 같은 정의를 쓴다

GET_NEARBY_STATIONS_SCHEMA = {
    "type": "function",
    "function": {
        "name": "get_nearby_stations",
        "description": (
            "좌표 기준으로 근처의 전기차 충전소를 조회합니다. "
            "조건에 맞는 전체 건수는 total_count로, 그중 가까운 순 최대 10곳만 stations로 반환합니다."
        ),
        "parameters": {
            "type": "object",
            "properties": {
                "lat": {
                    "type": "number",
                    "description": "위도 (예: 37.5665)",
                },
                "lng": {
                    "type": "number",
                    "description": "경도 (예: 126.9780)",
                },
                "radius_km": {
                    "type": "number",
                    "description": "검색 반경(km). 기본값 3",
                },
                "charger_types": CHARGER_TYPES_PROPERTY,
                "speed": SPEED_PROPERTY,
                "available_only": {
                    "type": "boolean",
                    "description": "True이면 현재 이용 가능한 충전기가 있는 곳만 반환. 기본값 true",
                },
                "parking_free": PARKING_FREE_PROPERTY,
            },
            "required": ["lat", "lng"],
        },
    },
}

SEARCH_STATIONS_SCHEMA = {
    "type": "function",
    "function": {
        "name": "search_stations",
        "description": (
            "충전소 이름이나 주소에 포함된 키워드로 충전소를 검색합니다 "
            "(예: '한국전력 광주지사 충전소가 어디야', '충장로에 있는 충전소'). "
            "반경 제한 없이 전국에서 찾되, 사용자 위치에서 가까운 순으로 최대 10곳을 반환합니다. "
            "'근처/주변'처럼 위치 기준 질문에는 이 tool이 아니라 get_nearby_stations를 쓰세요."
        ),
        "parameters": {
            "type": "object",
            "properties": {
                "keyword": {
                    "type": "string",
                    "description": (
                        "충전소명 또는 주소에서 찾을 키워드. 사용자가 말한 이름 그대로 넣되, "
                        "'충전소'처럼 거의 모든 이름에 들어가는 일반 단어만 남지 않도록 핵심 단어를 쓰세요."
                    ),
                },
                "lat": {
                    "type": "number",
                    "description": "거리 계산 기준 위도 — 시스템 프롬프트의 현재 위치를 넣으세요",
                },
                "lng": {
                    "type": "number",
                    "description": "거리 계산 기준 경도 — 시스템 프롬프트의 현재 위치를 넣으세요",
                },
                "charger_types": CHARGER_TYPES_PROPERTY,
                "speed": SPEED_PROPERTY,
                "available_only": {
                    "type": "boolean",
                    "description": (
                        "True이면 현재 이용 가능한 충전기가 있는 곳만 반환. 기본값 false "
                        "(이름으로 찾는 질문은 지금 쓸 수 있는지와 무관한 경우가 많음)"
                    ),
                },
                "parking_free": PARKING_FREE_PROPERTY,
            },
            "required": ["keyword", "lat", "lng"],
        },
    },
}


# ---------------------------------------------------------------- 공통 쿼리 실행
# 두 tool은 WHERE 절만 다르고 SELECT 목록·정렬·상한·결과 형태·에러 처리가 모두 같아서 여기로 묶는다

async def _run_station_query(where: list[str], params: dict, tool_name: str) -> str:
    # COUNT(*) OVER ()는 LIMIT이 적용되기 전 기준으로 계산되므로, 잘라내기 전 전체 건수가 나온다
    query = text(f"""
        SELECT
            s."statId",
            s."statNm",
            s.addr,
            s."parkingFree",
            {DISTANCE_KM_SQL} AS distance_km,
            COUNT(*) OVER () AS total_count
        FROM station s
        WHERE {' AND '.join(where)}
        ORDER BY distance_km
        LIMIT :limit
    """)

    # DB 장애 시 예외를 그대로 터뜨리면 /chat이 500이 되므로, error를 담은 JSON으로 바꿔서
    # LLM이 사용자에게 안내 문구로 풀어주도록 한다
    try:
        async with AsyncSessionLocal() as session:
            result = await session.execute(query, {**params, "limit": MAX_STATIONS})
            rows = result.mappings().all() # 결과를 컬럼명 → 값 형태의 dict처럼 (list[RowMapping])
    except DB_ERRORS:
        logger.exception("%s DB 조회 실패 (params=%s)", tool_name, params)
        return json.dumps(
            {"error": "충전소 정보를 조회하지 못했습니다. 잠시 후 다시 시도해 주세요."},
            ensure_ascii=False,
        )

    stations = [
        {
            "statId": row["statId"],
            "statNm": row["statNm"],
            "addr": row["addr"],
            "parkingFree": row["parkingFree"],
            "distance_km": round(row["distance_km"], 2),
        }
        for row in rows
    ]
    # rows가 비면 조건에 맞는 곳이 아예 없는 것이므로 total_count도 0
    total_count = rows[0]["total_count"] if rows else 0

    return json.dumps(
        {"total_count": total_count, "stations": stations},
        ensure_ascii=False,
    )


def _append_common_filters(
    where: list[str],
    params: dict,
    charger_types: list[str] | None,
    available_only: bool,
    speed: str | None,
    parking_free: bool | None,
) -> None:
    """두 tool이 공유하는 필터를 where/params에 덧붙인다 (조건이 없으면 아무것도 안 붙음)."""
    for clause, clause_params in (
        build_charger_clause(charger_types, available_only, speed),
        build_parking_free_clause(parking_free),
    ):
        if clause:
            where.append(clause)
            params.update(clause_params)


# ---------------------------------------------------------------- 실제 tool 구현

# 거리(기본: 3km), 사용가능(기본)
async def get_nearby_stations(
    lat: float,
    lng: float,
    radius_km: float = 3,
    charger_types: list[str] | None = None,
    available_only: bool = True,
    parking_free: bool | None = None,
    speed: str | None = None,
) -> str:
    # 검색 출발점: 반경
    radius_clause, radius_params = build_radius_clause(radius_km)

    params = {"lat": lat, "lng": lng, **radius_params}
    where = [radius_clause]

    _append_common_filters(where, params, charger_types, available_only, speed, parking_free)

    return await _run_station_query(where, params, "get_nearby_stations")


# 이름/주소 키워드 검색 — 반경 제한 없이 찾고, 정렬만 현재 위치 기준
async def search_stations(
    keyword: str,
    lat: float,
    lng: float,
    charger_types: list[str] | None = None,
    available_only: bool = False,
    parking_free: bool | None = None,
    speed: str | None = None,
) -> str:
    # 검색 출발점: 이름/주소 키워드
    keyword_clause, keyword_params = build_keyword_clause(keyword)

    params = {"lat": lat, "lng": lng, **keyword_params}
    where = [keyword_clause]

    _append_common_filters(where, params, charger_types, available_only, speed, parking_free)

    return await _run_station_query(where, params, "search_stations")
