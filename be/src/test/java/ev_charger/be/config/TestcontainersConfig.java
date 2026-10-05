package ev_charger.be.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

// 네이티브 쿼리(PostGIS 함수)를 쓰는 레포지토리 테스트용 DB 컨테이너
// 사용: 테스트 클래스에 @Import(TestcontainersConfig.class)
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfig {

    // postgis 이미지는 postgres 이미지와 호환된다고 명시해야 PostgreSQLContainer로 띄울 수 있음
    private static final DockerImageName POSTGIS_IMAGE =
            DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres");

    // 실제 DB 스키마 기준 문서 (경로 기준: be 프로젝트 폴더)
    private static final String SCHEMA_PATH = "../sql/table.sql";

    // @ServiceConnection: 컨테이너의 url/username/password를 datasource 설정에 자동 연결
    @Bean
    @ServiceConnection
    PostgreSQLContainer postgisContainer() {
        return new PostgreSQLContainer(POSTGIS_IMAGE)
                // 컨테이너 첫 실행 시 /docker-entrypoint-initdb.d 안의 sql을 자동 실행
                // 20-: 이미지 기본 스크립트(10_postgis.sh) 다음에 실행되도록
                .withCopyFileToContainer(MountableFile.forHostPath(SCHEMA_PATH), "/docker-entrypoint-initdb.d/20-table.sql");
    }
}
