package ev_charger.be.notice;

import ev_charger.be.notice.dto.request.NoticeCreateRequest;
import ev_charger.be.notice.dto.response.NoticeListResponse;
import ev_charger.be.notice.dto.response.NoticeResponse;
import ev_charger.be.notification.NotificationHistory;
import ev_charger.be.notification.NotificationHistoryRepository;
import ev_charger.be.notification.NotificationHistoryService;
import ev_charger.be.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly=true)
@RequiredArgsConstructor
public class NoticeService {

    private final NoticeRepository noticeRepository;
    private final NotificationHistoryRepository notificationHistoryRepository;
    private final NotificationHistoryService notificationHistoryService;

    // 공지 등록
    @Transactional
    public void createNotice(NoticeCreateRequest request) {
        if (noticeRepository.existsByTitleAndContent(request.title(), request.content())) {
            throw new IllegalArgumentException("이미 존재하는 공지입니다.");
        }

        noticeRepository.save(Notice.builder()
                .title(request.title())
                .content(request.content())
                .build());
    }


    // 공지 단일 조회
    @Transactional
    public NoticeResponse getNotice(User user, long id) {
        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 공지입니다."));

        notificationHistoryService.save(user, notice);

        return new NoticeResponse(notice.getNoticeId(), notice.getTitle(), notice.getContent(), notice.getCreatedAt());
    }

    // 공지 목록 조회
    public Page<NoticeListResponse> getNotices(User user, Pageable pageable) {
        // pageable에 해당하는 notices
        Page<Notice>  notices = noticeRepository.findAll(pageable);

        // 읽은 notice의 id set
        // notice마다 contains()를 반복 호출하므로 set
        // notice가 동일한 id라도 다른 인스턴스면 false가 나올 수 있기에 id를 비교
            // e.g. notices: findAll로 직접 조회한 실제 엔티티
            //      notificationHistory의 notice: 지연로딩되므로 Hibernate 프록시일 가능성 높음(fetch.lazy)
        Set<Long> readIds = notificationHistoryRepository.findByUserAndNoticeIn(user, notices.getContent())
                .stream().map(h -> h.getNotice().getNoticeId()).collect(Collectors.toSet());

        // isRead: readIds에 해당 notice id가 있는지
        return notices.map(n -> NoticeListResponse.from(n, readIds.contains(n.getNoticeId())));

    }
}