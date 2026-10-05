package ev_charger.be.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;

@Configuration
public class FirebaseConfig {
    // Firebase Admin SDK의 최상위 객체
    // Firebase 프로젝트 연결 정보 + 서비스 계정 인증 + Firebase SDK 초기화
    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        // 서비스 계정 키
        ClassPathResource resource =
                new ClassPathResource("firebase-service-account.json"); // resources/

        // Firebase SDK 설정값
        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials( // Firebase 서버에게 프로젝트의 관리자 서버임을 증명(인증)
                        // json을 읽어서 google 서비스 계정 인증 객체를 생성
                        GoogleCredentials.fromStream(resource.getInputStream()) // IOException 발생 가능
                        // FireBase 설정 파일 없음 -> 애플리케이션 시작 실패
                )
                .build();

        // Firebase 초기화
        // 이후부터 FCM 사용 가능
        return FirebaseApp.initializeApp(options);
    }

    // spring이 자동으로 firebaseApp을 찾아서 넣어줌
    // 작동 순서: firebase App Bean 생성 -> firebaseMessaging Bean 생성
    @Bean
    public FirebaseMessaging firebaseMessaging(FirebaseApp firebaseApp) {
        return FirebaseMessaging.getInstance(firebaseApp); // FCM 전송 전용 객체
    }
}
