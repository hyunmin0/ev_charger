package ev_charger.be.user;

import ev_charger.be.user.profileImage.ProfileImage;
import jakarta.persistence.*;
import lombok.*;
import jakarta.validation.constraints.Email;

import java.util.UUID;

@Entity
@Table(name="users",
        uniqueConstraints = { @UniqueConstraint(
                columnNames = {"provider", "provider_id"})})
@Getter
@NoArgsConstructor(access=AccessLevel.PROTECTED)
public class User {
    @Id // 기본키 -> id 기준으로 객체 추적
    @Column(name="user_id")
    @GeneratedValue(strategy = GenerationType.UUID) // id 값을 자동 생성
    private UUID userId;

    @Column(nullable=false, length=50)
    private String nickname;

    @JoinColumn(name="profile_image_id")
    @ManyToOne(fetch=FetchType.LAZY)
    private ProfileImage profileImage;

    @Email
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false)
    private Provider provider;

    @Column(name="provider_id", nullable=false)
    private String providerId;

    @Column(name="refresh_token", length=512)
    private String refreshToken; //jwt(로그인 유지용)

    @Builder
    public User( String nickname, ProfileImage profileImage, String email, Provider provider, String providerId, String refreshToken) {
        this.nickname = nickname;
        this.profileImage = profileImage;
        this.email = email;
        this.provider = provider;
        this.providerId = providerId;
        this.refreshToken = refreshToken;
    }


    public void updateRefreshToken(String refreshToken){
    this.refreshToken = refreshToken;
}

    public void updateNickname(String newName) { this.nickname = newName; }

    public void updateProfileImage(ProfileImage newProfileImage) {this.profileImage = newProfileImage; }

}
