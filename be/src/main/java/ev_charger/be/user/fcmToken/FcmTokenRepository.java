package ev_charger.be.user.fcmToken;

import ev_charger.be.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface FcmTokenRepository extends JpaRepository<FcmToken, Long> {
    List<FcmToken> findByUserIn(Collection<User> users);

    void deleteByToken(String token);
    void deleteByUserAndToken(User user, String token);

    boolean existsByUserAndToken(User user, String token);
}
