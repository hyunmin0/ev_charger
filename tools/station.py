import json
from sqlalchemy import text
from db import AsyncSessionLocal

# OpenAI function calling용 JSON schema (LLM에 넘겨주는 tool)
# LLM이 이 schema를 보고, 파라미터를 작성함
GET_NEARBY_STATIONS_SCHEMA = {
    "type": "function",
    "function": {
        "name": "get_nearby_stations",
        "description": "사용자 위치 근처의 전기차 충전소를 조회합니다.",
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
                "charger_type": {
                    "type": "string",
                    "description": (
                        "충전기 타입 코드. "
                        "01=DC차데모, 02=AC완속, 03=DC차데모+AC3상, "
                        "04=DC콤보, 05=DC차데모+DC콤보, 06=DC차데모+AC3상+DC콤보, "
                        "07=AC3상, 08=DC콤보+AC3상, 09=HPC(버스), "
                        "10=슈퍼차저, 11=이동형충전기"
                    ),
                    "enum": ["01","02","03","04","05","06","07","08","09","10","11"],
                },
                "available_only": {
                    "type": "boolean",
                    "description": "True이면 현재 이용 가능한 충전기가 있는 곳만 반환. 기본값 true",
                },
                "parking_free": {
                    "type": "boolean",
                    "description": "True이면 무료주차 충전소만 반환. 미지정이면 조건 없음",
                },
            },
            "required": ["lat", "lng"],
        },
    },
}

# tool 이름 → 실제 함수 매핑 (chat_service.py에서 tool 실행할 때 사용)

# 실제 DB 조회
# 거리(기본: 3km), 사용가능(기본)
async def get_nearby_stations(
    lat: float,
    lng: float,
    radius_km: float = 3,
    charger_type: str | None = None,
    available_only: bool = True,
    parking_free: bool | None = None,
) -> str:
    params = {
        "lat": lat,
        "lng": lng,
        "radius_m": radius_km * 1000,
    }

    # 반경 필터 (필수)
    where = ["ST_DWithin(s.location, ST_MakePoint(:lng, :lat)::geography, :radius_m)"]

    # charger 테이블 조건 (charger_type, available_only 중 하나라도 있으면 EXISTS 사용)
    charger_conditions = []
    if available_only:
        charger_conditions.append("c.stat = '1'")       # '1' = 이용가능
    if charger_type:
        charger_conditions.append('c."chgerType" = :charger_type')
        params["charger_type"] = charger_type
    
    if charger_conditions: # charger_conditions 합치기 -> where
        where.append(
            f"""EXISTS (SELECT 1 FROM charger c WHERE c."statId" = s."statId" AND {' AND '.join(charger_conditions)})"""
        )

    # station 테이블 조건 - 무료주차 필터 (선택)
    if parking_free is True:
        where.append('s."parkingFree" = \'Y\'')

    query = text(f"""
        SELECT
            s."statId",
            s."statNm",
            s.addr,
            s."parkingFree",
            ST_Distance(s.location, ST_MakePoint(:lng, :lat)::geography) / 1000 AS distance_km
        FROM station s
        WHERE {' AND '.join(where)}
        ORDER BY distance_km
    """)

    async with AsyncSessionLocal() as session:
        result = await session.execute(query, params)
        rows = result.mappings().all() # 결과를 컬럼명 → 값 형태의 dict처럼 (list[RowMapping])

    stations = [{**dict(row), "distance_km": round(row["distance_km"], 2)} for row in rows]
    return json.dumps({"stations": stations}, ensure_ascii=False)
