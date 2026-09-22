import asyncio
from dotenv import load_dotenv
import os
from sqlalchemy import text
from sqlalchemy.ext.asyncio import create_async_engine

load_dotenv()

# DATABASE_URL이 postgresql+asyncpg:// 형식(asyncpg 드라이버)이라 동기 create_engine으론 접속 못 함
async def main():
    engine = create_async_engine(os.getenv("DATABASE_URL"))
    async with engine.connect() as conn:
        result = await conn.execute(text("SELECT 1"))
        print(result.fetchone())
    print("연결 성공!")


asyncio.run(main())
