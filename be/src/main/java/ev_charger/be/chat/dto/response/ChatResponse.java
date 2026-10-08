package ev_charger.be.chat.dto.response;

import java.util.List;

// AI 서버 응답을 그대로 앱에 전달 (충전소 추천이 아니면 stations는 빈 리스트)
public record ChatResponse(
        String reply,
        List<ChatStation> stations
) {
    public ChatResponse {
        if (stations == null) stations = List.of();
    }
}
