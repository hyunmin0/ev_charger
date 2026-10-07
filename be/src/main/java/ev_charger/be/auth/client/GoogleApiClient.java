package ev_charger.be.auth.client;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import ev_charger.be.auth.dto.response.UserInfo;
import ev_charger.be.common.exception.InternalServerException;
import ev_charger.be.user.enums.Provider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

@Component
public class GoogleApiClient implements OAuthApiClient{

    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    // 구글 공개키로 idToken 서명/만료/발급자/aud 검증 (공개키는 내부에서 캐시)
    private final GoogleIdTokenVerifier verifier;

    @Autowired
    public GoogleApiClient(
            @Value("${spring.security.oauth2.client.registration.google.client-id}") String webClientId) {
        this(new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), JSON_FACTORY)
                // 우리 앱(웹 클라이언트 ID)에 발급된 토큰만 통과 -> 다른 앱의 토큰으로 로그인 차단
                .setAudience(List.of(webClientId))
                .build());
    }

    // 테스트에서 검증기 주입용
    GoogleApiClient(GoogleIdTokenVerifier verifier) {
        this.verifier = verifier;
    }

    @Override
    public UserInfo getUserInfo(String idToken) {

        // JWT 형식이 아니면 위조/잘못된 토큰 -> 400으로 응답
        GoogleIdToken token;
        try {
            token = GoogleIdToken.parse(JSON_FACTORY, idToken);
        } catch (IllegalArgumentException | IOException e) {
            throw new IllegalArgumentException("유효하지 않은 소셜 토큰");
        }

        boolean valid;
        try {
            valid = verifier.verify(token);
        } catch (GeneralSecurityException | IOException e) {
            // 구글 공개키를 못 가져오는 등 서버 쪽 문제 -> 500
            throw new InternalServerException("구글 토큰 검증 중 오류가 발생했습니다.", e);
        }

        // 서명 불일치/만료/aud 불일치 -> 400으로 응답
        if (!valid) {
            throw new IllegalArgumentException("유효하지 않은 소셜 토큰");
        }

        GoogleIdToken.Payload payload = token.getPayload();
        return new UserInfo(payload.getSubject(), payload.getEmail());
    }

    @Override
    public Provider getProvider() {
        return Provider.GOOGLE;
    }
}
