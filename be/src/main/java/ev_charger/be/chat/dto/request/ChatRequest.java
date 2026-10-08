package ev_charger.be.chat.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

// 앱 -> be 챗봇 요청. user_id는 받지 않고 JWT의 로그인 유저로 채움
public record ChatRequest(
        Long carId,         // 선택한 차량(user_car의 car_id). 미선택이면 null
        @NotBlank @Size(max = 1000) String message,
        @Size(max = 200) List<@Valid ChatMessage> history,
        @DecimalMin("-90.0") @DecimalMax("90.0") Double lat,     // 현재 GPS. 위치 권한이 없으면 null
        @DecimalMin("-180.0") @DecimalMax("180.0") Double lng
) {
    public ChatRequest {
        if (history == null) history = List.of();
    }
}
