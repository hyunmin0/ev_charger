package ev_charger.be.car.converter;

import ev_charger.be.car.enums.AtomicChargerType;
import ev_charger.be.common.converter.CodeEnumConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class AtomicChargerTypeConverter extends CodeEnumConverter<AtomicChargerType> {
    public AtomicChargerTypeConverter() {
        super(AtomicChargerType.class);
    }
}