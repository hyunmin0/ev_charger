package ev_charger.be.station.charger.enums;
// 충전기 타입 enum

import ev_charger.be.car.enums.AtomicChargerType;
import ev_charger.be.common.converter.CodeEnum;
import lombok.Getter;

import java.util.Set;

@Getter
public enum ChgerType implements CodeEnum {
    DC_DEMO("01", Set.of(AtomicChargerType.DC_DEMO)), // DC차데모
    AC_SLOW("02", Set.of(AtomicChargerType.AC_SLOW)), // AC완속
    DC_DEMO_AC3("03", Set.of(AtomicChargerType.DC_DEMO, AtomicChargerType.AC3)), // DC차데모 + AC3상
    DC_COMBO("04", Set.of(AtomicChargerType.DC_COMBO)), // DC콤보
    DC_DEMO_DE_COMBO("05", Set.of(AtomicChargerType.DC_DEMO, AtomicChargerType.DC_COMBO)), // DC차데모 + DC콤보
    DC_DEMO_AC3_DC_COMBO("06", Set.of(AtomicChargerType.DC_DEMO, AtomicChargerType.AC3, AtomicChargerType.DC_COMBO)), // DC차데모 + AC3상 + DC콤보
    AC3("07", Set.of(AtomicChargerType.AC3)), // AC3상
    DC_COMBO_SLOW("08", Set.of(AtomicChargerType.DC_COMBO_SLOW)), // DC콤보(완속)
    NACS("09", Set.of(AtomicChargerType.NACS)),
    DC_COMBO_NACS("10", Set.of(AtomicChargerType.DC_COMBO, AtomicChargerType.NACS)), // DC콤보 + NACS
    DC_COMBO2_BUS("11", Set.of(AtomicChargerType.DC_COMBO2_BUS)); // DC콤보2(버스전용)

    private final String code;
    private final Set<AtomicChargerType> supportedTypes;

    ChgerType(String code, Set<AtomicChargerType> supportedTypes) {
        this.code = code;
        this.supportedTypes = supportedTypes;
    }
}