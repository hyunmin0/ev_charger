from uuid import UUID
from sqlalchemy import text
from db import AsyncSessionLocal

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

    return dict(row)
