from sqlalchemy.ext.asyncio import create_async_engine, AsyncSession
from sqlalchemy.orm import sessionmaker

import config

# asyncpg 드라이버 사용 → DATABASE_URL 앞에 postgresql+asyncpg:// 형식이어야 함
engine = create_async_engine(config.DATABASE_URL)

# DB 세션 팩토리 — 실제 쿼리는 세션을 통해 실행
# expire_on_commit=False: 커밋 후에도 메모리의 값 유지 -> read_only라서 커밋 자체가 없긴함
AsyncSessionLocal = sessionmaker(engine, class_=AsyncSession, expire_on_commit=False)
