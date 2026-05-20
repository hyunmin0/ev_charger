package ev_charger.be.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name="users")
@Getter
@NoArgsConstructor(access=AccessLevel.PROTECTED)
public class Member {
    private String nickname;

    @Id // 기본키 -> id 기준으로 객체 추적
    @GeneratedValue(strategy = GenerationType.UUID) // id 값을 자동 생성
    private UUID userid;

    private String profile;
    private String email;
    private String provider;
    private String providerId;
    private String refreshToken; //jwt(로그인 유지용)

    @Builder
    public Member(String nickname, String profile, String email, String provider, String providerId, String refreshToken) {
        this.nickname = nickname;
        this.profile = profile;
        this.email = email;
        this.provider = provider;
        this.providerId = providerId;
        this.refreshToken = refreshToken;
    }
}
