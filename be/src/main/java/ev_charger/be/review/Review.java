package ev_charger.be.review;

import ev_charger.be.review.dto.request.ReviewRequest;
import ev_charger.be.station.Station;
import ev_charger.be.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.time.LocalDateTime;

@Entity
@Table(name="review")
@EntityListeners(AuditingEntityListener.class) // created 자동 시간 측정
@NoArgsConstructor(access= AccessLevel.PROTECTED)
@Getter
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="review_id")
    private Long reviewId;

    @JoinColumn(name="user_id", nullable=false)
    @ManyToOne(fetch=FetchType.LAZY)
    private User user;

    @Column(length=500, nullable = false)
    private String content;

    @Min(1)
    @Max(5)
    @Column(nullable = false)
    private Integer rating;

    @JoinColumn(name="lastId", nullable=false)
    @ManyToOne(fetch = FetchType.LAZY)
    private Station station;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public Review(User user, String content, Integer rating, Station station) {
        this.user = user;
        this.content = content;
        this.rating = rating;
        this.station = station;
    }

    public void updatedContentAndRating(ReviewRequest request) {
        this.content = request.content();
        this.rating = request.rating();
    }
}
