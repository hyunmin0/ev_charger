from uuid import UUID

# 매 채팅마다 실행
# -> 약간 비효율적? 단순 pk 조회라서 빠르긴 할 듯 (캐시로 대체 가능 - 나중에 생각)

# db 조회는 비동기
# 사용자A가 DB 응답 기다리는 동안, 사용자B와 C의 요청을 동시에 처리
# (await를 쓰려면 그 함수도 async여야 하고, 그 함수를 부르는 함수도 async여야 함)
async def get_my_car(user_id: UUID) -> dict | None:
    # TODO: db.py 완성 후 실제 DB 조회로 교체
    # user_car + car 테이블 JOIN → car_name, battery_capacity, connector_type 반환
    return None
