package ev_charger.be.review.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record StationReviewsResponse(
        Long reviewId,
        String nickname,
        String profileImageUrl,
        int rating,
        String content,
        List<String> imageUrls,
        LocalDateTime createdAt,
        boolean isEdited
) {
}
