package ev_charger.be.car;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CarRepository extends JpaRepository<Car, Long> {

    /**
     * 차량 검색: 브랜드 또는 모델명에 검색어가 포함된 차량 조회 (대소문자 무시)
     * 메서드 이름으로 JPA가 쿼리를 자동 생성
     * -> where upper(brand) like upper('%brand%') or upper(model) like upper('%model%')
     * 검색창은 하나라서, 입력한 검색어를 브랜드 자리와 모델 자리에 똑같이 넣어서 호출
     * 예) "아이오닉" 검색 -> findBy...("아이오닉", "아이오닉")
     * @param brand 브랜드 검색어
     * @param model 모델명 검색어
     * @return 조건에 맞는 차량 목록
     */
    List<Car> findByBrandContainingIgnoreCaseOrModelContainingIgnoreCase(String brand, String model);
}