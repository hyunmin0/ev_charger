package ev_charger.be.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {
    // CursorData 객체를 JSON 문자열로 변환하거나, JSON 문자열을 CursorData 객체로 변환하기 위해 필요
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
