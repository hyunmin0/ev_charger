package ev_charger.be.station.enums;

import ev_charger.be.common.converter.CodeEnum;
import lombok.Getter;

import java.util.Arrays;

@Getter
public enum Kind implements CodeEnum {
    PUBLIC("AO", "공공시설"),
    PARKING("BO", "주차시설"),
    REST_AREA("CO", "휴게시설"),
    TOURIST("DO", "관광시설"),
    COMMERCIAL("EO", "상업시설"),
    CAR_MAINTENANCE("FO", "차량정비시설"),
    ETC("GO", "기타시설"),
    APARTMENT("HO", "공동주택시설"),
    NEIGHBORHOOD("IO", "근린생활시설"),
    EDUCATION("JO", "교육문화시설");

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
