from dotenv import load_dotenv
import os
from sqlalchemy import create_engine, text

load_dotenv()

engine = create_engine(os.getenv("DATABASE_URL"))
with engine.connect() as conn:
    result = conn.execute(text("SELECT 1"))
    print(result.fetchone())
print("연결 성공!")
