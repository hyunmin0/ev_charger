package ev_charger.be.review.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record UserReviewsResponse(
    Long reviewId,
    String statId,
    String statNm,
    int rating,
    String content,
    List<String> imageUrls,
    LocalDateTime createdAt,
    boolean isEdited
) {
}
