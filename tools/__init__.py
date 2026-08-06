from tools.station import GET_NEARBY_STATIONS_SCHEMA, get_nearby_stations
from tools.geocode import GEOCODE_ADDRESS_SCHEMA, geocode_address

# LLM 호출할 때는 tool 스키마 리스트
# (완성된 것만 여기에 추가)
ACTIVE_TOOLS = [GET_NEARBY_STATIONS_SCHEMA, GEOCODE_ADDRESS_SCHEMA]

# tool 이름 → 실제 함수 매핑 (chat_service.py에서 tool메시지의 content로 활용)
TOOL_FUNCTIONS = {
    "get_nearby_stations": get_nearby_stations,
    "geocode_address": geocode_address,
}
