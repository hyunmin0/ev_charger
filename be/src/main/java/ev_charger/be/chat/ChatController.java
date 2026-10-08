package ev_charger.be.chat;

import ev_charger.be.chat.dto.request.ChatRequest;
import ev_charger.be.chat.dto.response.ChatResponse;
import ev_charger.be.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    // 챗봇 질문 (로그인 필요)
    // input : ChatRequest { carId, message, history[{role, content}], lat, lng }
    // output: ChatResponse { reply, stations[{statId, statNm, addr, parkingFree, distance_km}] }
    @PostMapping
    public ResponseEntity<ChatResponse> chat(
            @AuthenticationPrincipal CustomUserDetails userDetails, // 로그인한 유저
            @Valid @RequestBody ChatRequest request) {
        return ResponseEntity.ok(chatService.chat(userDetails.getUser(), request));
    }
}
