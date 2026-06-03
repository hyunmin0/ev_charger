package ev_charger.be.favorite;

import ev_charger.be.favorite.dto.response.FavoriteResponse;
import ev_charger.be.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    // 즐겨찾기 추가
    // input : statId(String)
    // output: 없음 (200 OK)
    @PostMapping("/{statId}")
    public ResponseEntity<Void> addFavorite(
            @AuthenticationPrincipal CustomUserDetails userDetails, // 로그인한 유저
            @PathVariable String statId) {                          // URL에서 충전소 id 받음
        favoriteService.addFavorite(userDetails.getUser(), statId);
        return ResponseEntity.ok().build();
    }

    // 즐겨찾기 삭제
    // input : statId(String)
    // output: 없음 (200 OK)
    @DeleteMapping("/{statId}")
    public ResponseEntity<Void> deleteFavorite(
            @AuthenticationPrincipal CustomUserDetails userDetails, // 로그인한 유저
            @PathVariable String statId) {                          // URL에서 충전소 id 받음
        favoriteService.deleteFavorite(userDetails.getUser(), statId);
        return ResponseEntity.ok().build();
    }

    // 즐겨찾기 목록 조회
    // input : lat(double), lng(double) - 내 위치 (위도, 경도)
    // output: List<FavoriteResponse> [{ statId, statNm, addr, distance, created }, ...]
    @GetMapping
    public ResponseEntity<List<FavoriteResponse>> getFavoriteList(
            @AuthenticationPrincipal CustomUserDetails userDetails, // 로그인한 유저
            @RequestParam double lat,                               // 내 위도
            @RequestParam double lng) {                             // 내 경도
        return ResponseEntity.ok(favoriteService.getFavoriteList(userDetails.getUser(), lat, lng));
    }
}