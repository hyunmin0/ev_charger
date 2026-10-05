package ev_charger.be.review;

import ev_charger.be.review.dto.request.ReviewRequest;
import ev_charger.be.review.dto.response.StationReviewResponse;
import ev_charger.be.review.dto.response.StationReviewsSummary;
import ev_charger.be.review.dto.response.UserReviewsResponse;
import ev_charger.be.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    // 리뷰 작성
    // input : statId(String), ReviewRequest { content, rating }
    // output: 없음 (200 OK)
    @PostMapping("/{statId}")
    public ResponseEntity<Void> createReview(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String statId, //url에서 충전소 id받음
            @RequestBody ReviewRequest request) { //json으로 content, rating 받음
        reviewService.createReview(userDetails.getUser(), statId, request);
        return ResponseEntity.ok().build();
    }

    // 리뷰 수정
    // input : reviewId(Long), ReviewRequest { content, rating }
    // output: 없음 (200 OK)
    @PatchMapping("/{reviewId}")
    public ResponseEntity<Void> updateReview(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long reviewId,
            @RequestBody ReviewRequest request) {
        reviewService.updateReview(reviewId, userDetails.getUser(), request);
        return ResponseEntity.ok().build();
    }

    // 리뷰 삭제
    // input : reviewId(Long)
    // output: 없음 (200 OK)
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> deleteReview(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long reviewId) {
        reviewService.deleteReview(reviewId, userDetails.getUser());
        return ResponseEntity.ok().build();
    }

    // 내 리뷰 목록 조회
    // input : 헤더에 JWT 토큰
    // output: List<UserReviewsResponse>
    @GetMapping("/my")
    public ResponseEntity<List<UserReviewsResponse>> getReviewsByUser(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(reviewService.getReviewsByUser(userDetails.getUser()));
    }

    // 특정 충전소 리뷰 목록 조회
    // input : statId(String)
    // output: List<StationReviewsResponse>
    @GetMapping("/station/{statId}")
    public ResponseEntity<List<StationReviewResponse>> getReviewsByStation(
            @AuthenticationPrincipal CustomUserDetails userDetails, // 비로그인 시 null
            @PathVariable String statId) {
        return ResponseEntity.ok(reviewService.getReviewsByStation(
                userDetails != null ? userDetails.getUser() : null, statId));
    }

    // 특정 충전소 별점 평균/리뷰 개수
    // input : statId(String)
    // output: StationReviewsSummary { averageRating, reviewCount }
    @GetMapping("/station/{statId}/summary")
    public ResponseEntity<StationReviewsSummary> getStationReviewsSummary(
            @PathVariable String statId) { //user없으니까 로그인 안해도됨
        return ResponseEntity.ok(reviewService.getStationReviewsSummary(statId));
    }
}
