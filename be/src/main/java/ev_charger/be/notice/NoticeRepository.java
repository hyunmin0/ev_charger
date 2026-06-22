package ev_charger.be.notice;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NoticeRepository extends JpaRepository<Notice, Long> {
    boolean existsByTitleAndContent(String title, String content);
}
