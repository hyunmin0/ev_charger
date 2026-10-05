package ev_charger.be.car.dto.response;

import ev_charger.be.car.charge.Charge;

public record ChargeResponse(
        String chargerType,
        Integer chargerOutput, // 없으면 null
        int minutes // 충전 소요 시간
) {
    public static ChargeResponse from(Charge charge) {
        return new ChargeResponse(
                charge.getChargerType(),
                charge.getChargerOutput(),
                charge.getMinutes()
        );
    }
}
