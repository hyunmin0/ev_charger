package ev_charger.be.dto;

import java.util.UUID;

public class UserCarDto {
    public record ListResponse(UUID carid, String model, float batteryCapacity) {}
}
// input은 ~~Request
// output은 ~~Response