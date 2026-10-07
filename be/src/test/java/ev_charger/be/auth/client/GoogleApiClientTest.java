package ev_charger.be.auth.client;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import ev_charger.be.auth.dto.response.UserInfo;
import ev_charger.be.common.exception.InternalServerException;
import ev_charger.be.user.enums.Provider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class GoogleApiClientTest {

    private GoogleIdTokenVerifier verifier;
    private GoogleApiClient googleApiClient;
    private long startTime;

    @BeforeEach
    void setUp() {
        startTime = System.currentTimeMillis();
        verifier = mock(GoogleIdTokenVerifier.class);
        googleApiClient = new GoogleApiClient(verifier);
    }

    @AfterEach
    void tearDown(TestInfo testInfo) {
        System.out.println(testInfo.getDisplayName() + " 경과 시간: " + (System.currentTimeMillis() - startTime) + "ms");
    }

    // header.payload.signature 형식의 idToken 생성 (서명 검증은 verifier mock이 담당)
    private String idToken(String payloadJson) {
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        return encoder.encodeToString("{\"alg\":\"RS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8))
                + "." + encoder.encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8))
                + "." + encoder.encodeToString("signature".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void 검증된_idToken이면_sub와_email로_UserInfo_생성() throws Exception {
        // given
        given(verifier.verify(any(GoogleIdToken.class))).willReturn(true);

        // when
        UserInfo userInfo = googleApiClient.getUserInfo(idToken("""
                {"sub": "1234567890", "email": "test@gmail.com"}
                """));

        // then
        assertThat(userInfo.id()).isEqualTo("1234567890");
        assertThat(userInfo.email()).isEqualTo("test@gmail.com");
    }

    @Test
    void 검증_실패면_IllegalArgumentException_발생() throws Exception {
        // given
        // 서명 불일치, 만료, 다른 앱에 발급된 토큰(aud 불일치)이면 verify가 false 반환
        given(verifier.verify(any(GoogleIdToken.class))).willReturn(false);

        // when & then
        assertThatThrownBy(() -> googleApiClient.getUserInfo(idToken("""
                {"sub": "1234567890", "email": "test@gmail.com", "aud": "other-app"}
                """)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("유효하지 않은 소셜 토큰");
    }

    @Test
    void JWT_형식이_아니면_IllegalArgumentException_발생() throws Exception {
        // when & then
        // 예전 방식의 accessToken 등 JWT가 아닌 값
        assertThatThrownBy(() -> googleApiClient.getUserInfo("ya29.not-a-jwt"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("유효하지 않은 소셜 토큰");

        verify(verifier, never()).verify(any(GoogleIdToken.class));
    }

    @Test
    void 구글_공개키_조회_실패면_InternalServerException_발생() throws Exception {
        // given
        given(verifier.verify(any(GoogleIdToken.class))).willThrow(new IOException("network"));

        // when & then
        assertThatThrownBy(() -> googleApiClient.getUserInfo(idToken("""
                {"sub": "1234567890", "email": "test@gmail.com"}
                """)))
                .isInstanceOf(InternalServerException.class);
    }

    @Test
    void getProvider_는_GOOGLE_반환() {
        assertThat(googleApiClient.getProvider()).isEqualTo(Provider.GOOGLE);
    }
}