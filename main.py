import logging

from fastapi import FastAPI, Depends, HTTPException
from fastapi.security import APIKeyHeader
from fastapi.responses import JSONResponse
from sqlalchemy import text

import config
from db import AsyncSessionLocal, DB_ERRORS
from schemas import ChatRequest, ChatResponse
from chat_service import chat

logger = logging.getLogger(__name__)

app = FastAPI()

# X-Internal-Key 헤더에서 키를 읽음
api_key_header = APIKeyHeader(name="X-Internal-Key")

# FastAPI의 Depends를 사용하여 API 요청 시 내부 키를 검증
# 요청 헤더에서 제공된 키가 config.INTERNAL_API_KEY와 일치하지 않으면 401 Unauthorized 오류를 발생시킵니다.
def verify_internal_key(key: str = Depends(api_key_header)):
    if key != config.INTERNAL_API_KEY:
        raise HTTPException(status_code=401, detail="Unauthorized")


# POST /chat으로 들어오는 요청을 처리
# 의존성 자동 주입 (with 코드 재사용성)
# -> 필요한 것을 개발자가 직접 넘기지 않아도, 요청이 올 때 FastAPI가 알아서 준비해서 넣어줌
@app.post("/chat", response_model=ChatResponse, dependencies=[Depends(verify_internal_key)])
async def chat_endpoint(request: ChatRequest):
    return await chat(request)


# 프로세스가 요청을 받을 수 있는지만 확인 (인증 없음 — 프로세스 매니저/백엔드가 부담 없이 찌를 수 있게)
# DB를 여기서 확인하지 않는 이유: DB가 잠깐 끊겼다고 멀쩡한 서버가 재시작 루프에 빠지면 안 됨
@app.get("/health")
async def health():
    return {"status": "ok"}


# DB 도달성까지 확인 (배포 직후 확인용 / 장애 원인 구분용)
# 실패해도 예외를 던지지 않고 503 + 이유를 내려줘서, 호출한 쪽이 로그 없이도 원인을 알 수 있게 한다
@app.get("/health/db")
async def health_db():
    try:
        async with AsyncSessionLocal() as session:
            await session.execute(text("SELECT 1"))
    except DB_ERRORS as e:
        logger.exception("health check DB 연결 실패")
        return JSONResponse(
            status_code=503,
            content={"status": "error", "detail": f"DB 연결 실패: {type(e).__name__}"},
        )

    return {"status": "ok"}
