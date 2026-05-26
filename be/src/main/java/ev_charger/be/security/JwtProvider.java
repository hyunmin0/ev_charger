package ev_charger.be.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;

@Component
public class JwtProvider {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-expiration}")
    private long accessExpiration;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshExpiration;

    private SecretKey key;


    /**
     * 서버 시작 시 한 번만 실행
     * secret 문자열을 jwt용 Key 객체로 변환
     */
    @PostConstruct
    public void init() {
        key = Keys.hmacShaKeyFor(secret.getBytes());
    }

    /**
     * Access Token 생성
     * @param userId 사용자 UUID
     * @return accessToken
     */
    public String generateAccessToken(UUID userId) {
        return Jwts.builder()
                // 사용자 식별값 저장
                .subject(userId.toString())
                // 토큰 만료 시간
                .expiration(new Date(System.currentTimeMillis() + accessExpiration))
                // 서명
                .signWith(Keys.hmacShaKeyFor(secret.getBytes()))
                // jwt 생성
                .compact();
    }


    public String generateRefreshToken(UUID userId) {
        return Jwts.builder()
                // 사용자 식별값 저장
                .subject(userId.toString())
                // 토큰 만료 시간
                .expiration(new Date(System.currentTimeMillis() + refreshExpiration))
                // 서명
                .signWith(Keys.hmacShaKeyFor(secret.getBytes()))
                // jwt 생성
                .compact();
    }

    /**
     * jwt 유효성 검사
     * @param token jwt
     * @return 유효하면 true
     */
    public boolean validateToken(String token) {
        try {
            //jwt 파싱 및 검증
            Jwts.parser()// jwt 읽을 parser 준비
                    // 서명 검증용 key
                    // jwt 생성 시 마지막 signature 부분이 생성됨
                    // 그게 우리 서버가 만든 jwt가 맞는지 확인
                    .verifyWith(key)
                    // parser 완성
                    .build()
                    // 실제 jwt 검증
                    // jwt를 읽고 검사하고 내부 데이터(payload)를 해석
                    /**
                     * 1. jwt 형식 검사
                     * 2. signature 검증
                     * 3. exp 만료 검사
                     * 4. payload 읽기
                     * 성공: jwt 내부 claim 반환
                     * 실패: 예외 발생
                     */
                    .parseSignedClaims(token);
            // 예외 없으면 정상 토큰
            return true;
        } catch (Exception e) {
            // 만료/위조/형식 오류 등
            return false;
        }
    }

    /**
     * jwt에서 userId 추출
     * @param token jwt
     * @return 사용자 UUID
     */
    public UUID extractUserId(String token) {
        // jwt 내부의 payload 가져오기
        String subject = Jwts.parser()
                // 검증 키
                .verifyWith(Keys.hmacShaKeyFor(secret.getBytes()))
                // parser 생성
                .build()
                // jwt 파싱
                .parseSignedClaims(token)
                // payload 접근
                .getPayload()
                // subject 값 추출
                .getSubject();

        // String -> UUID 변환
        return UUID.fromString(subject);
    }
}