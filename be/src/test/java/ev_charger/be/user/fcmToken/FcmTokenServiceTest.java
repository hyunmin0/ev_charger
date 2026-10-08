package ev_charger.be.user.fcmToken;

import ev_charger.be.user.User;
import ev_charger.be.user.enums.Provider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FcmTokenServiceTest {

    @Mock
    private FcmTokenRepository fcmTokenRepository;

    private FcmTokenService fcmTokenService;

    private User user;

    private long startTime;

    @BeforeEach
    void setUp() {
        startTime = System.currentTimeMillis();
        fcmTokenService = new FcmTokenService(fcmTokenRepository);

        user = User.builder()
                .nickname("테스터")
                .email("test@example.com")
                .provider(Provider.GOOGLE)
                .providerId("google-1234")
                .build();
    }

    @Test
    void FCM_토큰_등록_성공() {
        // given
        String token = "token-1";
        given(fcmTokenRepository.findByToken(token)).willReturn(Optional.empty());

        // when
        fcmTokenService.register(user, token);

        // then
        ArgumentCaptor<FcmToken> captor = ArgumentCaptor.forClass(FcmToken.class);
        verify(fcmTokenRepository).save(captor.capture());

        FcmToken saved = captor.getValue();
        assertThat(saved.getUser()).isEqualTo(user);
        assertThat(saved.getToken()).isEqualTo(token);
    }

    @Test
    void 이미_등록된_토큰이면_그대로_둔다() {
        // given: 앱이 실행할 때마다 같은 토큰을 다시 보냄
        String token = "token-1";
        User me = userWithId(UUID.randomUUID());
        FcmToken existing = new FcmToken(me, token);
        given(fcmTokenRepository.findByToken(token)).willReturn(Optional.of(existing));

        // when
        fcmTokenService.register(me, token);

        // then
        verify(fcmTokenRepository, never()).save(any());
        assertThat(existing.getUser()).isEqualTo(me);
    }

    @Test
    void 다른_계정의_토큰이면_주인을_바꾼다() {
        // given: 같은 기기에서 로그아웃 없이 다른 계정으로 로그인
        String token = "token-1";
        User before = userWithId(UUID.randomUUID());
        User after = userWithId(UUID.randomUUID());
        FcmToken existing = new FcmToken(before, token);
        given(fcmTokenRepository.findByToken(token)).willReturn(Optional.of(existing));

        // when
        fcmTokenService.register(after, token);

        // then
        verify(fcmTokenRepository, never()).save(any());
        assertThat(existing.getUser()).isEqualTo(after);
    }

    @Test
    void FCM_토큰_삭제_성공() {
        // given
        String token = "token-1";

        // when
        fcmTokenService.delete(user, token);

        // then: 없는 토큰이어도 예외 없이 삭제 쿼리만 실행
        verify(fcmTokenRepository).deleteByUserAndToken(user, token);
    }

    private User userWithId(UUID userId) {
        User u = mock(User.class);
        given(u.getUserId()).willReturn(userId);
        return u;
    }

    @AfterEach
    void tearDown(TestInfo testInfo) {
        System.out.println(testInfo.getDisplayName() + " 경과 시간: " + (System.currentTimeMillis() - startTime) + "ms");
    }
}
