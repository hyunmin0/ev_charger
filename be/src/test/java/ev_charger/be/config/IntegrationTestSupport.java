package ev_charger.be.config;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import ev_charger.be.security.JwtProvider;
import ev_charger.be.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

// 통합 테스트 공통 설정
// - 실제 시큐리티 필터 + JWT + 서비스 + 리포지토리를 그대로 사용 (mock 없음)
// - DB: PostGIS 컨테이너 (table.sql), Redis: 별도 컨테이너
// - 설정이 같은 테스트끼리는 스프링 컨텍스트와 컨테이너를 한 번만 띄워서 공유
// - @Transactional: 테스트가 끝나면 DB 롤백 (Redis는 롤백되지 않음)
@SpringBootTest(properties = {
        // DB 설정은 StationRepositoryImplTest와 동일 (table.sql 스키마 + validate + 따옴표 컬럼명)
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.hibernate.naming.physical-strategy=org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl",
        "spring.jpa.properties.hibernate.globally_quoted_identifiers=true",
        "spring.jpa.properties.hibernate.globally_quoted_identifiers_skip_column_definitions=true"
})
@AutoConfigureMockMvc
@Import({TestcontainersConfig.class, RedisTestcontainersConfig.class})
@Transactional
public abstract class IntegrationTestSupport {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected JwtProvider jwtProvider;

    // 실제 FCM 서버로 푸시가 나가지 않도록 mock (서비스 계정 키 파일 없이도 컨텍스트가 뜨게 됨)
    @MockitoBean
    private FirebaseApp firebaseApp;

    @MockitoBean
    private FirebaseMessaging firebaseMessaging;

    // Authorization 헤더 값 (실제 JwtProvider로 발급한 access token)
    protected String bearer(User user) {
        return "Bearer " + jwtProvider.generateAccessToken(user.getUserId());
    }
}
