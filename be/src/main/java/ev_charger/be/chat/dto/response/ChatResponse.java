package ev_charger.be.chat.dto.response;

import java.util.List;

// AI 서버 응답(reply, stations)에 오늘 남은 질문 수를 붙여 앱에 전달 (충전소 추천이 아니면 stations는 빈 리스트)
// AI 서버 응답을 읽을 때는 remainingToday가 없어서 null
public record ChatResponse(
        String reply,
        List<ChatStation> stations,
        Integer remainingToday
) {
    public ChatResponse {
        if (stations == null) stations = List.of();
    }

    public ChatResponse(String reply, List<ChatStation> stations) {
        this(reply, stations, null);
    }

    public ChatResponse withRemainingToday(int remainingToday) {
        return new ChatResponse(reply, stations, remainingToday);
    }
}
