package ev_charger.be.user.fcmToken;

import ev_charger.be.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface FcmTokenRepository extends JpaRepository<FcmToken, Long> {

    List<FcmToken> findByUser(User user);

    void deleteByToken(String token);
    void deleteByUserAndToken(User user, String token);

    boolean existsByUserAndToken(User user, String token);
}
