package ev_charger.be.config;
// spring security 전체 설정 담당

import ev_charger.be.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;


@Configuration // configuration 등록
@EnableWebSecurity // spring security 활성화
@RequiredArgsConstructor // final 생성자 자동 생성
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, CorsConfigurationSource corsConfigurationSource) throws Exception {
        http
                // csrf 토큰 비활성화
                .csrf(csrf->csrf.disable())
                // 요청별 권한 설정
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/","/auth/**").permitAll() // 해당 요청이 오면 접근 허용
                        .anyRequest().authenticated() // 다른 요청의 경우 인증 필요 (없는 경우: 접근 제한 페이지를 보여줌)
                )
                // 세션 사용 안함  (jwt: stateless)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                // cors 설정
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                // jwt 필터 등록
                .addFilterBefore(jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // 허용할 프론트엔드 주소: 전체 허용
        // flutter는 실행환경이 다양하므로 origin이 계속 달라질 수 있음
        config.setAllowedOriginPatterns(List.of("*"));
        // 허용할 http 메서드
        // GET: 조회, POST: 생성, PUT: 수정, DELETE: 삭제, OPTIONS: 사전 요청(preflight)
        config.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS"));
        // 허용할 헤더: 전체 허용(Authorization 포함)
        config.setAllowedHeaders(List.of("*"));

        // 모든 경로에 적용
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // 모든 api 경로에 이 cors 정책 적용
        source.registerCorsConfiguration("/**", config);

        return source;
    }

}
