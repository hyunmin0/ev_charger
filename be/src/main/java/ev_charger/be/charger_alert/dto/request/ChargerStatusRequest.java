package ev_charger.be.charger_alert.dto.request;

import ev_charger.be.station.charger.enums.ChgerStat;

public record ChargerStatusRequest(
        String statId,
        String chgerId
) {}