package ev_charger.be.station;
// 충전소 조회용 projection
// 네이티브 쿼리를 가져올 떄는 record 불가
public interface StationProjection{
    String getStatId();
    String getStatNm();
    String getAddr();
    double getLat();
    double getLng();
    String getUseTime();
    Integer getTotalCount();
    Integer getAvailableCount();
    double getDistance();
}
