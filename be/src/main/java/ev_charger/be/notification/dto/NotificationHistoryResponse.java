package ev_charger.be.notification.dto;

import java.time.LocalDateTime;

public record NotificationHistoryResponse(
        Long id,
        String statId,
        String chgerId,
        LocalDateTime createdAt,
        boolean isRead
) {}