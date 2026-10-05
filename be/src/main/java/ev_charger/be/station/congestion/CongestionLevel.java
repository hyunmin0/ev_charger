package ev_charger.be.station.congestion;

import ev_charger.be.common.converter.CodeEnum;

public enum CongestionLevel implements CodeEnum {
    SPACIOUS("여유"),
    NORMAL("보통"),
    CONGESTED("혼잡");

    private final String code;

    CongestionLevel(String code) {
        this.code = code;
    }

    @Override
    public String getCode() {
        return code;
    }

}
