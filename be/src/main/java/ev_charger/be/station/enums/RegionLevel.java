package ev_charger.be.station.enums;

/**
 * 지도 요약 단위
 * SIDO: 시·도 (전국을 볼 때), CITY: 도 안의 시·군 + 광역시 (도 하나를 볼 때)
 */
public enum RegionLevel {
    SIDO, CITY;

    // 요청 파라미터 "sido" / "city" (대소문자 무시)
    public static RegionLevel from(String value) {
        for (RegionLevel level : values()) {
            if (level.name().equalsIgnoreCase(value)) return level;
        }
        throw new IllegalArgumentException("level은 sido 또는 city만 가능합니다.");
    }
}
