from dotenv import load_dotenv
import os

load_dotenv()

# 환경변수를 읽고, 없으면 즉시 에러 ( _ : 이 파일 내부에서만 쓰는 함수)
def _require(name: str) -> str:
    value = os.getenv(name)
    if not value:
        raise RuntimeError(f"환경변수 {name} 이(가) 설정되지 않았습니다.")
    return value

OPENAI_API_KEY = _require("OPENAI_API_KEY")
DATABASE_URL = _require("DATABASE_URL")
INTERNAL_API_KEY = _require("INTERNAL_API_KEY") # 백엔드 소통(키)
