from uuid import UUID
from sqlalchemy import text
from db import AsyncSessionLocal

# 충전기 타입 코드(chgerType)가 어떤 커넥터로 이뤄졌는지 — 한국환경공단 OpenAPI 가이드 3.3 코드표 그대로
# 08 'DC콤보(완속)'은 콤보 커넥터이므로 '콤보'로 취급. 11(DC콤보2)은 버스 전용이라 별도 커넥터로 둬서 승용차와 겹치지 않게 함
_CONNECTORS_BY_CODE = {
    "01": {"차데모"},
    "02": {"AC완속"},
    "03": {"차데모", "AC3상"},
    "04": {"콤보"},
    "05": {"차데모", "콤보"},
    "06": {"차데모", "AC3상", "콤보"},
    "07": {"AC3상"},
    "08": {"콤보"},
    "09": {"NACS"},
    "10": {"콤보", "NACS"},
    "11": {"콤보2(버스전용)"},
}


# 차량 충전구가 받는 추가 커넥터: 국내 DC콤보(콤보1/CCS1) 충전구는 위쪽이 AC 5핀(J1772 Type1)이라
# AC완속(02) 충전기 커넥터가 그대로 꽂힘
_EXTRA_CAR_CONNECTORS = {
    "콤보": {"AC완속"},
}


def expand_compatible_charger_types(car_codes: list[str]) -> list[str]:
    # car_charger에는 차량이 쓰는 커넥터가 코드 하나(예: 콤보=04)로만 들어있다.
    # 그런데 05·06·08·10 충전기에도 콤보 커넥터가 들어있어서 같은 차가 꽂을 수 있으므로,
    # 차량 커넥터와 하나라도 겹치는 충전기 코드를 전부 돌려준다.
    car_connectors = set().union(*(_CONNECTORS_BY_CODE.get(code, set()) for code in car_codes))
    car_connectors |= set().union(*(_EXTRA_CAR_CONNECTORS.get(c, set()) for c in car_connectors))
    compatible = {code for code, connectors in _CONNECTORS_BY_CODE.items() if connectors & car_connectors}
    # 코드표에 없는 값은 사라지지 않게 그대로 유지
    compatible |= {code for code in car_codes if code not in _CONNECTORS_BY_CODE}
    return sorted(compatible)


# 매 채팅마다 실행
# -> 약간 비효율적? 단순 pk 조회라서 빠르긴 할 듯 (캐시로 대체 가능 - 나중에 생각)

# user_id : car_id가 1:n이라, "어떤 차"인지는 프론트에서 선택해서 car_id로 넘겨줌
# car_id가 None이면 차량 미선택 -> DB 조회 없이 바로 None

# car_charger는 car_id가 아니라 (brand, model, model_year) 자연키로 car와 연결됨
# 한 차종이 커넥터를 여러 개 지원할 수 있어서(1차종 : n커넥터) array_agg로 배열 하나로 묶음
# FILTER로 매칭되는 car_charger가 없을 때 [null] 대신 빈 배열이 되게 함
_GET_MY_CAR_QUERY = text("""
    SELECT
        car.brand,
        car.model,
        car.trim,
        car.model_year,
        car.battery_type,
        user_car.battery_capacity,
        array_agg(car_charger.charger_type) FILTER (WHERE car_charger.charger_type IS NOT NULL) AS charger_types
    FROM user_car
    JOIN car ON user_car.car_id = car.car_id
    LEFT JOIN car_charger
        ON car_charger.brand = car.brand
        AND car_charger.model = car.model
        AND car_charger.model_year = car.model_year
    WHERE user_car.car_id = :car_id AND user_car.user_id = :user_id
    GROUP BY car.brand, car.model, car.trim, car.model_year, car.battery_type, user_car.battery_capacity
""")

# db 조회는 비동기
# 사용자A가 DB 응답 기다리는 동안, 사용자B와 C의 요청을 동시에 처리
# (await를 쓰려면 그 함수도 async여야 하고, 그 함수를 부르는 함수도 async여야 함)
async def get_my_car(user_id: UUID, car_id: int | None) -> dict | None:
    if car_id is None:
        return None

    async with AsyncSessionLocal() as session:
        # car_id뿐 아니라 user_id도 같이 걸어서, 남의 car_id가 실려 와도 조용히 걸러지도록 함
        result = await session.execute(_GET_MY_CAR_QUERY, {"car_id": car_id, "user_id": user_id})
        row = result.mappings().first()

    if row is None:
        return None

    my_car = dict(row)
    # 검색에 그대로 쓰일 값이라, 차량이 직접 쓰는 커넥터가 아니라 호환되는 충전기 코드 전체로 바꿔서 돌려줌
    my_car["charger_types"] = expand_compatible_charger_types(my_car["charger_types"] or [])
    return my_car
