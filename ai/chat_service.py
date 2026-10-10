import json
import logging
import re
from openai import AsyncOpenAI

import config
from schemas import ChatRequest, ChatResponse, Station
from prompts import build_system_prompt
from context import get_my_car
from tools import TOOL_FUNCTIONS, STATION_RESULT_TOOLS, CONTEXT_PARAMS, tools_for

logger = logging.getLogger(__name__)

client = AsyncOpenAI(api_key=config.OPENAI_API_KEY)

# geocode_address -> get_nearby_stations처럼 tool끼리 순차 의존이 있을 수 있어서
# LLM 호출을 정확히 2번이 아니라 상한이 있는 루프로 돈다 (Agent는 아님 - 상한 있음)
MAX_TOOL_ROUNDS = 3

# 기본값(1.0)에서는 필요한 정보가 다 있는데도 tool을 안 부르고 되묻는 일이 잦았다 (충전 시간 질문 24/30 → 0.2에서 28/30)
LLM_TEMPERATURE = 0.2


# 이름 비교 때 무시할 문자 (LLM이 공백·괄호·가운뎃점을 빼거나 바꿔 쓰는 경우)
_NAME_NOISE = re.compile(r"[\s()\[\]{}·.,/\-_'\"]")


def _normalize_name(text: str) -> str:
    return _NAME_NOISE.sub("", text or "")


def _stations_in_reply(reply: str | None, stations: list[Station]) -> list[Station]:
    """
    tool이 돌려준 충전소 중 답변에 이름이 나온 것만, 답변에 나온 순서대로 남긴다.
    tool은 조건에 맞는 곳을 최대 10곳 주지만 답변은 그중 몇 곳만 추천하는 경우가 많아서,
    전부 카드로 내려가면 답변("2곳 추천")과 카드 수가 어긋남.
    - 긴 이름부터 찾고 찾은 부분은 지워서, "순천지사"가 "순천지사(공용)" 안에서 같이 잡히지 않게 함
    - 이름이 같은 충전소가 여러 곳이면(예: 주소가 다른 "순천시청" 2곳) 답변에 나온 횟수만큼 tool 결과 순서(가까운 순)대로 남김
    - 이름이 하나도 안 나오면(요약형 답변 등) 원래 목록을 그대로 둠
    """
    text = _normalize_name(reply)
    if not text or not stations:
        return stations

    by_name: dict[str, list[Station]] = {}
    for station in stations:
        name = _normalize_name(station.statNm)
        if name:
            by_name.setdefault(name, []).append(station)

    found: list[tuple[int, Station]] = []
    for name in sorted(by_name, key=len, reverse=True):
        positions = [m.start() for m in re.finditer(re.escape(name), text)]
        found.extend(zip(positions, by_name[name]))  # 짧은 쪽에 맞춰 짝지음
        text = text.replace(name, "\0" * len(name))  # 길이를 유지해서 다른 이름의 위치가 바뀌지 않게 함

    if not found:
        return stations
    return [station for _, station in sorted(found, key=lambda f: f[0])]


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
    tools = tools_for(my_car)
    context = {"my_car": my_car, "user_lat": request.lat, "user_lng": request.lng}

    # -------------- ④ tool_calls가 있는 동안 반복 (최대 MAX_TOOL_ROUNDS번)
    for _ in range(MAX_TOOL_ROUNDS):
        response = await client.chat.completions.create(
            model="gpt-4.1-mini",
            temperature=LLM_TEMPERATURE,
            messages=messages,
            tools=tools, # tool 스키마
        )

        assistant_message = response.choices[0].message

        # tool_calls가 없으면 바로 응답 반환
        if not assistant_message.tool_calls:
            reply = assistant_message.content
            return ChatResponse(reply=reply, stations=_stations_in_reply(reply, stations))

        # LLM이 tool을 부르기로 한 메시지를 messages에 추가
        messages.append(assistant_message)

        for tool_call in assistant_message.tool_calls:
            name = tool_call.function.name

            # tool 이름/인자는 LLM이 만든 값이라 깨질 수 있음 (없는 tool, 잘못된 인자, 깨진 JSON).
            # 여기서 막지 않으면 요청 전체가 500이 되므로, error를 tool 결과로 돌려주고 대화를 계속한다
            try:
                args = json.loads(tool_call.function.arguments)
                injected = {key: context[key] for key in CONTEXT_PARAMS.get(name, ())}
                result = await TOOL_FUNCTIONS[name](**{**args, **injected}) # tool 함수 호출
            except Exception:
                logger.exception("tool 실행 실패 (name=%s, args=%s)", name, tool_call.function.arguments)
                result = json.dumps(
                    {"error": f"{name} 실행에 실패했습니다."},
                    ensure_ascii=False,
                )

            # 충전소 목록을 돌려주는 tool 결과에서 stations 추출 (마지막 호출 결과가 최종값)
            # 실패해서 error만 담긴 경우엔 stations가 없으므로 빈 리스트가 된다
            if name in STATION_RESULT_TOOLS:
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
        temperature=LLM_TEMPERATURE,
        messages=messages,
    )

    reply = final_response.choices[0].message.content
    return ChatResponse(reply=reply, stations=_stations_in_reply(reply, stations))
