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
    private String imageUrl;
    private String name;

    @Builder
    public ProfileImage(String imageUrl, String name) {
        this.imageUrl = imageUrl;
        this.name = name;
    }
}
