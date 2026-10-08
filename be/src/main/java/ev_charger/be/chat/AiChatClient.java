package ev_charger.be.chat;

import ev_charger.be.chat.dto.request.AiChatRequest;
import ev_charger.be.chat.dto.response.ChatResponse;
import ev_charger.be.common.exception.InternalServerException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Component
public class AiChatClient {

    static final String UNAVAILABLE_MESSAGE = "챗봇이 지금 응답하지 못하고 있어요. 잠시 후 다시 시도해 주세요.";

    private final RestClient aiRestClient;

    public AiChatClient(@Qualifier("aiRestClient") RestClient aiRestClient) {
        this.aiRestClient = aiRestClient;
    }

    /**
     * AI 서버에 질문 전달
     * @throws InternalServerException AI 서버에 연결하지 못하거나 오류로 응답한 경우 (500, 고정 문구)
     */
    public ChatResponse chat(AiChatRequest request) {
        try {
            ChatResponse response = aiRestClient.post()
                    .uri("/chat")
                    .body(request)
                    .retrieve()
                    .body(ChatResponse.class);
            if (response == null || response.reply() == null) {
                throw new InternalServerException(UNAVAILABLE_MESSAGE, null);
            }
            return response;
        } catch (RestClientResponseException e) {
            // 예외 메시지에 응답 본문(사용자 질문이 들어있을 수 있음)이 들어가서 원인 예외는 붙이지 않고 상태 코드만 남김
            // 401이면 AI 서버의 INTERNAL_API_KEY가 be의 internal.secret-key와 다른 것
            log.error("AI 서버가 오류 응답: status={}", e.getStatusCode().value());
            throw new InternalServerException(UNAVAILABLE_MESSAGE, null);
        } catch (ResourceAccessException e) {
            throw new InternalServerException(UNAVAILABLE_MESSAGE, e);
        }
    }
}
