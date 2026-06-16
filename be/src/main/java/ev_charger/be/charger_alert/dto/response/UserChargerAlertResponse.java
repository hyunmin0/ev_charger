package ev_charger.be.charger_alert.dto.response;

import ev_charger.be.station.charger.enums.ChgerStat;
import ev_charger.be.station.charger.enums.ChgerType;

import java.util.List;

public record UserChargerAlertResponse(
        String statId,
        String statNm,
        String useTime,
        Boolean parkingFree,
        Boolean openToPublic,
        String kind,
        String floorType,
        boolean hasFast,
        String busiNm,
        List<AlertedCharger> alertedChargers
) {
    public record AlertedCharger (
            String chgerId,
            ChgerType chgerType,
            String output,
            ChgerStat chgerStat
    ) {}
}
