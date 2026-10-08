package ev_charger.be.chat;

import ev_charger.be.chat.dto.request.AiChatRequest;
import ev_charger.be.chat.dto.request.ChatMessage;
import ev_charger.be.chat.dto.request.ChatRequest;
import ev_charger.be.chat.dto.response.ChatResponse;
import ev_charger.be.user.User;
import ev_charger.be.user.userCar.UserCarRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

// @Transactional을 쓰지 않는 이유: AI 호출이 수십 초 걸릴 수 있어서 그동안 DB 커넥션을 잡고 있지 않기 위해
@Service
@RequiredArgsConstructor
public class ChatService {

    // AI에 보내는 대화 이력은 최근 N건만 (앱이 전체를 보내도 토큰 비용이 늘지 않게)
    static final int MAX_HISTORY_MESSAGES = 20;

    private final UserCarRepository userCarRepository;
    private final AiChatClient aiChatClient;

    /**
     * 챗봇 질문을 AI 서버에 전달
     * user_id는 로그인한 유저에서 가져오고, car_id는 그 유저의 차량인지 확인한 뒤에만 넘김
     * @throws IllegalArgumentException 내 차량이 아닌 carId
     */
    public ChatResponse chat(User user, ChatRequest request) {
        Long carId = request.carId();
        if (carId != null && !userCarRepository.existsByUserAndCar_CarId(user, carId)) {
            throw new IllegalArgumentException("등록되지 않은 차량입니다.");
        }

        List<ChatMessage> history = request.history();
        if (history.size() > MAX_HISTORY_MESSAGES) {
            history = history.subList(history.size() - MAX_HISTORY_MESSAGES, history.size());
        }

        return aiChatClient.chat(new AiChatRequest(
                user.getUserId(), carId, request.message(), history, request.lat(), request.lng()));
    }
}
