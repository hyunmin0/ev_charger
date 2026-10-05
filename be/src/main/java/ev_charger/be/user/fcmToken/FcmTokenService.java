package ev_charger.be.user.fcmToken;

import ev_charger.be.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class FcmTokenService {

    private final FcmTokenRepository fcmTokenRepository;

    /**
     * fcm 토큰 등록(로그인 시)
     * @param user
     * @param token
     */
    public void register(User user, String token) {
        if (fcmTokenRepository.existsByUserAndToken(user, token)) {
            throw new IllegalArgumentException("중복된 Fcm 토큰 값입니다.");
        }

        fcmTokenRepository.save(new  FcmToken(user, token));
    }

    /**
     * fmc 토큰 제거(로그아웃 시)
     * @param user
     * @param token
     */
    public void delete(User user, String token) {
        if (!fcmTokenRepository.existsByUserAndToken(user, token)) {
            throw new IllegalArgumentException("Fcm 토큰이 존재하지 않습니다.");
        }

        fcmTokenRepository.deleteByUserAndToken(user, token);
    }
}
