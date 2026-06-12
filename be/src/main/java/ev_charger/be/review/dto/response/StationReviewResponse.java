package ev_charger.be.review.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record StationReviewResponse(
        Long reviewId,
        String nickname,
        String profileImageUrl,
        int rating,
        String content,
        List<String> imageUrls,
        LocalDateTime createdAt,
        boolean isMyReview, // 내 리뷰인가
        boolean isEdited
) {
}
