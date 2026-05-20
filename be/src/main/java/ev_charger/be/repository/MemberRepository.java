package ev_charger.be.repository;

import ev_charger.be.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MemberRepository extends JpaRepository<Member, UUID> {

    Optional<Member> findByProviderAndProviderId(String provider, String providerId);
    Optional<Member> findByRefreshToken(String refreshToken);

}