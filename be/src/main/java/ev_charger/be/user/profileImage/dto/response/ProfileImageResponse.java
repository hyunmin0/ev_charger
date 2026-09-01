package ev_charger.be.user.profileImage.dto.response;

import ev_charger.be.user.profileImage.ProfileImage;

public record ProfileImageResponse(
        Integer id,
        String imageUrl,
        String name
) {
    public static ProfileImageResponse from(ProfileImage profileImage) {
        return new ProfileImageResponse(
                profileImage.getId(),
                profileImage.getImageUrl(),
                profileImage.getName()
        );
    }
}
