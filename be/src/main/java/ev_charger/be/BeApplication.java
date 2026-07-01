package ev_charger.be;

import jakarta.persistence.EntityListeners;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableRetry // 재시도(@Retryable)
@EnableJpaAuditing // 자동으로 시간 측정
@EntityListeners(AuditingEntityListener.class) // entity 이벤트 감지해서 시간 컬럼 자동으로 채워줌(@CreatedDate, @LastModifiedDate
@SpringBootApplication
@EnableScheduling // 스케줄링
public class BeApplication {

    public static void main(String[] args) {
        SpringApplication.run(BeApplication.class, args);
    }

}
