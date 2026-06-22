package ev_charger.be.notice.dto.response;

import ev_charger.be.notice.Notice;

import java.time.LocalDateTime;

public record NoticeListResponse(
        long id,
        String title,
        LocalDateTime createdAt,
        boolean isRead
) {
    public static NoticeListResponse from(Notice notice, boolean isRead) {
        return new NoticeListResponse(
                notice.getNoticeId(),
                notice.getTitle(),
                notice.getCreatedAt(),
                isRead
        );
    }
}
