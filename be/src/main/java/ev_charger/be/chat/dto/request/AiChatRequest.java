package ev_charger.be.chat.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.UUID;

// be -> AI 서버(POST /chat) 요청. AI 서버의 ChatRequest(ai/schemas.py)와 필드명이 같아야 함
public record AiChatRequest(
        @JsonProperty("user_id") UUID userId,
        @JsonProperty("car_id") Long carId,
        String message,
        List<ChatMessage> history,
        Double lat,
        Double lng
) {}
