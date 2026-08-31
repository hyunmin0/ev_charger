package ev_charger.be.auth.client;

import com.sun.net.httpserver.HttpServer;
import ev_charger.be.auth.dto.response.UserInfo;
import ev_charger.be.user.enums.Provider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KakaoApiClientTest {

    private HttpServer server;
    private KakaoApiClient kakaoApiClient;
    private String receivedAuthHeader;
    private long startTime;

    @BeforeEach
    void setUp() throws IOException {
        startTime = System.currentTimeMillis();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        kakaoApiClient = new KakaoApiClient();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
        System.out.println("경과된 시간:" + (System.currentTimeMillis() - startTime) + "ms");
    }

    // 카카오 서버 흉내: 요청 헤더를 기록하고 지정된 JSON을 200으로 응답
    private void stubResponse(String json) throws IOException {
        server.createContext("/", exchange -> {
            receivedAuthHeader = exchange.getRequestHeaders().getFirst("Authorization");
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        ReflectionTestUtils.setField(kakaoApiClient, "apiUrl",
                "http://localhost:" + server.getAddress().getPort() + "/");
    }

    @Test
    void 정상_응답이면_id와_email로_UserInfo_생성() throws IOException {
        stubResponse("""
                {"id": 12345, "kakao_account": {"email": "test@kakao.com"}}
                """);

        UserInfo userInfo = kakaoApiClient.getUserInfo("test-token");

        assertThat(userInfo.id()).isEqualTo("12345");
        assertThat(userInfo.email()).isEqualTo("test@kakao.com");
    }

    @Test
    void Authorization_헤더에_Bearer_토큰_포함해서_요청() throws IOException {
        stubResponse("""
                {"id": 12345, "kakao_account": {"email": "test@kakao.com"}}
                """);

        kakaoApiClient.getUserInfo("my-access-token");

        assertThat(receivedAuthHeader).isEqualTo("Bearer my-access-token");
    }

    @Test
    void getProvider_는_KAKAO_반환() {
        assertThat(kakaoApiClient.getProvider()).isEqualTo(Provider.KAKAO);
    }

    @Test
    void kakao_account가_없으면_email은_null() throws IOException {
        // 개인정보 동의 자체를 거부하면 kakao_account 키가 응답에 없음
        stubResponse("""
                {"id": 12345}
                """);

        UserInfo userInfo = kakaoApiClient.getUserInfo("test-token");

        assertThat(userInfo.id()).isEqualTo("12345");
        assertThat(userInfo.email()).isNull();
    }

    @Test
    void kakao_account는_있는데_email_키만_없으면_email은_null() throws IOException {
        // 다른 동의 항목은 있지만 이메일만 동의하지 않은 경우
        stubResponse("""
                {"id": 12345, "kakao_account": {}}
                """);

        UserInfo userInfo = kakaoApiClient.getUserInfo("test-token");

        assertThat(userInfo.email()).isNull();
    }

    @Test
    void 서버_에러_응답이면_예외_전파() throws IOException {
        server.createContext("/", exchange -> {
            exchange.sendResponseHeaders(500, -1);
            exchange.close();
        });
        server.start();
        ReflectionTestUtils.setField(kakaoApiClient, "apiUrl",
                "http://localhost:" + server.getAddress().getPort() + "/");

        assertThatThrownBy(() -> kakaoApiClient.getUserInfo("test-token"))
                .isInstanceOf(RestClientException.class);
    }
}
