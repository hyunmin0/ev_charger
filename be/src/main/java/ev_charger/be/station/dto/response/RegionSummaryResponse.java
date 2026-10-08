package ev_charger.be.station.dto.response;

/**
 * 지역별 충전소 요약 (지도를 줌아웃했을 때 표시)
 */
public record RegionSummaryResponse(
        String code, // 지역 키 (시·도·광역시: zcode, 시·군: zscode 앞 4자리, 광주: 12-gwangju)
        String name, // 서울, 경기 / 수원시, 무안군 ...
        long stationCount,
        long availableStationCount, // 충전대기 충전기가 1대 이상인 충전소 수
        double lat, // 그 지역 충전소 위치의 중앙값 (말풍선 표시 위치)
        double lng
) {
}
