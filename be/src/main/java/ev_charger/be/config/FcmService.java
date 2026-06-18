package ev_charger.be.config;
// 알림 처리

import com.google.firebase.messaging.*;
import ev_charger.be.user.fcmToken.FcmTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

@Service
@Slf4j // lombok: 자동으로 log 필드 생성
@RequiredArgsConstructor
public class FcmService {

    private final FcmTokenRepository fcmTokenRepository;
    private final FirebaseMessaging firebaseMessaging;

    /**
     * fcm 알림 처리
     * @param token
     * @param title
     * @param body
     */
    // Retryable: 재시도 어노테이션(throw e를 감지하여 재시도)
    // retryFor 예외 발생 시 maxAttempts만큼 재시도
    // 간격(1초, 2배): 1초 -> 2초 -> 4초
    @Retryable(
            retryFor = {RuntimeException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2
            )
    )
    public void send(String token, String title, String body) {
        try {
            // FireBase Admin SDK로 푸시 발송
            // Message: fcm의 객체
            Message message = Message.builder()
                    .setToken(token)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .build();

            firebaseMessaging.send(message);

        } catch (FirebaseMessagingException e) {
            // 토큰 만료/삭제일 땐 db에서 삭제
            if (e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED) {
                log.warn("FCM 토큰 무효 [{}]: {}", token, e.getMessage());
                fcmTokenRepository.deleteByToken(token);
                return;
            }

            log.warn("FCM 발송 실패, 재시도 예정 [{}]: {}", e.getMessagingErrorCode(), e.getMessage());

            throw new IllegalStateException("FCM 발송 실패", e);
            }
        }

    /**
     * send 재시도 3회 후 호출되는 log메서드
     * @param e
     * @param token
     */
    @Recover
    public void recover(IllegalStateException e,
                        String token,
                        // Spring Retry는 파라미터가 일치해야 @Recover를 찾기에 title, body 추가
                        String title, String body) {

        if (e.getCause() instanceof FirebaseMessagingException fme) {
            log.error("FCM 발송 최종 실패 token={} [{}]: {}", token, fme.getMessagingErrorCode(), fme.getMessage());
        }
    }
}
