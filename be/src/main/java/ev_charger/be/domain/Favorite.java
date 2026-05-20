package ev_charger.be.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name="favorite")
@Getter
@NoArgsConstructor(access= AccessLevel.PROTECTED)
public class Favorite {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private UUID userid;
    private int statId;
    private LocalDateTime created;


    @Builder
    public Favorite(UUID userid, int statId, LocalDateTime created) {
        this.userid = userid;
        this.statId = statId;
        this.created = created;
    }
}
