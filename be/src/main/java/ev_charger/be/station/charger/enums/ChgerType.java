package ev_charger.be.station.charger.enums;
// 충전기 타입 enum

import ev_charger.be.common.converter.CodeEnum;
import lombok.Getter;

@Getter
public enum ChgerType implements CodeEnum {
    DC_DEMO("01"), // DC차데모
    AC_SLOW("02"), // AC완속
    DC_DEMO_AC3("03"), // DC차데모 + AC3상
    DC_COMBO("04"), // DC콤보
    DC_DEMO_DE_COMBO("05"), // DC차데모 + DC콤보
    DC_DEMO_AC3_DC_COMBO("06"), // DC차데모 + AC3상 + DC콤보
    AC3("07"), // AC3상
    DC_COMBO_SLOW("08"), // DC콤보(완속)
    NACS("09"),
    DC_COMBO_NACS("10"), // DC콤보 + NACS
    DC_COMBO2_BUS("11"); // DC콤보2(버스전용)

    private final String code;


    ChgerType(String code) {
        this.code = code;
    }
}
