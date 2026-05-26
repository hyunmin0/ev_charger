package ev_charger.be.security;
// 요청마다 jwt 검사하는 필터

// 모든 요청마다 한 번만 실행되는 필터
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final CustomUserDetailsService customUserDetailsService;
    private final RedisTemplate<String, String> redisTemplate;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                 HttpServletResponse response,
                                 FilterChain filterChain) throws ServletException, IOException {
        // 토큰 추출
        String token = resolveToken(request);

        // 토큰 검증
        if (token != null && jwtProvider.validateToken(token)) {
            // blacklist에 있는지 확인 -> 있으면 인증 없이 return
            if (Boolean.TRUE.equals(redisTemplate.hasKey("blacklist:" + token))) {
                filterChain.doFilter(request,response);
                return;
            }
            // 유저 조회
            String userId = jwtProvider.extractUserId(token).toString();
            UserDetails userDetails = customUserDetailsService.loadUserByUsername(userId);

            // 파라미터 3개: 인증 완료 / 파라미터 2개: 인증 전 상태
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    userDetails,    // 유저 정보(UserDetails 타입만 받음)
                    null,               // 비밀번호(jwt는 필요없으므로 null)
                    userDetails.getAuthorities() // 권한 목록
            ); // 토큰 검증 완료 -> 인증 완료 상태로 처리

            // security context 저장
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        // 다음 필터로 넘김
        filterChain.doFilter(request, response);
    }

    // 토큰 추출
    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }

}
