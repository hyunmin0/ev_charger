package ev_charger.be.notice;

import ev_charger.be.config.SecurityConfig;
import ev_charger.be.notice.dto.request.NoticeCreateRequest;
import ev_charger.be.notice.dto.response.NoticeResponse;
import ev_charger.be.security.CustomUserDetails;
import ev_charger.be.security.CustomUserDetailsService;
import ev_charger.be.security.JwtProvider;
import ev_charger.be.user.User;
import ev_charger.be.user.enums.Provider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
        controllers = NoticeController.class,
        excludeAutoConfiguration = {
                OAuth2ClientAutoConfiguration.class,
                OAuth2ClientWebSecurityAutoConfiguration.class
        }
)
@Import(SecurityConfig.class)
class NoticeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private NoticeService noticeService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private RedisTemplate<String, String> redisTemplate;

    private long startTime;
    private User user;
    private User admin;

    @BeforeEach
    void setUp() {
        startTime = System.currentTimeMillis();

        user = User.builder()
                .nickname("테스터")
                .email("test@example.com")
                .provider(Provider.GOOGLE)
                .providerId("google-1234")
                .build();
        ReflectionTestUtils.setField(user, "userId", UUID.randomUUID());

        admin = User.builder()
                .nickname("관리자")
                .email("admin@example.com")
                .provider(Provider.GOOGLE)
                .providerId("google-5678")
                .build();
        ReflectionTestUtils.setField(admin, "userId", UUID.randomUUID());
        admin.promoteToAdmin();
    }

    @AfterEach
    void tearDown(TestInfo testInfo) {
        System.out.println(testInfo.getDisplayName() + " 경과 시간: " + (System.currentTimeMillis() - startTime) + "ms");
    }

    @Test
    void 관리자가_공지를_등록하면_200을_반환한다() throws Exception {
        // given
        Authentication auth = authOf(admin);

        NoticeCreateRequest request = new NoticeCreateRequest("점검 안내", "9/10 점검 예정입니다.");

        // when
        mockMvc.perform(post("/notices")
                .with(authentication(auth))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // then
        verify(noticeService).createNotice(request);
    }

    @Test
    void 일반_유저가_공지를_등록하면_403을_반환한다() throws Exception {
        // given
        Authentication auth = authOf(user);

        NoticeCreateRequest request = new NoticeCreateRequest("점검 안내", "9/10 점검 예정입니다.");

        // when
        mockMvc.perform(post("/notices")
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        // then
        verify(noticeService, never()).createNotice(any());
    }

    @Test
    void 로그인하지_않고_공지를_등록하면_403을_반환한다() throws Exception {
        // given
        NoticeCreateRequest request = new NoticeCreateRequest("점검 안내", "9/10 점검 예정입니다.");

        // when
        mockMvc.perform(post("/notices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        // then
        verify(noticeService, never()).createNotice(any());
    }

    @Test
    void 이미_존재하는_공지를_등록하면_400을_반환한다() throws Exception {
        // given
        Authentication auth = authOf(admin);

        NoticeCreateRequest request = new NoticeCreateRequest("점검 안내", "9/10 점검 예정입니다.");

        willThrow(new IllegalArgumentException("이미 존재하는 공지입니다."))
                .given(noticeService).createNotice(request);

        // when
        mockMvc.perform(post("/notices")
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("이미 존재하는 공지입니다."));
    }

    @Test
    void 공지_단건_조회_성공시_200과_공지를_반환한다() throws Exception {
        // given
        Authentication auth = authOf(user);

        given(noticeService.getNotice(user, 1L))
                .willReturn(new NoticeResponse(1L, "점검 안내", "9/10 점검 예정입니다.", LocalDateTime.now()));

        // when
        mockMvc.perform(get("/notices/{noticeId}", 1L)
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("점검 안내"))
                .andExpect(jsonPath("$.content").value("9/10 점검 예정입니다."));
    }

    @Test
    void 존재하지_않는_공지를_조회하면_400을_반환한다() throws Exception {
        // given
        Authentication auth = authOf(user);

        willThrow(new IllegalArgumentException("유효하지 않은 공지입니다."))
                .given(noticeService).getNotice(user, 1L);

        // when
        mockMvc.perform(get("/notices/{noticeId}", 1L)
                        .with(authentication(auth)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("유효하지 않은 공지입니다."));
    }

    @Test
    void 로그인하지_않고_공지를_조회하면_200을_반환한다() throws Exception {
        // when
        mockMvc.perform(get("/notices/{noticeId}", 1L))
                .andExpect(status().isOk());

        // then
        verify(noticeService).getNotice(null, 1L);
    }

    @Test
    void 공지_목록_조회시_기본값은_0페이지_10개_최신순이다() throws Exception {
        // given
        Authentication auth = authOf(user);

        // when
        mockMvc.perform(get("/notices")
                        .with(authentication(auth)))
                .andExpect(status().isOk());

        // then
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(noticeService).getNotices(eq(user), captor.capture());

        Pageable pageable = captor.getValue();
        assertThat(pageable.getPageNumber()).isEqualTo(0);
        assertThat(pageable.getPageSize()).isEqualTo(10);
        assertThat(pageable.getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @Test
    void 공지_목록_조회시_요청한_페이지와_크기가_전달된다() throws Exception {
        // given
        Authentication auth = authOf(user);

        // when
        mockMvc.perform(get("/notices")
                        .param("page", "2")
                        .param("size", "5")
                        .with(authentication(auth)))
                .andExpect(status().isOk());

        // then
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(noticeService).getNotices(eq(user), captor.capture());

        Pageable pageable = captor.getValue();
        assertThat(pageable.getPageNumber()).isEqualTo(2);
        assertThat(pageable.getPageSize()).isEqualTo(5);
        assertThat(pageable.getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @Test
    void 로그인하지_않고_공지_목록을_조회하면_200을_반환한다() throws Exception {
        // when
        mockMvc.perform(get("/notices"))
                .andExpect(status().isOk());

        // then
        verify(noticeService).getNotices(isNull(), any());
    }

    private Authentication authOf(User user) {
        CustomUserDetails principal = new CustomUserDetails(user);
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }
}
