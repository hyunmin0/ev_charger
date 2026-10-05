package ev_charger.be.station.charger.converter;
// ChgerType enum <-> db 코드값 변환 converter

import ev_charger.be.common.converter.CodeEnumConverter;
import ev_charger.be.station.charger.enums.ChgerType;
import jakarta.persistence.Converter;

@Converter(autoApply = true) // 해당 타입 사용 시 자동 적용
public class ChgerTypeConverter extends CodeEnumConverter<ChgerType> {
    public ChgerTypeConverter() { super(ChgerType.class); } // ChgerType으로 특정
}
