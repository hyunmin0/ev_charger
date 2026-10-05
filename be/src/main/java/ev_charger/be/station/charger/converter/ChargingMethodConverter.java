package ev_charger.be.station.charger.converter;
// method enum <-> db 코드값 변환 converter

import ev_charger.be.common.converter.CodeEnumConverter;
import ev_charger.be.station.charger.enums.ChargingMethod;
import jakarta.persistence.Converter;

@Converter(autoApply = true) // 해당 타입 사용 시 자동 적용
public class ChargingMethodConverter extends CodeEnumConverter<ChargingMethod> {
    public ChargingMethodConverter() { super(ChargingMethod.class); } //ChargingMethod로 특정
}
