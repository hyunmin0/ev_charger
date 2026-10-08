package ev_charger.be.station.congestion;

import com.fasterxml.jackson.annotation.JsonValue;
import ev_charger.be.common.converter.CodeEnum;

public enum CongestionLevel implements CodeEnum {
    SPACIOUS("여유"),
    NORMAL("보통"),
    CONGESTED("혼잡");

    private final String code;

    CongestionLevel(String code) {
        this.code = code;
    }

    // DB 값과 같은 "여유"/"보통"/"혼잡"을 JSON으로도 내보냄 (없으면 SPACIOUS 등 상수 이름이 나감)
    @JsonValue
    @Override
    public String getCode() {
        return code;
    }

}
