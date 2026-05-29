package ev_charger.be.user;

import ev_charger.be.user.enums.Provider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByProviderAndProviderId(Provider provider, String providerId);
    Optional<User> findByRefreshToken(String refreshToken);
    Boolean existsByProviderAndProviderId(Provider provider, String providerId);

}