import json
from openai import AsyncOpenAI

import config
from schemas import ChatRequest, ChatResponse, Station
from prompts import build_system_prompt
from context import get_my_car
from tools import ACTIVE_TOOLS, TOOL_FUNCTIONS

client = AsyncOpenAI(api_key=config.OPENAI_API_KEY)

# geocode_address -> get_nearby_stations처럼 tool끼리 순차 의존이 있을 수 있어서
# LLM 호출을 정확히 2번이 아니라 상한이 있는 루프로 돈다 (Agent는 아님 - 상한 있음)
MAX_TOOL_ROUNDS = 3


# schemas.py - ChatRequest, ChatResponse
async def chat(request: ChatRequest) -> ChatResponse:
    # -------------- ① context.py - 사용자 차량 정보 미리 조회 (매 요청마다)
    my_car = await get_my_car(request.user_id, request.car_id)

    # -------------- ② prompts.py - system prompt 생성 (현재 위치도 함께 주입)
    system_prompt = build_system_prompt(my_car, request.lat, request.lng)

    # -------------- ③ OpenAI에 넘길 messages 조립
    #    system → 이전 대화(history) → 현재 질문 순서
    messages = [
        {"role": "system", "content": system_prompt},
        *[{"role": m.role, "content": m.content} for m in request.history],
        {"role": "user", "content": request.message},
    ]

    stations: list[Station] = []

    # -------------- ④ tool_calls가 있는 동안 반복 (최대 MAX_TOOL_ROUNDS번)
    for _ in range(MAX_TOOL_ROUNDS):
        response = await client.chat.completions.create(
            model="gpt-4.1-mini",
            messages=messages,
            tools=ACTIVE_TOOLS, # tool 스키마
        )

        assistant_message = response.choices[0].message

        # tool_calls가 없으면 바로 응답 반환
        if not assistant_message.tool_calls:
            return ChatResponse(reply=assistant_message.content, stations=stations)

        # LLM이 tool을 부르기로 한 메시지를 messages에 추가
        messages.append(assistant_message)

        for tool_call in assistant_message.tool_calls:
            name = tool_call.function.name
            args = json.loads(tool_call.function.arguments)

            result = await TOOL_FUNCTIONS[name](**args) # tool 함수 호출

            # get_nearby_stations 결과에서 stations 추출 (마지막 호출 결과가 최종값)
            if name == "get_nearby_stations":
                result_data = json.loads(result)
                stations = [Station(**s) for s in result_data.get("stations", [])]

            # tool 실행 결과를 messages에 추가
            messages.append({
                "role": "tool",
                "tool_call_id": tool_call.id,
                "content": result,
            })

    # -------------- ⑤ 루프 상한까지 돌았으면 tool 없이 마지막 호출로 강제 마무리
    final_response = await client.chat.completions.create(
        model="gpt-4.1-mini",
        messages=messages,
    )

    return ChatResponse(
        reply=final_response.choices[0].message.content,
        stations=stations,
    )
