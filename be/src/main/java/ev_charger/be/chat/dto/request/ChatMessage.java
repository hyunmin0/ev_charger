package ev_charger.be.chat.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// 대화 이력 한 건 (앱 -> be, be -> AI 공통)
public record ChatMessage(
        @Pattern(regexp = "user|assistant") String role,
        @NotBlank @Size(max = 4000) String content
) {}
