from tools.station import (
    GET_NEARBY_STATIONS_SCHEMA,
    SEARCH_STATIONS_SCHEMA,
    get_nearby_stations,
    search_stations,
)
from tools.geocode import GEOCODE_ADDRESS_SCHEMA, geocode_address
from tools.charging_time import GET_CHARGING_TIME_SCHEMA, get_charging_time

# LLM 호출할 때는 tool 스키마 리스트
# (완성된 것만 여기에 추가)
ACTIVE_TOOLS = [
    GET_NEARBY_STATIONS_SCHEMA,
    SEARCH_STATIONS_SCHEMA,
    GEOCODE_ADDRESS_SCHEMA,
    GET_CHARGING_TIME_SCHEMA,
]

# tool 이름 → 실제 함수 매핑 (chat_service.py에서 tool메시지의 content로 활용)
TOOL_FUNCTIONS = {
    "get_nearby_stations": get_nearby_stations,
    "search_stations": search_stations,
    "geocode_address": geocode_address,
    "get_charging_time": get_charging_time,
}

# 결과에 stations가 들어있어서 ChatResponse.stations로 앱에 내려줘야 하는 tool들
# (앱은 이 목록을 카드로 그리고, statId로 충전소 상세 화면으로 이동)
STATION_RESULT_TOOLS = {"get_nearby_stations", "search_stations"}
