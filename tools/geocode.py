import json
import httpx

import config

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

    async with httpx.AsyncClient() as client:
        response = await client.get(KAKAO_KEYWORD_SEARCH_URL, headers=headers, params=params)
        response.raise_for_status()
        data = response.json()

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
