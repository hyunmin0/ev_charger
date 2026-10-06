from sqlalchemy.ext.asyncio import create_async_engine, AsyncSession
from sqlalchemy.orm import sessionmaker
from sqlalchemy.exc import SQLAlchemyError

import config

# DB 조회를 감쌀 때 잡아야 하는 예외들.
# DB 호스트에 아예 못 붙는 경우(연결 거부, DNS 실패, 연결 타임아웃)는 SQLAlchemy가 감싸주지 않고
# ConnectionRefusedError 같은 OSError가 그대로 올라온다 — 실제로 DB를 끄고 확인함.
# SQLAlchemyError만 잡으면 정작 "DB 다운"이라는 가장 흔한 장애를 놓치므로 OSError도 같이 잡는다.
DB_ERRORS = (SQLAlchemyError, OSError)

# asyncpg 드라이버 사용 → DATABASE_URL 앞에 postgresql+asyncpg:// 형식이어야 함
engine = create_async_engine(config.DATABASE_URL)

# DB 세션 팩토리 — 실제 쿼리는 세션을 통해 실행
# expire_on_commit=False: 커밋 후에도 메모리의 값 유지 -> read_only라서 커밋 자체가 없긴함
AsyncSessionLocal = sessionmaker(engine, class_=AsyncSession, expire_on_commit=False)
