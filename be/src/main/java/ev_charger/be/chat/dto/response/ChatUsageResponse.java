package ev_charger.be.chat.dto.response;

// 오늘(한국 날짜) 챗봇 질문 사용량. 자정에 초기화
public record ChatUsageResponse(
        int limit,
        int used,
        int remaining
) {}
