package ev_charger.be.station.enums;

import java.util.List;
import java.util.Map;

/**
 * station.zcode(시·도 코드) -> 지도에 표시할 짧은 이름
 * 주소 앞부분은 "서울"/"서울특별시"처럼 표기가 섞여 있어서 코드로 이름을 정함
 */
public final class Sido {

    private static final Map<String, String> NAMES = Map.ofEntries(
            Map.entry("11", "서울"),
            Map.entry("12", "전남광주"), // 전남광주통합특별시
            Map.entry("26", "부산"),
            Map.entry("27", "대구"),
            Map.entry("28", "인천"),
            Map.entry("30", "대전"),
            Map.entry("31", "울산"),
            Map.entry("36", "세종"),
            Map.entry("41", "경기"),
            Map.entry("43", "충북"),
            Map.entry("44", "충남"),
            Map.entry("47", "경북"),
            Map.entry("48", "경남"),
            Map.entry("50", "제주"),
            Map.entry("51", "강원"),
            Map.entry("52", "전북")
    );

    // 구로 나누지 않고 하나로 보여줄 특별시·광역시·특별자치시
    public static final List<String> METRO_CODES = List.of("11", "26", "27", "28", "30", "31", "36");

    // 전남광주(12) 안의 광주 구들 (동구 1221 ~ 광산구 1233, zscode 앞 4자리) -> "광주" 하나로 묶음
    public static final String GWANGJU_ZCODE = "12";
    public static final String GWANGJU_ZSCODE_FROM = "1220";
    public static final String GWANGJU_ZSCODE_TO = "1239";
    public static final String GWANGJU_KEY = "12-gwangju";
    public static final String GWANGJU_NAME = "광주";

    private Sido() {}

    // 모르는 코드면 코드를 그대로 보여줌
    public static String nameOf(String code) {
        return NAMES.getOrDefault(code, code);
    }
}
