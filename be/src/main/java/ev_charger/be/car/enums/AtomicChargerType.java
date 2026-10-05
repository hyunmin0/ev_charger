package ev_charger.be.car.enums;

import ev_charger.be.common.converter.CodeEnum;
import lombok.Getter;

@Getter
public enum AtomicChargerType implements CodeEnum {
    DC_DEMO("01"),        // DC차데모
    AC_SLOW("02"),        // AC완속
    DC_COMBO("04"),       // DC콤보
    AC3("07"),             // AC3상
    DC_COMBO_SLOW("08"),  // DC콤보(완속)
    NACS("09"),
    DC_COMBO2_BUS("11");  // DC콤보2(버스전용)

    private final String code;

    AtomicChargerType(String code) {
        this.code = code;
    }
}