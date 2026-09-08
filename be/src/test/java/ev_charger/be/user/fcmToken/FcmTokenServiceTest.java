package ev_charger.be.user.fcmToken;

import ev_charger.be.user.User;
import ev_charger.be.user.enums.Provider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
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
        given(fcmTokenRepository.existsByUserAndToken(user, token)).willReturn(false);

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
    void 중복된_토큰이면_등록시_예외_발생() {
        // given
        String token = "token-1";
        given(fcmTokenRepository.existsByUserAndToken(user, token)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> fcmTokenService.register(user, token))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("중복된 Fcm 토큰 값입니다.");

        verify(fcmTokenRepository, never()).save(any());
    }

    @Test
    void FCM_토큰_삭제_성공() {
        // given
        String token = "token-1";
        given(fcmTokenRepository.existsByUserAndToken(user, token)).willReturn(true);

        // when
        fcmTokenService.delete(user, token);

        // then
        verify(fcmTokenRepository).deleteByUserAndToken(user, token);
    }

    @Test
    void 존재하지_않는_토큰이면_삭제시_예외_발생() {
        // given
        String token = "token-1";
        given(fcmTokenRepository.existsByUserAndToken(user, token)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> fcmTokenService.delete(user, token))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Fcm 토큰이 존재하지 않습니다.");

        verify(fcmTokenRepository, never()).deleteByUserAndToken(any(), anyString());
    }

    @AfterEach
    void tearDown() {
        System.out.println("경과 시간: " + (System.currentTimeMillis() - startTime) + "ms");
    }
}
