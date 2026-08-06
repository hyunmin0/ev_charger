from uuid import UUID

# 매 채팅마다 실행
# -> 약간 비효율적? 단순 pk 조회라서 빠르긴 할 듯 (캐시로 대체 가능 - 나중에 생각)

# user_id : car_id가 1:n이라, "어떤 차"인지는 프론트에서 선택해서 car_id로 넘겨줌
# car_id가 None이면 차량 미선택 -> DB 조회 없이 바로 None

# db 조회는 비동기
# 사용자A가 DB 응답 기다리는 동안, 사용자B와 C의 요청을 동시에 처리
# (await를 쓰려면 그 함수도 async여야 하고, 그 함수를 부르는 함수도 async여야 함)
async def get_my_car(user_id: UUID, car_id: int | None) -> dict | None:
    if car_id is None:
        return None

    # TODO: db.py 완성 후 실제 DB 조회로 교체
    # user_car + car 테이블 JOIN, WHERE car_id = :car_id AND user_id = :user_id
    # (남의 car_id가 실려 와도 조용히 걸러지도록 소유권 조건을 같이 검사) → car_name, battery_capacity, connector_type 반환
    return None
