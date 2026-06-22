package ev_charger.be.security;
//로그인된 사용자 정보 객체

import ev_charger.be.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@RequiredArgsConstructor
public class CustomUserDetails implements UserDetails {
    private final User user;

    // 권한 목록
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // ROLE_USER: 일반 유저
        // ROLE_ADMIN: 관리자
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole()));
    }

    // Spring Security 식별자로 userId 사용
    @Override
    public String getUsername() {
        return user.getUserId().toString();
    }

    @Override
    public String getPassword() {
        return "";
    }

    // 컨트롤러가 User 객체를 가져오기 위해 필요
    public User getUser() { return user;}
}
