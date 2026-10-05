package ev_charger.be.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

// 통합 테스트용 Redis 컨테이너 (JwtAuthenticationFilter가 요청마다 blacklist 조회)
// 로컬에서 띄운 6379 Redis에 테스트 데이터가 섞이지 않도록 별도 컨테이너 사용
// 사용: 테스트 클래스에 @Import(RedisTestcontainersConfig.class)
@TestConfiguration(proxyBeanMethods = false)
public class RedisTestcontainersConfig {

    // @ServiceConnection(name = "redis"): GenericContainer라 이름으로 redis임을 알려줘야 host/port가 자동 연결됨
    @Bean
    @ServiceConnection(name = "redis")
    GenericContainer<?> redisContainer() {
        return new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                .withExposedPorts(6379);
    }
}
