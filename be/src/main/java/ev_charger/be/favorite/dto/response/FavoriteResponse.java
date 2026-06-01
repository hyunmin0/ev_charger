package ev_charger.be.favorite.dto.response;

import java.time.LocalDateTime;

public record FavoriteResponse(
        String statId,
        String statNm,
        String addr,
        Double distance,
        LocalDateTime created

) {
}
