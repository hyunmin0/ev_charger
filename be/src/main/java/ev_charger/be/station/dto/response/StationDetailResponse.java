package ev_charger.be.station.dto.response;

import ev_charger.be.review.dto.response.StationReviewResponse;
import ev_charger.be.station.charger.enums.ChgerStat;
import ev_charger.be.station.charger.enums.ChgerType;
import ev_charger.be.station.congestion.CongestionLevel;

import java.util.List;

public record StationDetailResponse(
        String statId,
        String statNm,
        String addr,
        String addrDetail,
        String useTime,
        Boolean parkingFree,
        String note,
        Boolean openToPublic,
        String limitDetail,
        String kind,
        String kindDetail,
        String floorNum,
        String floorType,
        boolean hasFast, // 1개 이상이 급속
        String busiNm,
        String busiCall,
        Double averageRating, // 리뷰 없을 땐 null
        int reviewCount,
        Boolean isFavorite,
        List<ChgerDetail> chargers,
        List<StationReviewResponse> reviews,
        CongestionDetail congestions
) {
    public record ChgerDetail(
            String chgerId,
            ChgerType chgerType,
            String output,
            ChgerStat chgerStat,
            boolean isAlert
    ) {}

    public record CongestionDetail(
           Double accuracy,
           CongestionLevel oneHour,
           CongestionLevel twoHour,
           CongestionLevel threeHour
    ) {}
}
