package ev_charger.be.review;

import ev_charger.be.review.dto.request.ReviewRequest;
import ev_charger.be.review.dto.response.StationReviewsResponse;
import ev_charger.be.review.dto.response.StationReviewsSummary;
import ev_charger.be.review.dto.response.UserReviewsResponse;
import ev_charger.be.review.reviewImage.ReviewImage;
import ev_charger.be.review.reviewImage.ReviewImageRepository;
import ev_charger.be.station.Station;
import ev_charger.be.station.StationRepository;
import ev_charger.be.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final StationRepository stationRepository;
    private final ReviewImageRepository reviewImageRepository;

    /**
     * 리뷰 생성
     * @param user
     * @param statId
     * @param request rating, content
     */
    @Transactional
    public void createReview(User user, String statId, ReviewRequest request) {
        Station station = stationRepository.findById(statId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 충전소입니다."));

        if (reviewRepository.existsByUserAndStation(user, station)){
            throw new IllegalArgumentException("이미 등록한 리뷰가 있습니다.");
        }

        reviewRepository.save(Review.builder()
                .user(user)
                .station(station)
                .content(request.content())
                .rating(request.rating())
                .build());
    }

    /**
     * 리뷰 수정
     * @param reviewId
     * @param user
     * @param request rating, content
     */
    @Transactional
    public void updateReview(Long reviewId, User user, ReviewRequest request) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 리뷰입니다."));

        if (!review.getUser().equals(user)) {
            throw new IllegalArgumentException("본인 리뷰만 수정할 수 있습니다.");
        }

        review.updatedContentAndRating(request);
    }

    /**
     * 리뷰 삭제
     * @param reviewId
     * @param user
     */
    @Transactional
    public void deleteReview(Long reviewId, User user) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 리뷰입니다."));

        if (!review.getUser().equals(user)) {
            throw new IllegalArgumentException("본인 리뷰만 삭제할 수 있습니다.");
        }

        reviewRepository.delete(review);
    }

    /**
     * 유저의 리뷰 조회
     * @param user
     * @return 리뷰id, 충전소id, 충전소이름, 별점, 내용, 이미지링크리스트, 생성날짜, 수정여부
     */
    public List<UserReviewsResponse> getReviewsByUser(User user) {

        List<Review> reviews = reviewRepository.findByUser(user);

        Map<Long, List<String>> imageMap = getImageMap(reviews);

        return reviews.stream()
                .map(r -> new UserReviewsResponse(
                        r.getReviewId(),
                        r.getStation().getStatId(), // 리뷰를 통해 충전소로 이동할 수 있도록
                        r.getStation().getStatNm(),
                        r.getRating(),
                        r.getContent(),
                        imageMap.getOrDefault(r.getReviewId(), List.of()), // 이미지 없으면 빈 리스트
                        r.getCreatedAt(),
                        !r.getCreatedAt().equals(r.getUpdatedAt()) // 수정여부
                ))
                .toList();

    }

    /**
     * 특정 충전소의 리뷰 조회
     * @param statId
     * @return 리뷰id, 유저 이름, 프로필사진url 별점, 내용, 이미지링크리스트, 생성날짜, 수정여부
     */
    public List<StationReviewsResponse> getReviewsByStation(String statId) {
        Station station = stationRepository.findById(statId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 충전소입니다."));

        List<Review> reviews = reviewRepository.findByStation(station);

        Map<Long, List<String>> imageMap = getImageMap(reviews);

        return reviews.stream()
                .map(r -> new StationReviewsResponse(
                        r.getReviewId(),
                        r.getUser().getNickname(),
                        r.getUser().getProfileImage() != null // 프로필 사진이 없으면 null
                                ? r.getUser().getProfileImage().getImageUrl() : null,
                        r.getRating(),
                        r.getContent(),
                        imageMap.getOrDefault(r.getReviewId(), List.of()), // 이미지 없으면 빈 리스트
                        r.getCreatedAt(),
                        !r.getCreatedAt().equals(r.getUpdatedAt()) // 수정여부
                ))
                .toList();

    }

    /**
     * 리뷰id와 리뷰이미지url을 매핑
     * @param reviews
     * @return map<reviewId, List<iamgeUrl>>
     */
    private Map<Long, List<String>> getImageMap(List<Review> reviews) {
        return reviewImageRepository.findByReviewIn(reviews).stream()
                .collect(Collectors.groupingBy(
                        img -> img.getReview().getReviewId(), // reviewId를 기준으로 그룹핑
                        Collectors.mapping(ReviewImage::getImageUrl, Collectors.toList())  // 각 그룹에서 imageUrl만 추출해 리스트로
                ));
    }

    /**
     * 평균별점, 리뷰개수
     * @param statId
     * @return 평균별점, 리뷰개수
     */
    public StationReviewsSummary getStationReviewsSummary(String statId) {
        Station station = stationRepository.findById(statId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 충전소입니다."));

        // 별점 리스트 가져오기
        // List<int>는 사용 불가
        List<Integer> ratings = reviewRepository.findRatingsByStation(station);

        // 별점 평균값
        double averageRating = ratings.stream()
                .mapToInt(rating -> rating) // Integer -> int
                // average()는 IntStream에 있어서 mapToInt가 필요함
                .average()// 평균 계산 결과: Optional<Double>
                .orElse(0);

        return new StationReviewsSummary(averageRating, ratings.size());
    }

}
