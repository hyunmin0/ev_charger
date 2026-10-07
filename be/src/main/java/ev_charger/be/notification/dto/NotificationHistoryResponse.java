package ev_charger.be.notification.dto;

import java.time.LocalDateTime;

public record NotificationHistoryResponse(
        Long id,
        String statId,
        String statNm, // 충전소 이름 (알림 이후 충전소가 삭제됐으면 null)
        String chgerId,
        LocalDateTime createdAt,
        boolean isRead
) {}