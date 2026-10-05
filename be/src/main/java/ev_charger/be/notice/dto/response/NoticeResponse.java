package ev_charger.be.notice.dto.response;

import java.time.LocalDateTime;

public record NoticeResponse(
        long id,
        String title,
        String content,
        LocalDateTime createdAt
) {
}
