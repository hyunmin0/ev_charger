package ev_charger.be.review;

import ev_charger.be.config.SecurityConfig;
import ev_charger.be.review.dto.request.ReviewRequest;
import ev_charger.be.review.dto.response.StationReviewResponse;
import ev_charger.be.review.dto.response.StationReviewsSummary;
import ev_charger.be.review.dto.response.UserReviewsResponse;
import ev_charger.be.security.CustomUserDetails;
import ev_charger.be.security.CustomUserDetailsService;
import ev_charger.be.security.JwtProvider;
import ev_charger.be.user.User;
import ev_charger.be.user.enums.Provider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = ReviewController.class,
        excludeAutoConfiguration = {
                OAuth2ClientAutoConfiguration.class,
                OAuth2ClientWebSecurityAutoConfiguration.class
        }
)
@Import(SecurityConfig.class)
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ReviewService reviewService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private RedisTemplate<String, String> redisTemplate;

    // ===== 샘플 데이터 =====

    // 로그인한 유저 정보
    private static final String NICKNAME = "테스터";
    private static final String EMAIL = "test@example.com";
    private static final String PROVIDER_ID = "google-1234";
    private static final String PROFILE_IMAGE_URL = "https://example.com/profile.png";

    // 리뷰 작성 대상 충전소
    private static final String STAT_ID = "ME000001";
    private static final String STAT_NM = "서울시청 충전소";
    private static final String BUSI_NM = "환경부";
    private static final String ADDR = "서울특별시 중구 세종대로 110";
    // DB에 없는 충전소 id
    private static final String INVALID_STAT_ID = "INVALID";

    // 리뷰 작성/수정 요청 본문 값
    private static final int RATING = 5;
    private static final String CONTENT = "충전 속도가 빨라요";
    private static final int UPDATED_RATING = 3;
    private static final String UPDATED_CONTENT = "주차가 불편해요";

    // 수정/삭제 대상 리뷰
    private static final long REVIEW_ID = 1L;
    private static final String IMAGE_URL = "https://example.com/review.png";
    private static final long SECOND_REVIEW_ID = 2L;
    private static final String SECOND_NICKNAME = "다른유저";
    private static final int SECOND_RATING = 4;
    private static final String SECOND_CONTENT = "깨끗해요";
    // DB에 없는 리뷰 id
    private static final long INVALID_REVIEW_ID = 999L;
    // Long PathVariable에 숫자가 아닌 값을 넣는 타입 불일치 케이스
    private static final String NOT_NUMBER_VALUE = "abc";

    // 충전소 별점 요약
    private static final double AVERAGE_RATING = 4.5;
    private static final int REVIEW_COUNT = 2;

    // 로그인한 유저
    private User user;
    // @AuthenticationPrincipal로 주입될 인증 정보
    private Authentication auth;
    // 리뷰 작성 요청 본문
    private ReviewRequest reviewRequest;
    // 리뷰 수정 요청 본문
    private ReviewRequest updateReviewRequest;
    // 내 리뷰 목록 조회 응답
    private List<UserReviewsResponse> userReviewsResponses;
    // 비로그인 충전소 리뷰 목록 조회 응답 (isMyReview = 모두 false)
    private List<StationReviewResponse> guestStationReviewResponses;
    // 로그인 충전소 리뷰 목록 조회 응답 (첫 번째 리뷰만 isMyReview = true)
    private List<StationReviewResponse> userStationReviewResponses;
    // 충전소 별점 요약 응답
    private StationReviewsSummary stationReviewsSummary;

    private long startTime;

    @BeforeEach
    void setUp() {
        startTime = System.currentTimeMillis();

        user = User.builder()
                .nickname(NICKNAME)
                .email(EMAIL)
                .provider(Provider.GOOGLE)
                .providerId(PROVIDER_ID)
                .build();
        ReflectionTestUtils.setField(user, "userId", UUID.randomUUID());

        CustomUserDetails principal = new CustomUserDetails(user);
        auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        reviewRequest = new ReviewRequest(RATING, CONTENT);
        updateReviewRequest = new ReviewRequest(UPDATED_RATING, UPDATED_CONTENT);

        userReviewsResponses = List.of(
                new UserReviewsResponse(REVIEW_ID, STAT_ID, STAT_NM, BUSI_NM, ADDR, RATING, CONTENT,
                        List.of(IMAGE_URL), LocalDateTime.now(), false)
        );

        guestStationReviewResponses = List.of(
                new StationReviewResponse(REVIEW_ID, NICKNAME, PROFILE_IMAGE_URL, RATING, CONTENT,
                        List.of(IMAGE_URL), LocalDateTime.now(), false, false),
                new StationReviewResponse(SECOND_REVIEW_ID, SECOND_NICKNAME, PROFILE_IMAGE_URL, SECOND_RATING, SECOND_CONTENT,
                        List.of(), LocalDateTime.now().minusDays(1), false, true)
        );
        userStationReviewResponses = List.of(
                new StationReviewResponse(REVIEW_ID, NICKNAME, PROFILE_IMAGE_URL, RATING, CONTENT,
                        List.of(IMAGE_URL), LocalDateTime.now(), true, false),
                new StationReviewResponse(SECOND_REVIEW_ID, SECOND_NICKNAME, PROFILE_IMAGE_URL, SECOND_RATING, SECOND_CONTENT,
                        List.of(), LocalDateTime.now().minusDays(1), false, true)
        );

        stationReviewsSummary = new StationReviewsSummary(AVERAGE_RATING, REVIEW_COUNT);
    }

    @AfterEach
    void tearDown(TestInfo testInfo) {
        System.out.println(testInfo.getDisplayName() + " 경과 시간: " + (System.currentTimeMillis() - startTime) + "ms");
    }

    // ========== POST /reviews/{statId} ==========

    @Test
    void 리뷰_작성_성공시_200을_반환한다() throws Exception {
        // when
        mockMvc.perform(post("/reviews/{statId}", STAT_ID)
                .with(authentication(auth))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reviewRequest)))

        // then
                .andExpect(status().isOk());

        verify(reviewService).createReview(user, STAT_ID, reviewRequest);
    }

    @Test
    void 리뷰_작성시_요청_본문이_없으면_400을_반환한다() throws Exception {
        // when
        mockMvc.perform(post("/reviews/{statId}", STAT_ID)
                        .with(authentication(auth)))

                // then
                .andExpect(status().isBadRequest());

        verify(reviewService, never()).createReview(any(), any(), any());
    }

    @Test
    void 리뷰_작성시_존재하지_않는_충전소면_400을_반환한다() throws Exception {
        // given
        willThrow(new IllegalArgumentException("존재하지 않는 충전소입니다.")).given(reviewService).createReview(user,  INVALID_STAT_ID, reviewRequest);

        // when
        mockMvc.perform(post("/reviews/{statId}", INVALID_STAT_ID)
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reviewRequest)))

                // then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("존재하지 않는 충전소입니다."));

        verify(reviewService).createReview(user, INVALID_STAT_ID, reviewRequest);
    }

    @Test
    void 리뷰_작성시_이미_등록한_리뷰가_있으면_400을_반환한다() throws Exception {
        // given
        willThrow(new IllegalArgumentException("이미 등록한 리뷰가 있습니다.")).given(reviewService).createReview(user,  STAT_ID, reviewRequest);

        // when
        mockMvc.perform(post("/reviews/{statId}", STAT_ID)
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reviewRequest)))

                // then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("이미 등록한 리뷰가 있습니다."));

        verify(reviewService).createReview(user, STAT_ID, reviewRequest);
    }

    @Test
    void 리뷰_작성시_인증_정보가_없으면_403을_반환한다() throws Exception {
        // when
        mockMvc.perform(post("/reviews/{statId}", INVALID_STAT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reviewRequest)))

                // then
                .andExpect(status().isForbidden());

        verify(reviewService, never()).createReview(any(), any(), any());
    }

    // ========== PATCH /reviews/{reviewId} ==========

    @Test
    void 리뷰_수정_성공시_200을_반환한다() throws Exception {
        // when
        mockMvc.perform(patch("/reviews/{reviewId}", REVIEW_ID)
                .with(authentication(auth))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateReviewRequest)))

        // then
                .andExpect(status().isOk());

        verify(reviewService).updateReview(REVIEW_ID, user, updateReviewRequest);
    }

    @Test
    void 리뷰_수정시_reviewId가_숫자가_아니면_400을_반환한다() throws Exception {
        // given

        // when
        mockMvc.perform(patch("/reviews/{reviewId}", NOT_NUMBER_VALUE)
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReviewRequest)))

                // then
                .andExpect(status().isBadRequest());

        verify(reviewService, never()).updateReview(any(), any(), any());
    }

    @Test
    void 리뷰_수정시_존재하지_않는_리뷰면_400을_반환한다() throws Exception {
        // given
        willThrow(new IllegalArgumentException("존재하지 않는 리뷰입니다.")).given(reviewService).updateReview(INVALID_REVIEW_ID, user, updateReviewRequest);

        // when
        mockMvc.perform(patch("/reviews/{reviewId}", INVALID_REVIEW_ID)
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReviewRequest)))

                // then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("존재하지 않는 리뷰입니다."));

        verify(reviewService).updateReview(INVALID_REVIEW_ID, user, updateReviewRequest);
    }

    @Test
    void 리뷰_수정시_본인_리뷰가_아니면_400을_반환한다() throws Exception {
        // given
        willThrow(new IllegalArgumentException("본인 리뷰만 수정할 수 있습니다.")).given(reviewService).updateReview(REVIEW_ID, user, updateReviewRequest);

        // when
        mockMvc.perform(patch("/reviews/{reviewId}", REVIEW_ID)
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReviewRequest)))

                // then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("본인 리뷰만 수정할 수 있습니다."));

        verify(reviewService).updateReview(REVIEW_ID, user, updateReviewRequest);
    }

    @Test
    void 리뷰_수정시_인증_정보가_없으면_403을_반환한다() throws Exception {
        // when
        mockMvc.perform(patch("/reviews/{reviewId}", REVIEW_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReviewRequest)))

                // then
                .andExpect(status().isForbidden());

        verify(reviewService, never()).updateReview(any(), any(), any());
    }

    // ========== DELETE /reviews/{reviewId} ==========

    @Test
    void 리뷰_삭제_성공시_200을_반환한다() throws Exception {
        // when
        mockMvc.perform(delete("/reviews/{reviewId}", REVIEW_ID)
                .with(authentication(auth)))

        // then
                .andExpect(status().isOk());

        verify(reviewService).deleteReview(REVIEW_ID, user);
    }

    @Test
    void 리뷰_삭제시_reviewId가_숫자가_아니면_400을_반환한다() throws Exception {
        // when
        mockMvc.perform(delete("/reviews/{reviewId}", NOT_NUMBER_VALUE)
                        .with(authentication(auth)))

                // then
                .andExpect(status().isBadRequest());

        verify(reviewService, never()).deleteReview(any(), any());
    }

    @Test
    void 리뷰_삭제시_존재하지_않는_리뷰면_400을_반환한다() throws Exception {
        // given
        willThrow(new IllegalArgumentException("존재하지 않는 리뷰입니다.")).given(reviewService).deleteReview(INVALID_REVIEW_ID, user);

        // when
        mockMvc.perform(delete("/reviews/{reviewId}", INVALID_REVIEW_ID)
                        .with(authentication(auth)))

                // then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("존재하지 않는 리뷰입니다."));

        verify(reviewService).deleteReview(INVALID_REVIEW_ID, user);
    }

    @Test
    void 리뷰_삭제시_본인_리뷰가_아니면_400을_반환한다() throws Exception {
        // given
        willThrow(new IllegalArgumentException("본인 리뷰만 삭제할 수 있습니다.")).given(reviewService).deleteReview(REVIEW_ID, user);

        // when
        mockMvc.perform(delete("/reviews/{reviewId}", REVIEW_ID)
                        .with(authentication(auth)))

                // then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("본인 리뷰만 삭제할 수 있습니다."));

        verify(reviewService).deleteReview(REVIEW_ID, user);
    }

    @Test
    void 리뷰_삭제시_인증_정보가_없으면_403을_반환한다() throws Exception {
        // when
        mockMvc.perform(delete("/reviews/{reviewId}", REVIEW_ID))

                // then
                .andExpect(status().isForbidden());

        verify(reviewService, never()).deleteReview(any(), any());
    }

    // ========== GET /reviews/my ==========

    @Test
    void 내_리뷰_목록_조회_성공시_200과_리뷰_목록을_반환한다() throws Exception {
        // given
        given(reviewService.getReviewsByUser(user)).willReturn(userReviewsResponses);

        // when
        mockMvc.perform(get("/reviews/my")
                .with(authentication(auth)))

        // then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(userReviewsResponses.size()))
                .andExpect(jsonPath("$[0].reviewId").value(REVIEW_ID))
                .andExpect(jsonPath("$[0].statId").value(STAT_ID))
                .andExpect(jsonPath("$[0].statNm").value(STAT_NM))
                .andExpect(jsonPath("$[0].busiNm").value(BUSI_NM))
                .andExpect(jsonPath("$[0].addr").value(ADDR))
                .andExpect(jsonPath("$[0].rating").value(RATING))
                .andExpect(jsonPath("$[0].content").value(CONTENT))
                .andExpect(jsonPath("$[0].imageUrls[0]").value(IMAGE_URL))
                .andExpect(jsonPath("$[0].isEdited").value(false));

        verify(reviewService).getReviewsByUser(user);
    }

    @Test
    void 내_리뷰_목록_조회시_작성한_리뷰가_없으면_빈_리스트를_반환한다() throws Exception {
        // given
        given(reviewService.getReviewsByUser(user)).willReturn(List.of());

        // when
        mockMvc.perform(get("/reviews/my")
                        .with(authentication(auth)))

                // then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());

        verify(reviewService).getReviewsByUser(user);
    }

    @Test
    void 내_리뷰_목록_조회시_인증_정보가_없으면_403을_반환한다() throws Exception {
        // when
        mockMvc.perform(get("/reviews/my"))

                // then
                .andExpect(status().isForbidden());

        verify(reviewService, never()).getReviewsByUser(any());
    }

    // ========== GET /reviews/station/{statId} ==========

    @Test
    void 충전소_리뷰_목록_조회시_비로그인이면_user를_null로_넘기고_200을_반환한다() throws Exception {
        // given
        given(reviewService.getReviewsByStation(null, STAT_ID)).willReturn(guestStationReviewResponses);

        // when
        mockMvc.perform(get("/reviews/station/{statId}", STAT_ID))

        // then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(guestStationReviewResponses.size()))
                .andExpect(jsonPath("$[0].reviewId").value(REVIEW_ID))
                .andExpect(jsonPath("$[0].nickname").value(NICKNAME))
                .andExpect(jsonPath("$[0].rating").value(RATING))
                .andExpect(jsonPath("$[0].content").value(CONTENT))
                .andExpect(jsonPath("$[0].isMyReview").value(false))
                .andExpect(jsonPath("$[1].reviewId").value(SECOND_REVIEW_ID))
                .andExpect(jsonPath("$[1].isMyReview").value(false));

        verify(reviewService).getReviewsByStation(null, STAT_ID);
    }

    @Test
    void 충전소_리뷰_목록_조회시_로그인이면_user를_넘기고_내_리뷰_여부를_반환한다() throws Exception {
        // given
        given(reviewService.getReviewsByStation(user, STAT_ID)).willReturn(userStationReviewResponses);

        // when
        mockMvc.perform(get("/reviews/station/{statId}", STAT_ID)
                        .with(authentication(auth)))

                // then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(userStationReviewResponses.size()))
                .andExpect(jsonPath("$[0].reviewId").value(REVIEW_ID))
                .andExpect(jsonPath("$[0].isMyReview").value(true))
                .andExpect(jsonPath("$[1].reviewId").value(SECOND_REVIEW_ID))
                .andExpect(jsonPath("$[1].isMyReview").value(false));

        verify(reviewService).getReviewsByStation(user, STAT_ID);
    }

    @Test
    void 충전소_리뷰_목록_조회시_리뷰가_없으면_빈_리스트를_반환한다() throws Exception {
        // given
        given(reviewService.getReviewsByStation(null, STAT_ID)).willReturn(List.of());

        // when
        mockMvc.perform(get("/reviews/station/{statId}", STAT_ID))

                // then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());

        verify(reviewService).getReviewsByStation(null, STAT_ID);
    }

    // ========== GET /reviews/station/{statId}/summary ==========

    @Test
    void 충전소_별점_요약_조회_성공시_200과_평균_별점과_리뷰_개수를_반환한다() throws Exception {
        // given
        given(reviewService.getStationReviewsSummary(STAT_ID)).willReturn(stationReviewsSummary);

        // when
        mockMvc.perform(get("/reviews/station/{statId}/summary", STAT_ID))

        // then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageRating").value(AVERAGE_RATING))
                .andExpect(jsonPath("$.reviewCount").value(REVIEW_COUNT));

        verify(reviewService).getStationReviewsSummary(STAT_ID);
    }

    @Test
    void 충전소_별점_요약_조회시_리뷰가_없으면_평균_별점은_null이고_리뷰_개수는_0을_반환한다() throws Exception {
        // given
        given(reviewService.getStationReviewsSummary(STAT_ID)).willReturn(new StationReviewsSummary(null, 0));

        // when
        mockMvc.perform(get("/reviews/station/{statId}/summary", STAT_ID))

                // then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageRating").isEmpty())
                .andExpect(jsonPath("$.reviewCount").value(0));

        verify(reviewService).getStationReviewsSummary(STAT_ID);
    }

    @Test
    void 충전소_별점_요약_조회시_존재하지_않는_충전소면_400을_반환한다() throws Exception {
        // given
        given(reviewService.getStationReviewsSummary(INVALID_STAT_ID)).willThrow(new IllegalArgumentException("존재하지 않는 충전소입니다."));

        // when
        mockMvc.perform(get("/reviews/station/{statId}/summary", INVALID_STAT_ID))

                // then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("존재하지 않는 충전소입니다."));

        verify(reviewService).getStationReviewsSummary(INVALID_STAT_ID);
    }
}
