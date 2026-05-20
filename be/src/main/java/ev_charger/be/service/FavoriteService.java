package ev_charger.be.service;

import ev_charger.be.repository.FavoriteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;

    public void addFavorite(int statId) {
        // statId가 station 테이블에 존재하는지 검증
        // statId가 user의 favorite에 있는지 확인
        // userid, statId 저장
    }

    public void deleteFavorite(int statId) {
        // statId가 user의 favorite에 있는지 검증
        // statId 제거
    }

    public  List</**레코드만들기**/> getFavoriteList() {
        // userid로 favorite 조회
        // 위의 결과 중 stateId와 station 테이블을 조인
        // 이름
        // 리뷰수
        // 간단한 도로명 주소, addr, addrDetail
        // 내 위치에서의 거리(즉, location)
        // 이용 가능한 충전기 수, 그에 맞춰서 이용가능/충전중/고장
        // 충전타입
        // created

    }

}

