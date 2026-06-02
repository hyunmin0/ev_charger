package ev_charger.be.common;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Base64;

@Component
@RequiredArgsConstructor
public class CursorUtils {
    private final ObjectMapper objectMapper;

    // 커서에 담길 데이터: 마지막 id와 거리
    public record CursorData(String lastId, double distance) {}

    /**
     * Base64로 인코딩된 커서를 CursorData로 디코딩
     * @param cursor
     * @return lastId, distance
     */
    @SneakyThrows // try-catch 대신
    public CursorData decode(String cursor) {
            String decoded = new String(Base64.getDecoder().decode(cursor));
            return objectMapper.readValue(decoded, CursorData.class);
    }

    /**
     * CursorData를 json으로 직렬화 후 Base64로 인코딩
     * (직렬화: 객체를 전송/저장 가능한 형태로 변환(문자열, 바이트 등))
     * @param lastId
     * @param distance
     * @return cursor
     */
    @SneakyThrows
    public String encode(String lastId, double distance) {
        String json = objectMapper.writeValueAsString(new CursorData(lastId, distance));
        return Base64.getEncoder().encodeToString(json.getBytes());
    }
}
