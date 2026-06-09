package ev_charger.be.station.converter;

import ev_charger.be.common.converter.CodeEnumConverter;
import ev_charger.be.station.enums.Kind;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class KindConverter extends CodeEnumConverter<Kind> {
    public KindConverter() { super(Kind.class); }
}
