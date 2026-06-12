package ev_charger.be.favorite;

import ev_charger.be.station.Station;
import ev_charger.be.station.StationRepository;
import ev_charger.be.station.dto.response.StationResponse;
import ev_charger.be.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final StationRepository stationRepository;

    /**
     * 즐찾 추가
     * @param user
     * @param statId
     */
    @Transactional
    public void addFavorite(User user, String statId) {
        // statId가 station 테이블에 존재하는지 검증
        Station station = stationRepository.findById(statId)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 충전소입니다."));

        // station이 user의 favorite에 있는지 확인
        if (favoriteRepository.existsByUserAndStation(user, station)) {
                    throw new IllegalArgumentException("이미 등록된 충전소입니다.");
                }

        // db 저장
        favoriteRepository.save(Favorite.builder()
                .user(user)
                .station(station)
                .build());

    }

    /**
     * 즐찾 제거
     * @param user
     * @param statId
     */
    @Transactional
    public void deleteFavorite(User user, String statId) {
        // statId가 user의 favorite에 있는지 검증
        Station station = stationRepository.findById(statId)
                .orElseThrow(() -> new IllegalArgumentException("즐겨찾기에 존재하지 않는 충전소입니다."));

        // lastId 제거
        favoriteRepository.deleteByUserAndStation(user, station);
    }

    /**
     * 즐겨찾기 조회
     * @param user
     * @param lat
     * @param lng
     * @return
     */
    public  List<StationResponse> getFavoriteList(User user, double lat, double lng) {
        return stationRepository.findFavoriteStations(user.getUserId(), lat, lng);
    }


}

