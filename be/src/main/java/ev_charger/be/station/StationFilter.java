package ev_charger.be.station;

import java.util.List;

// 사용자가 켠 필터만 보낼 수 있도록 모든 필드는 nullable (보내지 않은 값은 null = 조건 미적용)
public record StationFilter (
        Boolean availableOnly, // true = waiting만
        Boolean parkingFree,
        Boolean limitYn, // true = 개방(='N')
        Integer minOutput,
        Integer maxOutput,
        List<String> chgerTypes,
        List<String> kinds,
        List<String> floorTypes
){
    // 필터 없이 요청한 경우 사용 (조건 없이 전체 조회)
    public static StationFilter empty() {
        return new StationFilter(null, null, null, null, null, null, null, null);
    }
}
