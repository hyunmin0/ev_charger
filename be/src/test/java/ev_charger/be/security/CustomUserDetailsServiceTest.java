package ev_charger.be.security;

import ev_charger.be.user.User;
import ev_charger.be.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.catchThrowable;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
public class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    private long startTime;

    @BeforeEach
    void setUp() {
        startTime = System.currentTimeMillis();
    }

    @Test
    void 존재하는_유저는_UserDetails를_반환한다() {
        // given
        UUID userId = UUID.randomUUID(); // DB에 있다고 가정할 유저의 식별자
        User user = User.builder()
                .nickname("nick")
                .email("test@test.com")
                .build(); // 실제 DB 없이 테스트용으로 직접 만든 User 객체
        ReflectionTestUtils.setField(user, "userId", userId);
        // User.userId는 @GeneratedValue라 builder로 못 채우니, 리플렉션으로 강제 주입

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        // userRepository는 가짜(mock)라 진짜 DB 조회 안 함
        // "findById(userId)로 호출되면 위에서 만든 user를 리턴해라"라고 미리 답을 정해둠

        // when
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(userId.toString());

        // then
        assertThat(userDetails).isInstanceOf(CustomUserDetails.class);
        // loadUserByUsername이 UserDetails 타입을 리턴하지만
        // 실제로는 CustomUserDetails 구현체여야 함 (다른 구현체가 섞여 반환되면 안 됨)

        assertThat(userDetails.getUsername()).isEqualTo(userId.toString());
        // CustomUserDetails.getUsername()은 user.getUserId().toString()을 리턴하도록 구현돼 있으니 미리 설정한 userId랑 같은 값이 나와야 정상

        assertThat(((CustomUserDetails) userDetails).getUser()).isSameAs(user);
        // CustomUserDetails가 내부에 감싸고 있는 User 객체가 mock으로 리턴하도록 설정한 그 user 인스턴스와 같은 객체인지 확인
        // (User가 equals()를 오버라이드 안 해서 isEqualTo 대신 isSameAs로 참조 비교)
    }

    @Test
    void 존재하지_않는_유저는_예외를_던진다() {
        // given
        UUID userId = UUID.randomUUID();
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // when
        Throwable thrown = catchThrowable(() ->
                customUserDetailsService.loadUserByUsername(userId.toString()));
                // 실행한 후 던져진 예외를 변수에 담아둠

        // then
        assertThat(thrown).isInstanceOf(UsernameNotFoundException.class);

    }

    @AfterEach
    void tearDown() {
        System.out.println("경과된 시간:" + (System.currentTimeMillis() - startTime) + "ms");
    }
}
