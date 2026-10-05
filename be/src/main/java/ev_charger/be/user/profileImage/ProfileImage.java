package ev_charger.be.user.profileImage;


import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name="profile_image")
@Getter
@NoArgsConstructor(access= AccessLevel.PROTECTED)
public class ProfileImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "image_url", nullable = false, length = 512, unique = true)
    private String imageUrl;

    @Column(length = 50, nullable = false, unique = true)
    private String name;

    @Builder
    public ProfileImage(String imageUrl, String name) {
        this.imageUrl = imageUrl;
        this.name = name;
    }
}
