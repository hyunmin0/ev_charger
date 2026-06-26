from fastapi import FastAPI, Depends, HTTPException
from fastapi.security import APIKeyHeader

import config
from schemas import ChatRequest, ChatResponse
from chat_service import chat

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
