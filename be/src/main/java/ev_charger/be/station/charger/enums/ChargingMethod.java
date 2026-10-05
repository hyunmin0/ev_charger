package ev_charger.be.station.charger.enums;
// 충전 방식 enum

import ev_charger.be.common.converter.CodeEnum;
import lombok.Getter;

@Getter
public enum ChargingMethod implements CodeEnum {
    SOLO("단독"),
    SIMULTANEOUS("동시");

    private final String code;

    ChargingMethod(String code) {
        this.code = code;
    }
}
