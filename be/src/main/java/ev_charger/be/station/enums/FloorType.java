package ev_charger.be.station.enums;
// 충전소 층(F = 지상, B = 지하)

import java.util.Arrays;

public enum FloorType {
    F("지상"), B("지하");

    private final String description;

    FloorType(String description) {
        this.description = description;
    }

    public static String descriptionOf(String code) {
        return Arrays.stream(values())
                .filter(f -> f.name().equals(code)) // name(): enum의 상수 이름을 문자열로 반환
                .map(f -> f.description)
                .findFirst().orElse(code);
    }
}
