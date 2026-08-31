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

class GoogleApiClientTest {

    private HttpServer server;
    private GoogleApiClient googleApiClient;
    private String receivedAuthHeader;
    private long startTime;

    @BeforeEach
    void setUp() throws IOException {
        startTime = System.currentTimeMillis();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        googleApiClient = new GoogleApiClient();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
        System.out.println("경과된 시간:" + (System.currentTimeMillis() - startTime) + "ms");
    }

    // 구글 서버 흉내: 요청 헤더 기록하고 지정된 JSON을 200으로 응답
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
        ReflectionTestUtils.setField(googleApiClient, "apiUrl",
                "http://localhost:" + server.getAddress().getPort() + "/");
    }

    @Test
    void 정상_응답이면_sub와_email로_UserInfo_생성() throws IOException {
        stubResponse("""
                {"sub": "1234567890", "email": "test@gmail.com"}
                """);

        UserInfo userInfo = googleApiClient.getUserInfo("test-token");

        assertThat(userInfo.id()).isEqualTo("1234567890");
        assertThat(userInfo.email()).isEqualTo("test@gmail.com");
    }

    @Test
    void Authorization_헤더에_Bearer_토큰_포함해서_요청() throws IOException {
        stubResponse("""
                {"sub": "1234567890", "email": "test@gmail.com"}
                """);

        googleApiClient.getUserInfo("my-access-token");

        assertThat(receivedAuthHeader).isEqualTo("Bearer my-access-token");
    }

    @Test
    void getProvider_는_GOOGLE_반환() {
        assertThat(googleApiClient.getProvider()).isEqualTo(Provider.GOOGLE);
    }

    @Test
    void 서버_에러_응답이면_예외_전파() throws IOException {
        server.createContext("/", exchange -> {
            exchange.sendResponseHeaders(500, -1);
            exchange.close();
        });
        server.start();
        ReflectionTestUtils.setField(googleApiClient, "apiUrl",
                "http://localhost:" + server.getAddress().getPort() + "/");

        assertThatThrownBy(() -> googleApiClient.getUserInfo("test-token"))
                .isInstanceOf(RestClientException.class);
    }
}