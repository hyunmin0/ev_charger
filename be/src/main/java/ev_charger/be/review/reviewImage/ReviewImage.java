package ev_charger.be.review.reviewImage;

import ev_charger.be.review.Review;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name="review_image")
@NoArgsConstructor(access= AccessLevel.PROTECTED)
@Getter
public class ReviewImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="image_id")
    private Long imageId;

    @JoinColumn(nullable = false, name="review_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private Review review;

    @Column(name="image_url", nullable = false, length = 512)
    private String imageUrl;

    @Builder
    public ReviewImage(Review review, String imageUrl) {
        this.review = review;
        this.imageUrl = imageUrl;
    }
}
