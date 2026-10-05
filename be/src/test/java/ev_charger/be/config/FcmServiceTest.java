package ev_charger.be.config;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import ev_charger.be.user.fcmToken.FcmTokenRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FcmServiceTest {

    @Mock
    private FcmTokenRepository fcmTokenRepository;
    @Mock
    private FirebaseMessaging firebaseMessaging;

    private FcmService fcmService;

    private long startTime;

    @BeforeEach
    void setUp() {
        startTime = System.currentTimeMillis();
        fcmService = new FcmService(fcmTokenRepository, firebaseMessaging);
    }

    @Test
    void 알림_발송_성공() throws Exception {
        // given
        given(firebaseMessaging.send(any(Message.class))).willReturn("projects/test/messages/1");

        // when
        fcmService.send("token-1", "제목", "내용", 1L);

        // then
        verify(firebaseMessaging).send(any(Message.class));
        verify(fcmTokenRepository, never()).deleteByToken(any());
    }

    @Test
    void 만료된_토큰이면_DB에서_삭제하고_예외를_던지지_않는다() throws Exception {
        // given
        FirebaseMessagingException exception = mock(FirebaseMessagingException.class);
        given(exception.getMessagingErrorCode()).willReturn(MessagingErrorCode.UNREGISTERED);
        given(firebaseMessaging.send(any(Message.class))).willThrow(exception);

        // when
        fcmService.send("token-1", "제목", "내용", 1L);

        // then
        verify(fcmTokenRepository).deleteByToken("token-1");
    }

    @Test
    void 만료_외_에러면_예외를_던진다() throws Exception {
        // given
        FirebaseMessagingException exception = mock(FirebaseMessagingException.class);
        given(exception.getMessagingErrorCode()).willReturn(MessagingErrorCode.INTERNAL);
        given(firebaseMessaging.send(any(Message.class))).willThrow(exception);

        // when & then
        assertThatThrownBy(() -> fcmService.send("token-1", "제목", "내용", 1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("FCM 발송 실패")
                .hasCause(exception);

        verify(fcmTokenRepository, never()).deleteByToken(any());
    }

    @Test
    void 재시도_최종_실패_후_recover가_예외없이_처리된다() {
        // given
        FirebaseMessagingException cause = mock(FirebaseMessagingException.class);
        IllegalStateException e = new IllegalStateException("FCM 발송 실패", cause);

        // when & then
        assertThatCode(() -> fcmService.recover(e, "token-1", "제목", "내용", 1L))
                .doesNotThrowAnyException();
    }

    @AfterEach
    void tearDown(TestInfo testInfo) {
        System.out.println(testInfo.getDisplayName() + " 경과 시간: " + (System.currentTimeMillis() - startTime) + "ms");
    }
}
