package ev_charger.be.user.dto.response;

import java.util.List;

public record UserResponse (String nickname, String email, String imageUrl, List<String> model){
}
