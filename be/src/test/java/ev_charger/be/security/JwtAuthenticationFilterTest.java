package ev_charger.be.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class JwtAuthenticationFilterTest {

    @Mock
    private JwtProvider jwtProvider;
    @Mock
    private CustomUserDetailsService customUserDetailsService;
    @Mock
    private RedisTemplate<String, String> redisTemplate;
    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private long startTime;

    @BeforeEach
    void setUp() {
        startTime = System.currentTimeMillis();
    }

    @Test
    void 유효한_토큰이면_인증_정보가_SecurityContext에_저장된다() throws Exception {
        // given
        String token = "valid-token";
        UUID userId = UUID.randomUUID();
        UserDetails userDetails = mock(UserDetails.class);
        List<GrantedAuthority> authorities = List.of();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        given(jwtProvider.validateToken(token)).willReturn(true);
        given(redisTemplate.hasKey("blacklist:" + token)).willReturn(false);
        given(jwtProvider.extractUserId(token)).willReturn(userId);
        given(customUserDetailsService.loadUserByUsername(userId.toString())).willReturn(userDetails);
        given(userDetails.getAuthorities()).willReturn((List) authorities);

        // when
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isSameAs(userDetails);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void 토큰이_없으면_인증_없이_다음_필터로_진행한다() throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void Bearer_접두사가_없으면_토큰을_추출하지_않는다() throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "raw-token-without-bearer");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(jwtProvider, never()).validateToken(anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void 유효하지_않은_토큰이면_인증_없이_다음_필터로_진행한다() throws Exception {
        // given
        String token = "invalid-token";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        given(jwtProvider.validateToken(token)).willReturn(false);

        // when
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void 블랙리스트에_등록된_토큰이면_인증_없이_다음_필터로_진행한다() throws Exception {
        // given
        String token = "blacklisted-token";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        given(jwtProvider.validateToken(token)).willReturn(true);
        given(redisTemplate.hasKey("blacklist:" + token)).willReturn(true);

        // when
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(customUserDetailsService, never()).loadUserByUsername(anyString());
        verify(filterChain).doFilter(request, response);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        System.out.println("경과된 시간:" + (System.currentTimeMillis() - startTime) + "ms");
    }
}
