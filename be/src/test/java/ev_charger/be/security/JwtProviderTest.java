package ev_charger.be.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class JwtProviderTest {

    private JwtProvider jwtProvider;
    private long startTime;

    @BeforeEach
    void setUp() {
        startTime = System.currentTimeMillis();
        jwtProvider = new JwtProvider();
        // yml 파일 대신 주입
        ReflectionTestUtils.setField(jwtProvider, "secret", "test-secret-key-must-be-long-enough-for-hmac-sha256");
        ReflectionTestUtils.setField(jwtProvider, "accessExpiration", 1000L * 60 * 30);
        ReflectionTestUtils.setField(jwtProvider, "refreshExpiration", 1000L * 60 * 60 * 24 * 7);

        jwtProvider.init(); // secret key 등록
    }

    @Test
    void 액세스_토큰을_생성할_수_있다() {
        // given
        UUID uuid = UUID.randomUUID();

        // when
        String token = jwtProvider.generateAccessToken(uuid);

        // then
        assertThat(token).isNotBlank();
    }

    @Test
    void 리프레시_토큰을_생성할_수_있다() {
        // given
        UUID uuid = UUID.randomUUID();

        // when
        String token = jwtProvider.generateRefreshToken(uuid);

        // then
        assertThat(token).isNotBlank();
    }

    @Test
    void 생성한_토큰은_유효성_검사를_통과한다() {
        // given
        UUID uuid = UUID.randomUUID();
        String token = jwtProvider.generateAccessToken(uuid);

        // when
        boolean isValid = jwtProvider.validateToken(token);

        // then
        assertThat(isValid).isTrue();
    }

    @Test
    void 토큰에서_userId를_추출할_수_있다() {
        // given
        UUID uuid = UUID.randomUUID();
        String token = jwtProvider.generateAccessToken(uuid);

        // when
        UUID userId = jwtProvider.extractUserId(token);

        // then
        assertThat(userId).isEqualTo(uuid);
    }

    @Test
    void 액세스_토큰의_남은_만료_시간을_확인할_수_있다() {
        // given
        UUID uuid = UUID.randomUUID();
        String token = jwtProvider.generateAccessToken(uuid);

        // when
        long expiration = jwtProvider.getAccessExpiration(token);

        // then
        assertThat(expiration)
                .isPositive()
                .isLessThanOrEqualTo(1000L * 60 * 30)
                .isGreaterThan(1000L * 60 * 30 - 5000L); // 실행 시간 오차 허용
    }

    @Test
    void 만료된_토큰은_유효성_검사에_실패한다() {
        // given
        ReflectionTestUtils.setField(jwtProvider, "accessExpiration", -1000L);
        UUID uuid = UUID.randomUUID();
        String token = jwtProvider.generateAccessToken(uuid);

        // when
        boolean isValid = jwtProvider.validateToken(token);

        // then
        assertThat(isValid).isFalse();
    }

    @Test
    void 위조된_토큰은_유효성_검사에_실패한다() {
        // given
        UUID uuid = UUID.randomUUID();
        String token = jwtProvider.generateAccessToken(uuid);
        String forgedToken = token.substring(0, token.length() - 1) + "x"; // 마지막 글자 변조

        // when
        boolean isValid = jwtProvider.validateToken(forgedToken);

        // then
        assertThat(isValid).isFalse();
    }

    @AfterEach
    void tearDown() {
        System.out.println("경과된 시간:" + (System.currentTimeMillis() - startTime) + "ms");
    }
}
