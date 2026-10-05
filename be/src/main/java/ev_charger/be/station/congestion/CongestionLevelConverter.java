package ev_charger.be.station.congestion;

import ev_charger.be.common.converter.CodeEnumConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class CongestionLevelConverter extends CodeEnumConverter<CongestionLevel> {
    public CongestionLevelConverter() {
        super(CongestionLevel.class);
    }
}
