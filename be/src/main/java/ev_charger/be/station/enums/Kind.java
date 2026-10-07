package ev_charger.be.station.enums;

import ev_charger.be.common.converter.CodeEnum;
import lombok.Getter;

import java.util.Arrays;

@Getter
public enum Kind implements CodeEnum {
    PUBLIC("A0", "공공시설"),
    PARKING("B0", "주차시설"),
    REST_AREA("C0", "휴게시설"),
    TOURIST("D0", "관광시설"),
    COMMERCIAL("E0", "상업시설"),
    CAR_MAINTENANCE("F0", "차량정비시설"),
    ETC("G0", "기타시설"),
    APARTMENT("H0", "공동주택시설"),
    NEIGHBORHOOD("I0", "근린생활시설"),
    EDUCATION("J0", "교육문화시설");

    private final String code;
    private final String description;

    Kind(String code, String description) {
        this.code = code;
        this.description = description;
    }


    @Override
    public String getCode() {
        return code;
    }

    public static String descriptionOf(String code) {
        return Arrays.stream(Kind.values())
                .filter(k -> k.code.equals(code))
                .map(k -> k.description)
                .findFirst()
                .orElse(code);
    }
}
