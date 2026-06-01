package ev_charger.be.favorite;

import java.time.LocalDateTime;
// 즐겨찾기 조회용 projection
// 네이티브 쿼리를 가져올 떄는 record 불가
public interface FavoriteProjection {
    String getStatId();
    String getStatNm();
    String getAddr();
    double getDistance();
    LocalDateTime getCreated();
}
