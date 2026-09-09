package ev_charger.be.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing // 자동으로 시간 측정
public class JpaAuditingConfig {
}
