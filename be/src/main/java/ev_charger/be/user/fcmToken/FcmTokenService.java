package ev_charger.be.user.fcmToken;

import ev_charger.be.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class FcmTokenService {

    private final FcmTokenRepository fcmTokenRepository;

    /**
     * fcm 토큰 등록(앱 실행·로그인·알림 켜기 시)
     * 앱이 실행할 때마다 다시 보내므로 이미 있으면 그대로 둠
     * @param user
     * @param token
     */
    public void register(User user, String token) {
        Optional<FcmToken> existing = fcmTokenRepository.findByToken(token);

        if (existing.isEmpty()) {
            fcmTokenRepository.save(new FcmToken(user, token));
            return;
        }

        // token은 unique라 다른 계정이 쓰던 토큰이면 새로 넣지 않고 주인만 바꿈
        FcmToken fcmToken = existing.get();
        if (!fcmToken.getUser().getUserId().equals(user.getUserId())) {
            fcmToken.changeUser(user);
        }
    }

    /**
     * fcm 토큰 제거(로그아웃·알림 끄기 시)
     * 이미 없으면 그대로 둠
     * @param user
     * @param token
     */
    public void delete(User user, String token) {
        fcmTokenRepository.deleteByUserAndToken(user, token);
    }
}
