package ev_charger.be.common;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

import java.util.Base64;

@Component
@RequiredArgsConstructor
public class CursorUtils {
    /**
     * Base64로 인코딩된 커서를 CursorData로 디코딩
     * @param cursor
     * @return distance
     */
    @SneakyThrows // try-catch 대신
    public Double decode(String cursor) {
            return Double.parseDouble(new String(Base64.getDecoder().decode(cursor)));
    }

    /**
     * CursorData를 json으로 직렬화 후 Base64로 인코딩
     * (직렬화: 객체를 전송/저장 가능한 형태로 변환(문자열, 바이트 등))
     * @param
     * @param distance
     * @return cursor
     */
    @SneakyThrows
    public String encode(double distance) {
        return Base64.getEncoder().encodeToString(String.valueOf(distance).getBytes());
    }
}
