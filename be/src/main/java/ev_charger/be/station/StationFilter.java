package ev_charger.be.station;

import java.util.List;

public record StationFilter (
        boolean availableOnly, // true = waiting만
        boolean parkingFree,
        boolean limitYn, // true = 개방(='N')
        Integer minOutput,
        Integer maxOutput,
        List<String> chgerTypes,
        List<String> kinds,
        List<String> floorTypes
){}
