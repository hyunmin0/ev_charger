import json
import logging
import httpx

import config

logger = logging.getLogger(__name__)

# 카카오 API가 느려질 때 /chat 전체가 같이 늘어지지 않도록 타임아웃을 건다
KAKAO_TIMEOUT_SECONDS = 5.0

# 카카오 로컬 API - 키워드로 장소 검색 (주소 검색 대신 이걸 쓰는 이유:
# "전남대"처럼 건물/장소명은 정식 주소 형식이 아니라 키워드 검색이 인식률이 더 높음)
KAKAO_KEYWORD_SEARCH_URL = "https://dapi.kakao.com/v2/local/search/keyword.json"

# OpenAI function calling용 JSON schema
GEOCODE_ADDRESS_SCHEMA = {
    "type": "function",
    "function": {
        "name": "geocode_address",
        "description": (
            "지명·건물명·주소를 위도/경도 좌표로 변환합니다. "
            "사용자가 현재 위치가 아닌 다른 지역/장소를 언급했을 때만 사용하세요 "
            "(예: '전남대 근처', '강남역 주변'). 좌표를 직접 추측하지 마세요."
        ),
        "parameters": {
            "type": "object",
            "properties": {
                "query": {
                    "type": "string",
                    "description": "검색할 지명, 건물명, 또는 주소 (예: '전남대학교', '강남역')",
                },
            },
            "required": ["query"],
        },
    },
}


async def geocode_address(query: str) -> str:
    headers = {"Authorization": f"KakaoAK {config.KAKAO_API_KEY}"}
    params = {"query": query}

    # 카카오 API 장애/타임아웃은 "장소를 못 찾음(found: False)"과 구분해서 error로 내려준다
    # (못 찾은 건 다시 물어보면 되지만, 장애는 재시도 안내를 해야 하므로)
    try:
        async with httpx.AsyncClient(timeout=KAKAO_TIMEOUT_SECONDS) as client:
            response = await client.get(KAKAO_KEYWORD_SEARCH_URL, headers=headers, params=params)
            response.raise_for_status()
            data = response.json()
    except (httpx.HTTPError, ValueError):
        logger.exception("geocode_address 카카오 API 호출 실패 (query=%s)", query)
        return json.dumps(
            {"error": "장소 검색에 실패했습니다. 잠시 후 다시 시도해 주세요."},
            ensure_ascii=False,
        )

    documents = data.get("documents", [])
    if not documents:
        return json.dumps({"found": False}, ensure_ascii=False)

    top = documents[0]
    return json.dumps(
        {
            "found": True,
            "place_name": top["place_name"],
            "address": top.get("road_address_name") or top.get("address_name"),
            # 카카오 응답은 x=경도, y=위도 이고 둘 다 문자열로 옴
            "lat": float(top["y"]),
            "lng": float(top["x"]),
        },
        ensure_ascii=False,
    )
