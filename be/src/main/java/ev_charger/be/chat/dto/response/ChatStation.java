package ev_charger.be.chat.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

// 챗봇이 추천한 충전소. 앱이 이미 쓰는 필드명 그대로 내려줌 (parkingFree는 'Y'/'N', 위치를 모르는 검색이면 distance_km은 null)
public record ChatStation(
        String statId,
        String statNm,
        String addr,
        String parkingFree,
        @JsonProperty("distance_km") Double distanceKm
) {}
