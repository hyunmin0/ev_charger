package ev_charger.be.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

// AI 서버(FastAPI) 호출용 클라이언트. X-Internal-Key는 be의 internal.secret-key를 그대로 쓰고, AI 서버의 INTERNAL_API_KEY와 같아야 함
@Configuration
public class AiClientConfig {

    @Bean
    public RestClient aiRestClient(
            @Value("${ai.base-url:http://localhost:8000}") String baseUrl,
            @Value("${internal.secret-key}") String internalKey,
            // LLM이 tool을 여러 번 부르면 수십 초 걸릴 수 있음
            @Value("${ai.read-timeout-seconds:60}") long readTimeoutSeconds) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(readTimeoutSeconds));

        return RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("X-Internal-Key", internalKey)
                .requestFactory(requestFactory)
                .build();
    }
}
