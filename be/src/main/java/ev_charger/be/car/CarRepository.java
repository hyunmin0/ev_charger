package ev_charger.be.car;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CarRepository extends JpaRepository<Car, Long> {

    /**
     * 차량 검색: "브랜드 모델 트림"을 이어 붙인 이름에 검색어가 포함된 차량 조회 (대소문자 무시)
     * 브랜드/모델 칸을 따로 보면 "테슬라 Model Y"처럼 칸을 걸친 검색어는 0건이 됨 -> 이어 붙여서 비교
     * 예) "아이오닉", "테슬라 Model Y", "Model Y 프리미엄" 모두 검색됨
     * @param keyword 검색어 (공백은 한 칸으로 정리해서 넘김)
     * @return 조건에 맞는 차량 목록
     */
    @Query("""
            select c from Car c
            where lower(concat(c.brand, ' ', c.model, ' ', coalesce(c.trim, ''))) like lower(concat('%', :keyword, '%'))
            """)
    List<Car> searchByName(@Param("keyword") String keyword);
}