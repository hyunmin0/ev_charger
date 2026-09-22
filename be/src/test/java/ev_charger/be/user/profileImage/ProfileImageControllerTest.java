package ev_charger.be.user.profileImage;

import ev_charger.be.config.SecurityConfig;
import ev_charger.be.security.CustomUserDetailsService;
import ev_charger.be.security.JwtProvider;
import ev_charger.be.user.profileImage.dto.response.ProfileImageResponse;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = ProfileImageController.class,
        excludeAutoConfiguration = {
                OAuth2ClientAutoConfiguration.class,
                OAuth2ClientWebSecurityAutoConfiguration.class
        }
)
@Import(SecurityConfig.class)
class ProfileImageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProfileImageService profileImageService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private RedisTemplate<String, String> redisTemplate;

    // ===== 샘플 데이터 =====

    // 프로필 이미지 목록 조회 응답에 들어갈 이미지 정보
    private static final Integer PROFILE_IMAGE_ID = 1;
    private static final String IMAGE_URL = "https://example.com/profile/1.png";
    private static final String IMAGE_NAME = "기본 이미지 1";
    private static final Integer SECOND_PROFILE_IMAGE_ID = 2;
    private static final String SECOND_IMAGE_URL = "https://example.com/profile/2.png";
    private static final String SECOND_IMAGE_NAME = "기본 이미지 2";

    // 프로필 이미지 목록 조회 응답
    private List<ProfileImageResponse> profileImageResponses;

    private long startTime;

    @BeforeEach
    void setUp() {
        startTime = System.currentTimeMillis();

        profileImageResponses = List.of(
                new ProfileImageResponse(PROFILE_IMAGE_ID, IMAGE_URL, IMAGE_NAME),
                new ProfileImageResponse(SECOND_PROFILE_IMAGE_ID, SECOND_IMAGE_URL, SECOND_IMAGE_NAME)
        );
    }

    @AfterEach
    void tearDown(TestInfo testInfo) {
        System.out.println(testInfo.getDisplayName() + " 경과 시간: " + (System.currentTimeMillis() - startTime) + "ms");
    }

    // ========== GET /profile-images ==========

    @Test
    void 프로필_이미지_목록_조회시_인증_없이도_200과_이미지_목록을_반환한다() throws Exception {
        // given
        given(profileImageService.getProfileImageList()).willReturn(profileImageResponses);

        // when
        mockMvc.perform(get("/profile-images"))

        // then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(PROFILE_IMAGE_ID))
                .andExpect(jsonPath("$[0].imageUrl").value(IMAGE_URL))
                .andExpect(jsonPath("$[0].name").value(IMAGE_NAME))
                .andExpect(jsonPath("$[1].id").value(SECOND_PROFILE_IMAGE_ID))
                .andExpect(jsonPath("$[1].imageUrl").value(SECOND_IMAGE_URL))
                .andExpect(jsonPath("$[1].name").value(SECOND_IMAGE_NAME));

        verify(profileImageService).getProfileImageList();
    }

    @Test
    void 프로필_이미지_목록_조회시_등록된_이미지가_없으면_빈_리스트를_반환한다() throws Exception {
        // given
        given(profileImageService.getProfileImageList()).willReturn(List.of());

        // when
        mockMvc.perform(get("/profile-images"))

                // then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());

        verify(profileImageService).getProfileImageList();
    }
}
