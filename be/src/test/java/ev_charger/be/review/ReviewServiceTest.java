package ev_charger.be.review;

import ev_charger.be.review.dto.request.ReviewRequest;
import ev_charger.be.review.dto.response.StationReviewResponse;
import ev_charger.be.review.dto.response.StationReviewsSummary;
import ev_charger.be.review.dto.response.UserReviewsResponse;
import ev_charger.be.station.Station;
import ev_charger.be.station.stationOperator.StationOperator;
import ev_charger.be.user.User;
import ev_charger.be.user.enums.Provider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ev_charger.be.review.reviewImage.ReviewImage;
import ev_charger.be.review.reviewImage.ReviewImageRepository;
import ev_charger.be.station.StationRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private StationRepository stationRepository;
    @Mock
    private ReviewImageRepository reviewImageRepository;

    private ReviewService reviewService;

    private long startTime;

    @BeforeEach
    void setUp() {
        startTime = System.currentTimeMillis();
        reviewService = new ReviewService(
                reviewRepository,
                stationRepository,
                reviewImageRepository
        );
    }

    @Test
    void 리뷰_생성_성공() {
        // given
        User user = User.builder()
                .nickname("nick")
                .email("test@test.com")
                .provider(Provider.GOOGLE)
                .providerId("google-sub")
                .build();

        Station station = Station.builder()
                .statId("ST12345")
                .statNm("테스트충전소")
                .build();

        String statId = "ST12345";
        ReviewRequest request = new ReviewRequest(5, "content");

        given(stationRepository.findById(statId)).willReturn(Optional.ofNullable(station));
        given(reviewRepository.existsByUserAndStation(user, station)).willReturn(Boolean.FALSE);

        // when
        reviewService.createReview(user, statId, request);

        // then
        ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
        verify(reviewRepository).save(captor.capture());

        Review saved = captor.getValue();
        assertThat(saved.getUser()).isEqualTo(user);
        assertThat(saved.getStation()).isEqualTo(station);
        assertThat(saved.getContent()).isEqualTo("content");
        assertThat(saved.getRating()).isEqualTo(5);
    }

    @Test
    void 존재하지_않는_충전소면_리뷰_생성시_예외_발생() {
        // given
        User user = User.builder()
                .nickname("nick")
                .email("test@test.com")
                .provider(Provider.GOOGLE)
                .providerId("google-sub")
                .build();

        String statId = "ST12345";
        ReviewRequest request = new ReviewRequest(5, "content");

        // when & then
        assertThatThrownBy(() -> reviewService.createReview(user, statId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 충전소입니다.");
    }

    @Test
    void 이미_등록한_리뷰가_있으면_생성시_예외_발생() {
        // given
        User user = User.builder()
                .nickname("nick")
                .email("test@test.com")
                .provider(Provider.GOOGLE)
                .providerId("google-sub")
                .build();

        Station station = Station.builder()
                .statId("ST12345")
                .statNm("테스트충전소")
                .build();

        String statId = "ST12345";
        ReviewRequest request = new ReviewRequest(5, "content");

        given(stationRepository.findById(statId)).willReturn(Optional.ofNullable(station));
        given(reviewRepository.existsByUserAndStation(user, station)).willReturn(Boolean.TRUE);

        // when & then
        assertThatThrownBy(() -> reviewService.createReview(user, statId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이미 등록한 리뷰가 있습니다.");
    }

    @Test
    void 리뷰_수정_성공() {
        // given
        Long reviewId = 1L;
        User user = mock(User.class);
        given(user.getUserId()).willReturn(UUID.randomUUID());

        Review review = mock(Review.class);
        given(review.getUser()).willReturn(user);

        ReviewRequest request = new ReviewRequest(5, "updated-content");

        given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review));

        // when
        reviewService.updateReview(reviewId, user, request);

        // then
        verify(review).updatedContentAndRating(request);

    }

    @Test
    void 존재하지_않는_리뷰면_수정시_예외_발생() {
        // given
        Long reviewId = 1L;
        User user = User.builder()
                .nickname("nick")
                .email("test@test.com")
                .provider(Provider.GOOGLE)
                .providerId("google-sub")
                .build();

        ReviewRequest request = new ReviewRequest(5, "updated-content");

        // when & then
        assertThatThrownBy(() -> reviewService.updateReview(reviewId, user, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 리뷰입니다.");
    }

    @Test
    void 본인_리뷰가_아니면_수정시_예외_발생() {
        // given
        Long reviewId = 1L;
        User user = mock(User.class);
        given(user.getUserId()).willReturn(UUID.randomUUID());

        User other = mock(User.class);
        given(other.getUserId()).willReturn(UUID.randomUUID());

        Review review = mock(Review.class);
        given(review.getUser()).willReturn(other);

        ReviewRequest request = new ReviewRequest(5, "updated-content");

        given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review));

        // when & then
        assertThatThrownBy(() -> reviewService.updateReview(reviewId, user, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("본인 리뷰만 수정할 수 있습니다.");
    }

    @Test
    void 리뷰_삭제_성공() {
        // given
        Long reviewId = 1L;
        User user = mock(User.class);
        given(user.getUserId()).willReturn(UUID.randomUUID());

        Review review = mock(Review.class);
        given(review.getUser()).willReturn(user);

        given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review));

        // when
        reviewService.deleteReview(reviewId, user);

        // then
        verify(reviewRepository).delete(review);
    }

    @Test
    void 존재하지_않는_리뷰면_삭제시_예외_발생() {
        // given
        Long reviewId = 1L;
        User user = User.builder()
                .nickname("nick")
                .email("test@test.com")
                .provider(Provider.GOOGLE)
                .providerId("google-sub")
                .build();

        // when & then
        assertThatThrownBy(() -> reviewService.deleteReview(reviewId, user))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 리뷰입니다.");
    }

    @Test
    void 본인_리뷰가_아니면_삭제시_예외_발생() {
        // given
        Long reviewId = 1L;
        User user = mock(User.class);
        given(user.getUserId()).willReturn(UUID.randomUUID());

        User other = mock(User.class);
        given(other.getUserId()).willReturn(UUID.randomUUID());

        Review review = mock(Review.class);
        given(review.getUser()).willReturn(other);

        given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review));

        // when & then
        assertThatThrownBy(() -> reviewService.deleteReview(reviewId, user))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("본인 리뷰만 삭제할 수 있습니다.");

        verify(reviewRepository, never()).delete(any());
    }

    @Test
    void 유저_리뷰_목록_조회() {
        // given
        User user = User.builder()
                .nickname("nick")
                .email("test@test.com")
                .provider(Provider.GOOGLE)
                .providerId("google-sub")
                .build();

        StationOperator operator = StationOperator.builder()
                .busiNm("테스트운영사")
                .build();
        Station station = Station.builder()
                .statId("ST12345")
                .statNm("테스트충전소")
                .addr("서울시 강남구 테헤란로 123")
                .stationOperator(operator)
                .build();

        LocalDateTime createdAt = LocalDateTime.now();

        Review review = mock(Review.class);
        given(review.getReviewId()).willReturn(1L);
        given(review.getStation()).willReturn(station);
        given(review.getRating()).willReturn(5);
        given(review.getContent()).willReturn("좋아요");
        given(review.getCreatedAt()).willReturn(createdAt);
        given(review.getUpdatedAt()).willReturn(createdAt); // 생성시간=수정시간 -> 수정 안됨

        ReviewImage image = ReviewImage.builder()
                .review(review)
                .imageUrl("http://example.com/img.png")
                .build();

        given(reviewRepository.findByUser(user)).willReturn(List.of(review));
        given(reviewImageRepository.findByReviewIn(List.of(review))).willReturn(List.of(image));

        // when
        List<UserReviewsResponse> responses = reviewService.getReviewsByUser(user);

        // then
        assertThat(responses).hasSize(1);
        UserReviewsResponse response = responses.get(0);
        assertThat(response.reviewId()).isEqualTo(1L);
        assertThat(response.statId()).isEqualTo("ST12345");
        assertThat(response.statNm()).isEqualTo("테스트충전소");
        assertThat(response.busiNm()).isEqualTo("테스트운영사");
        assertThat(response.addr()).isEqualTo("서울시 강남구 테헤란로 123");
        assertThat(response.rating()).isEqualTo(5);
        assertThat(response.content()).isEqualTo("좋아요");
        assertThat(response.imageUrls()).containsExactly("http://example.com/img.png");
        assertThat(response.isEdited()).isFalse();
    }

    @Test
    void 충전소_리뷰_조회시_로그인유저면_본인리뷰여부가_반영() {
        // given
        User user = mock(User.class);
        given(user.getUserId()).willReturn(UUID.randomUUID());
        String statId = "ST12345";

        StationOperator operator = StationOperator.builder()
                .busiNm("테스트운영사")
                .build();
        Station station = Station.builder()
                .statId(statId)
                .statNm("테스트충전소")
                .addr("서울시 강남구 테헤란로 123")
                .stationOperator(operator)
                .build();

        LocalDateTime createdAt = LocalDateTime.now();

        Review review = mock(Review.class);
        given(review.getReviewId()).willReturn(1L);
        given(review.getUser()).willReturn(user); // 본인이 작성한 리뷰
        given(review.getRating()).willReturn(5);
        given(review.getContent()).willReturn("좋아요");
        given(review.getCreatedAt()).willReturn(createdAt);
        given(review.getUpdatedAt()).willReturn(createdAt);

        given(stationRepository.findById(statId)).willReturn(Optional.of(station));
        given(reviewRepository.findByStation(station)).willReturn(List.of(review));
        given(reviewImageRepository.findByReviewIn(List.of(review))).willReturn(List.of());

        // when
        List<StationReviewResponse> responses = reviewService.getReviewsByStation(user, statId);

        // then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).isMyReview()).isTrue();
    }

    @Test
    void 충전소_리뷰_조회시_비로그인유저면_본인리뷰여부는_false() {
        // given
        User writer = User.builder()
                .nickname("writer")
                .email("writer@test.com")
                .provider(Provider.GOOGLE)
                .providerId("writer-sub")
                .build();
        String statId = "ST12345";

        StationOperator operator = StationOperator.builder()
                .busiNm("테스트운영사")
                .build();
        Station station = Station.builder()
                .statId(statId)
                .statNm("테스트충전소")
                .addr("서울시 강남구 테헤란로 123")
                .stationOperator(operator)
                .build();

        LocalDateTime createdAt = LocalDateTime.now();

        Review review = mock(Review.class);
        given(review.getReviewId()).willReturn(1L);
        given(review.getUser()).willReturn(writer);
        given(review.getRating()).willReturn(5);
        given(review.getContent()).willReturn("좋아요");
        given(review.getCreatedAt()).willReturn(createdAt);
        given(review.getUpdatedAt()).willReturn(createdAt);

        given(stationRepository.findById(statId)).willReturn(Optional.of(station));
        given(reviewRepository.findByStation(station)).willReturn(List.of(review));
        given(reviewImageRepository.findByReviewIn(List.of(review))).willReturn(List.of());

        // when
        List<StationReviewResponse> responses = reviewService.getReviewsByStation(null, statId);

        // then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).isMyReview()).isFalse();
    }

    @Test
    void 존재하지_않는_충전소면_리뷰_조회시_예외_발생() {
        // given
        String statId = "ST12345";

        // when & then
        assertThatThrownBy(() -> reviewService.getReviewsByStation(null, statId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 충전소입니다.");
    }

    @Test
    void 별점이_있으면_평균과_리뷰개수를_반환() {
        // given
        String statId = "ST12345";
        Station station = Station.builder()
                .statId(statId)
                .build();

        given(stationRepository.findById(statId)).willReturn(Optional.of(station));
        given(reviewRepository.findRatingsByStation(station)).willReturn(List.of(5, 4, 4));

        // when
        StationReviewsSummary summary = reviewService.getStationReviewsSummary(statId);

        // then
        assertThat(summary.averageRating()).isEqualTo(4.3);
        assertThat(summary.reviewCount()).isEqualTo(3);
    }

    @Test
    void 별점이_없으면_평균은_null() {
        // given
        String statId = "ST12345";
        Station station = Station.builder()
                .statId(statId)
                .build();

        given(stationRepository.findById(statId)).willReturn(Optional.of(station));
        given(reviewRepository.findRatingsByStation(station)).willReturn(List.of());

        // when
        StationReviewsSummary summary = reviewService.getStationReviewsSummary(statId);

        // then
        assertThat(summary.averageRating()).isNull();
        assertThat(summary.reviewCount()).isEqualTo(0);
    }

    @Test
    void 존재하지_않는_충전소면_요약_조회시_예외_발생() {
        // given
        String statId = "ST12345";

        // when & then
        assertThatThrownBy(() -> reviewService.getStationReviewsSummary(statId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 충전소입니다.");
    }

    @AfterEach
    void tearDown() {
        System.out.println("경과 시간: " + (System.currentTimeMillis()-startTime) + "ms");
    }
}
