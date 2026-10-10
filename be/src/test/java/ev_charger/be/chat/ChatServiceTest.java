package ev_charger.be.chat;

import ev_charger.be.chat.dto.request.AiChatRequest;
import ev_charger.be.chat.dto.request.ChatMessage;
import ev_charger.be.chat.dto.request.ChatRequest;
import ev_charger.be.chat.dto.response.ChatResponse;
import ev_charger.be.chat.dto.response.ChatUsageResponse;
import ev_charger.be.common.exception.InternalServerException;
import ev_charger.be.common.exception.TooManyRequestsException;
import ev_charger.be.user.User;
import ev_charger.be.user.enums.Provider;
import ev_charger.be.user.userCar.UserCarRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    private static final long MY_CAR_ID = 5L;
    private static final long OTHER_CAR_ID = 99L;
    private static final String MESSAGE = "근처 급속 충전소 알려줘";
    private static final double LAT = 37.5665;
    private static final double LNG = 126.9780;

    @Mock
    private UserCarRepository userCarRepository;
    @Mock
    private AiChatClient aiChatClient;
    @Mock
    private ChatUsageLimiter chatUsageLimiter;

    private ChatService chatService;
    private User user;
    private ChatResponse aiResponse;
    private ChatUsageLimiter.Reservation reservation;

    @BeforeEach
    void setUp() {
        chatService = new ChatService(userCarRepository, aiChatClient, chatUsageLimiter);

        user = User.builder()
                .nickname("테스터")
                .email("test@example.com")
                .provider(Provider.GOOGLE)
                .providerId("google-1234")
                .build();
        ReflectionTestUtils.setField(user, "userId", UUID.randomUUID());

        aiResponse = new ChatResponse("가까운 충전소는 여기예요.", List.of());
        reservation = new ChatUsageLimiter.Reservation("chat:usage:test", 7);
        lenient().when(chatUsageLimiter.acquire(any())).thenReturn(reservation);
    }

    private AiChatRequest sentToAi() {
        ArgumentCaptor<AiChatRequest> captor = ArgumentCaptor.forClass(AiChatRequest.class);
        verify(aiChatClient).chat(captor.capture());
        return captor.getValue();
    }

    @Test
    void 차량_미선택이면_차량_확인_없이_AI에_user_id만_실어_보낸다() {
        given(aiChatClient.chat(any())).willReturn(aiResponse);

        ChatResponse result = chatService.chat(user, new ChatRequest(null, MESSAGE, List.of(), LAT, LNG));

        assertThat(result.reply()).isEqualTo(aiResponse.reply());
        assertThat(result.remainingToday()).isEqualTo(7);
        verifyNoInteractions(userCarRepository);
        AiChatRequest sent = sentToAi();
        assertThat(sent.userId()).isEqualTo(user.getUserId());
        assertThat(sent.carId()).isNull();
        assertThat(sent.message()).isEqualTo(MESSAGE);
        assertThat(sent.lat()).isEqualTo(LAT);
        assertThat(sent.lng()).isEqualTo(LNG);
    }

    @Test
    void 내_차량이면_car_id를_AI에_넘긴다() {
        given(userCarRepository.existsByUserAndCar_CarId(user, MY_CAR_ID)).willReturn(true);
        given(aiChatClient.chat(any())).willReturn(aiResponse);

        chatService.chat(user, new ChatRequest(MY_CAR_ID, MESSAGE, List.of(), null, null));

        AiChatRequest sent = sentToAi();
        assertThat(sent.carId()).isEqualTo(MY_CAR_ID);
        assertThat(sent.lat()).isNull();
        assertThat(sent.lng()).isNull();
    }

    @Test
    void 내_차량이_아니면_AI를_호출하지_않고_예외() {
        given(userCarRepository.existsByUserAndCar_CarId(user, OTHER_CAR_ID)).willReturn(false);

        assertThatThrownBy(() -> chatService.chat(user, new ChatRequest(OTHER_CAR_ID, MESSAGE, List.of(), null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("등록되지 않은 차량입니다.");

        verify(aiChatClient, never()).chat(any());
        verify(chatUsageLimiter, never()).acquire(any());
    }

    @Test
    void 대화_이력이_상한을_넘으면_최근_것만_보낸다() {
        List<ChatMessage> history = new ArrayList<>();
        int total = ChatService.MAX_HISTORY_MESSAGES + 5;
        for (int i = 0; i < total; i++) {
            history.add(new ChatMessage(i % 2 == 0 ? "user" : "assistant", "메시지" + i));
        }
        given(aiChatClient.chat(any())).willReturn(aiResponse);

        chatService.chat(user, new ChatRequest(null, MESSAGE, history, null, null));

        List<ChatMessage> sent = sentToAi().history();
        assertThat(sent).hasSize(ChatService.MAX_HISTORY_MESSAGES);
        assertThat(sent.get(0).content()).isEqualTo("메시지5");
        assertThat(sent.get(sent.size() - 1).content()).isEqualTo("메시지" + (total - 1));
    }

    @Test
    void 대화_이력이_상한_이하면_그대로_보낸다() {
        List<ChatMessage> history = List.of(
                new ChatMessage("user", "안녕"),
                new ChatMessage("assistant", "안녕하세요"));
        given(aiChatClient.chat(any())).willReturn(aiResponse);

        chatService.chat(user, new ChatRequest(null, MESSAGE, history, null, null));

        assertThat(sentToAi().history()).isEqualTo(history);
    }

    @Test
    void 오늘_질문_수를_다_썼으면_AI를_호출하지_않고_예외() {
        given(chatUsageLimiter.acquire(user.getUserId()))
                .willThrow(new TooManyRequestsException("오늘 챗봇 질문 10회를 모두 사용했어요. 내일 다시 이용해 주세요."));

        assertThatThrownBy(() -> chatService.chat(user, new ChatRequest(null, MESSAGE, List.of(), null, null)))
                .isInstanceOf(TooManyRequestsException.class);

        verify(aiChatClient, never()).chat(any());
        verify(chatUsageLimiter, never()).release(any());
    }

    @Test
    void AI_응답을_받지_못하면_차감을_되돌리고_예외를_그대로_던진다() {
        InternalServerException failure = new InternalServerException(AiChatClient.UNAVAILABLE_MESSAGE, null);
        given(aiChatClient.chat(any())).willThrow(failure);

        assertThatThrownBy(() -> chatService.chat(user, new ChatRequest(null, MESSAGE, List.of(), null, null)))
                .isSameAs(failure);

        verify(chatUsageLimiter).release(reservation);
    }

    @Test
    void 답변을_받으면_차감을_되돌리지_않는다() {
        given(aiChatClient.chat(any())).willReturn(aiResponse);

        chatService.chat(user, new ChatRequest(null, MESSAGE, List.of(), null, null));

        verify(chatUsageLimiter).acquire(user.getUserId());
        verify(chatUsageLimiter, never()).release(any());
    }

    @Test
    void 사용량은_상한_사용_남은_횟수를_내려준다() {
        given(chatUsageLimiter.used(user.getUserId())).willReturn(3);

        ChatUsageResponse usage = chatService.usage(user);

        assertThat(usage).isEqualTo(new ChatUsageResponse(ChatUsageLimiter.DAILY_LIMIT, 3, ChatUsageLimiter.DAILY_LIMIT - 3));
    }
}
