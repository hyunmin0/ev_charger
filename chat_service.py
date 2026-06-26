import json
from openai import AsyncOpenAI

import config
from schemas import ChatRequest, ChatResponse, Station
from prompts import build_system_prompt
from context import get_my_car
from tools import ACTIVE_TOOLS, TOOL_FUNCTIONS

client = AsyncOpenAI(api_key=config.OPENAI_API_KEY)


# schemas.py - ChatRequest, ChatResponse
async def chat(request: ChatRequest) -> ChatResponse:
    # -------------- ① context.py - 사용자 차량 정보 미리 조회 (매 요청마다)
    my_car = await get_my_car(request.user_id)

    # -------------- ② prompts.py - system prompt 생성
    system_prompt = build_system_prompt(my_car)

    # -------------- ③ OpenAI에 넘길 messages 조립
    #    system → 이전 대화(history) → 현재 질문 순서
    messages = [
        {"role": "system", "content": system_prompt},
        *[{"role": m.role, "content": m.content} for m in request.history],
        {"role": "user", "content": request.message},
    ]

    # -------------- ④ LLM 첫 번째 호출 (tool 사용 여부 판단) - tools/__init__.py
    response = await client.chat.completions.create(
        model="gpt-4.1-mini",
        messages=messages,
        tools=ACTIVE_TOOLS,
    )

    assistant_message = response.choices[0].message

    # -------------- ⑤ tool_calls가 없으면 바로 응답 반환
    if not assistant_message.tool_calls:
        return ChatResponse(reply=assistant_message.content)


    # -------------- ⑥ tool_calls가 있으면 → tool 실행 → LLM 두 번째 호출
    # LLM이 tool을 부르기로 한 메시지를 messages에 추가
    messages.append(assistant_message)

    stations: list[Station] = []

    for tool_call in assistant_message.tool_calls:
        name = tool_call.function.name
        args = json.loads(tool_call.function.arguments)

        result = await TOOL_FUNCTIONS[name](**args)

        # get_nearby_stations 결과에서 stations 추출
        if name == "get_nearby_stations":
            result_data = json.loads(result)
            stations = [Station(**s) for s in result_data.get("stations", [])]

        # tool 실행 결과를 messages에 추가
        messages.append({
            "role": "tool",
            "tool_call_id": tool_call.id,
            "content": result,
        })

    # -------------- ⑦ tool 결과를 포함해서 LLM 두 번째 호출 → 최종 답변 생성
    tool_response = await client.chat.completions.create(
        model="gpt-4.1-mini",
        messages=messages,
    )

    return ChatResponse(
        reply=tool_response.choices[0].message.content,
        stations=stations,
    )
