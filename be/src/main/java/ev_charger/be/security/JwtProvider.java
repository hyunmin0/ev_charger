package ev_charger.be.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
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

    // 토큰 종류 구분용 claim (refresh token을 access token 대신 쓰지 못하게 막음)
    private static final String TOKEN_TYPE = "type";
    private static final String ACCESS = "access";
    private static final String REFRESH = "refresh";

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
                // 토큰 종류
                .claim(TOKEN_TYPE, ACCESS)
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
                // 토큰 종류
                .claim(TOKEN_TYPE, REFRESH)
                // 토큰 만료 시간
                .expiration(new Date(System.currentTimeMillis() + refreshExpiration))
                // 서명
                .signWith(Keys.hmacShaKeyFor(secret.getBytes()))
                // jwt 생성
                .compact();
    }

    /**
     * access token 유효성 검사 (인증 필터에서 사용)
     * @param token jwt
     * @return 유효한 access token이면 true, refresh token이면 false
     */
    public boolean validateAccessToken(String token) {
        return validateToken(token, ACCESS);
    }

    /**
     * refresh token 유효성 검사 (토큰 재발급에서 사용)
     * @param token jwt
     * @return 유효한 refresh token이면 true, access token이면 false
     */
    public boolean validateRefreshToken(String token) {
        return validateToken(token, REFRESH);
    }

    /**
     * jwt 유효성 검사
     * @param token jwt
     * @param expectedType 기대하는 토큰 종류(access/refresh)
     * @return 유효하고 종류가 일치하면 true
     */
    private boolean validateToken(String token, String expectedType) {
        try {
            //jwt 파싱 및 검증
            Claims claims = Jwts.parser()// jwt 읽을 parser 준비
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
                    .parseSignedClaims(token)
                    .getPayload();
            // 예외 없으면 정상 토큰 -> 종류까지 일치해야 통과
            return expectedType.equals(claims.get(TOKEN_TYPE, String.class));
        } catch (Exception e) {
            // 만료/위조/형식 오류 등
            return false;
        }
    }

    /**
     * access token의 남은 만료 시간(ms)
     * @param token jwt
     * @return 남은 시간, 이미 만료된 토큰이면 0
     * @throws IllegalArgumentException 위조/형식 오류 토큰 -> 400
     */
    public long getAccessExpiration(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getExpiration()
                    .getTime() - System.currentTimeMillis(); // 만료 시각 - 지금 시각
        } catch (ExpiredJwtException e) {
            // 이미 만료된 토큰은 더 쓸 수 없으니 남은 시간 0
            return 0;
        } catch (JwtException | IllegalArgumentException e) {
            throw new IllegalArgumentException("유효하지 않은 access token");
        }
    }


    /**
     * jwt에서 userId 추출
     * @param token jwt
     * @return 사용자 UUID
     */
    public UUID extractUserId(String token) {

        // String -> UUID 변환
        return UUID.fromString(Jwts.parser() // jwt 내부의 payload 가져오기
                // 검증 키
                .verifyWith(Keys.hmacShaKeyFor(secret.getBytes()))
                // parser 생성
                .build()
                // jwt 파싱
                .parseSignedClaims(token)
                // payload 접근
                .getPayload()
                // subject 값 추출
                .getSubject());
    }
}