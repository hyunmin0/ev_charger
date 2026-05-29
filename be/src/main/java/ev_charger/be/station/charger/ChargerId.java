package ev_charger.be.station.charger;
// Charger 복합키 클래스(statId + chgerId)

import lombok.EqualsAndHashCode;

import java.io.Serializable;

@EqualsAndHashCode
public class ChargerId implements Serializable {
    private String statId;
    private String chgerId;
}
