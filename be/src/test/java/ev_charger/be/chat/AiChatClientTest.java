package ev_charger.be.chat;

import ev_charger.be.chat.dto.request.AiChatRequest;
import ev_charger.be.chat.dto.request.ChatMessage;
import ev_charger.be.chat.dto.response.ChatResponse;
import ev_charger.be.common.exception.InternalServerException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AiChatClientTest {

    private static final String BASE_URL = "http://ai.test";
    private static final String INTERNAL_KEY = "test-key";
    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private MockRestServiceServer server;
    private AiChatClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl(BASE_URL)
                .defaultHeader("X-Internal-Key", INTERNAL_KEY);
        server = MockRestServiceServer.bindTo(builder).build();
        client = new AiChatClient(builder.build());
    }

    private AiChatRequest request() {
        return new AiChatRequest(USER_ID, 5L, "근처 충전소", List.of(new ChatMessage("user", "안녕")), 37.5, 127.0);
    }

    @Test
    void AI_서버에_snake_case_본문과_내부_키_헤더로_요청하고_응답을_변환한다() {
        server.expect(requestTo(BASE_URL + "/chat"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(header("X-Internal-Key", INTERNAL_KEY))
                .andExpect(content().json("""
                        {"user_id":"11111111-1111-1111-1111-111111111111","car_id":5,"message":"근처 충전소",
                         "history":[{"role":"user","content":"안녕"}],"lat":37.5,"lng":127.0}"""))
                .andRespond(withSuccess("""
                        {"reply":"추천해요","stations":[
                          {"statId":"SE000014","statNm":"서울시 본관청사","addr":"서울 중구","parkingFree":"Y","distance_km":0.3},
                          {"statId":"X","statNm":"위치 모름","addr":"어딘가","parkingFree":"N","distance_km":null}]}""",
                        MediaType.APPLICATION_JSON));

        ChatResponse response = client.chat(request());

        assertThat(response.reply()).isEqualTo("추천해요");
        assertThat(response.stations()).hasSize(2);
        assertThat(response.stations().get(0).statId()).isEqualTo("SE000014");
        assertThat(response.stations().get(0).distanceKm()).isEqualTo(0.3);
        assertThat(response.stations().get(1).distanceKm()).isNull();
        server.verify();
    }

    @Test
    void stations가_없는_응답은_빈_리스트() {
        server.expect(requestTo(BASE_URL + "/chat"))
                .andRespond(withSuccess("{\"reply\":\"안녕하세요\"}", MediaType.APPLICATION_JSON));

        ChatResponse response = client.chat(request());

        assertThat(response.reply()).isEqualTo("안녕하세요");
        assertThat(response.stations()).isEmpty();
    }

    @Test
    void AI_서버가_5xx면_500과_고정_문구() {
        server.expect(requestTo(BASE_URL + "/chat")).andRespond(withServerError());

        assertThatThrownBy(() -> client.chat(request()))
                .isInstanceOf(InternalServerException.class)
                .hasMessage(AiChatClient.UNAVAILABLE_MESSAGE);
    }

    @Test
    void 내부_키가_틀려_401이어도_500과_고정_문구() {
        server.expect(requestTo(BASE_URL + "/chat")).andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> client.chat(request()))
                .isInstanceOf(InternalServerException.class)
                .hasMessage(AiChatClient.UNAVAILABLE_MESSAGE);
    }

    @Test
    void AI_서버_연결_실패_또는_시간_초과면_500과_고정_문구() {
        server.expect(requestTo(BASE_URL + "/chat")).andRespond(withException(new IOException("Connection refused")));

        assertThatThrownBy(() -> client.chat(request()))
                .isInstanceOf(InternalServerException.class)
                .hasMessage(AiChatClient.UNAVAILABLE_MESSAGE);
    }

    @Test
    void 응답에_reply가_없으면_500과_고정_문구() {
        server.expect(requestTo(BASE_URL + "/chat"))
                .andRespond(withSuccess("{\"stations\":[]}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.chat(request()))
                .isInstanceOf(InternalServerException.class)
                .hasMessage(AiChatClient.UNAVAILABLE_MESSAGE);
    }
}
