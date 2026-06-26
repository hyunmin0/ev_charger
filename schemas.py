from pydantic import BaseModel
from uuid import UUID

# 클라이언트(앱)에서 서버로 보내는 요청과, 서버에서 클라이언트로 보내는 응답의 데이터 구조 정의

class ChatMessage(BaseModel):
    role: str       # "user" | "assistant"
    content: str

# 앱 -> AI
class ChatRequest(BaseModel):
    user_id: UUID
    message: str
    # history는 클라이언트(앱)이 저장 (일단)
    # 앱이 들고 다니다가 매 요청에 포함해서 보냄
    history: list[ChatMessage]

class Station(BaseModel):
    statId: str
    statNm: str
    addr: str
    parkingFree: str    # 'Y' | 'N'
    distance_km: float

# AI -> 앱
class ChatResponse(BaseModel):
    # history가 없어도 되는 이유:
    # 클라이언트는 자신이 보낸 질문(message)와 ai대답(reply)만 저장
    reply: str
    stations: list[Station] = []  # 충전소 추천이 아니면 빈 리스트