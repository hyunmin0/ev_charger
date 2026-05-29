package ev_charger.be.station.charger.converter;
// ChgerStat enum <-> db 코드값 변환 converter

import ev_charger.be.common.converter.CodeEnumConverter;
import ev_charger.be.station.charger.enums.ChgerStat;
import jakarta.persistence.Converter;

@Converter(autoApply = true) // 해당 타입 사용 시 자동 적용
public class ChgerStatConverter extends CodeEnumConverter<ChgerStat> {
    public ChgerStatConverter() { super(ChgerStat.class); } // ChgerStat로 특정
}
