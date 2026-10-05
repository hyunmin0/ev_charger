package ev_charger.be.notice;

import ev_charger.be.notice.dto.request.NoticeCreateRequest;
import ev_charger.be.notice.dto.response.NoticeListResponse;
import ev_charger.be.notice.dto.response.NoticeResponse;
import ev_charger.be.notification.NotificationHistory;
import ev_charger.be.notification.NotificationHistoryRepository;
import ev_charger.be.notification.NotificationHistoryService;
import ev_charger.be.user.User;
import ev_charger.be.user.enums.Provider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class NoticeServiceTest {

    @Mock
    private NoticeRepository noticeRepository;
    @Mock
    private NotificationHistoryRepository notificationHistoryRepository;
    @Mock
    private NotificationHistoryService notificationHistoryService;

    private NoticeService noticeService;

    private User user;

    private long startTime;

    @BeforeEach
    void setUp() {
        startTime = System.currentTimeMillis();
        noticeService = new NoticeService(
                noticeRepository,
                notificationHistoryRepository,
                notificationHistoryService
        );

        user = User.builder()
                .nickname("테스터")
                .email("test@example.com")
                .provider(Provider.GOOGLE)
                .providerId("google-1234")
                .build();
    }

    @Test
    void 공지_생성_성공() {
        // given
        NoticeCreateRequest request = new NoticeCreateRequest("점검 안내", "9/10 점검 예정입니다.");
        given(noticeRepository.existsByTitleAndContent(request.title(), request.content())).willReturn(false);

        // when
        noticeService.createNotice(request);

        // then
        ArgumentCaptor<Notice> captor = ArgumentCaptor.forClass(Notice.class);
        verify(noticeRepository).save(captor.capture());

        Notice saved = captor.getValue();
        assertThat(saved.getTitle()).isEqualTo("점검 안내");
        assertThat(saved.getContent()).isEqualTo("9/10 점검 예정입니다.");
    }

    @Test
    void 이미_존재하는_공지면_생성시_예외_발생() {
        // given
        NoticeCreateRequest request = new NoticeCreateRequest("점검 안내", "9/10 점검 예정입니다.");
        given(noticeRepository.existsByTitleAndContent(request.title(), request.content())).willReturn(true);

        // when & then
        assertThatThrownBy(() -> noticeService.createNotice(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이미 존재하는 공지입니다.");

        verify(noticeRepository, never()).save(any());
    }

    @Test
    void 공지_단일_조회_성공() {
        // given
        long noticeId = 1L;
        LocalDateTime createdAt = LocalDateTime.now();

        Notice notice = mock(Notice.class);
        given(notice.getNoticeId()).willReturn(noticeId);
        given(notice.getTitle()).willReturn("점검 안내");
        given(notice.getContent()).willReturn("9/10 점검 예정입니다.");
        given(notice.getCreatedAt()).willReturn(createdAt);

        given(noticeRepository.findById(noticeId)).willReturn(Optional.of(notice));

        // when
        NoticeResponse response = noticeService.getNotice(user, noticeId);

        // then
        assertThat(response.id()).isEqualTo(noticeId);
        assertThat(response.title()).isEqualTo("점검 안내");
        assertThat(response.content()).isEqualTo("9/10 점검 예정입니다.");
        assertThat(response.createdAt()).isEqualTo(createdAt);

        verify(notificationHistoryService).save(user, notice);
    }

    @Test
    void 유효하지_않은_공지면_조회시_예외_발생() {
        // given
        long noticeId = 1L;
        given(noticeRepository.findById(noticeId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> noticeService.getNotice(user, noticeId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("유효하지 않은 공지입니다.");

        verify(notificationHistoryService, never()).save(any(), any());
    }

    @Test
    void 공지_목록_조회시_읽음여부가_반영된다() {
        // given
        Pageable pageable = PageRequest.of(0, 10);

        Notice readNotice = mock(Notice.class);
        given(readNotice.getNoticeId()).willReturn(1L);
        given(readNotice.getTitle()).willReturn("읽은 공지");
        given(readNotice.getCreatedAt()).willReturn(LocalDateTime.now());

        Notice unreadNotice = mock(Notice.class);
        given(unreadNotice.getNoticeId()).willReturn(2L);
        given(unreadNotice.getTitle()).willReturn("안읽은 공지");
        given(unreadNotice.getCreatedAt()).willReturn(LocalDateTime.now());

        Page<Notice> notices = new PageImpl<>(List.of(readNotice, unreadNotice), pageable, 2);
        given(noticeRepository.findAll(pageable)).willReturn(notices);

        NotificationHistory history = mock(NotificationHistory.class);
        given(history.getNotice()).willReturn(readNotice);
        given(notificationHistoryRepository.findByUserAndNoticeIn(user, notices.getContent())).willReturn(List.of(history));

        // when
        Page<NoticeListResponse> responses = noticeService.getNotices(user, pageable);

        // then
        assertThat(responses.getContent()).hasSize(2);
        assertThat(responses.getContent().get(0).id()).isEqualTo(1L);
        assertThat(responses.getContent().get(0).isRead()).isTrue();
        assertThat(responses.getContent().get(1).id()).isEqualTo(2L);
        assertThat(responses.getContent().get(1).isRead()).isFalse();
    }

    @Test
    void 비로그인_공지_단일_조회시_읽음기록을_남기지_않는다() {
        // given
        long noticeId = 1L;

        Notice notice = mock(Notice.class);
        given(notice.getNoticeId()).willReturn(noticeId);
        given(notice.getTitle()).willReturn("점검 안내");

        given(noticeRepository.findById(noticeId)).willReturn(Optional.of(notice));

        // when
        NoticeResponse response = noticeService.getNotice(null, noticeId);

        // then
        assertThat(response.id()).isEqualTo(noticeId);
        assertThat(response.title()).isEqualTo("점검 안내");

        verify(notificationHistoryService, never()).save(any(), any());
    }

    @Test
    void 비로그인_공지_목록_조회시_모두_안읽음으로_반영된다() {
        // given
        Pageable pageable = PageRequest.of(0, 10);

        Notice notice1 = mock(Notice.class);
        given(notice1.getNoticeId()).willReturn(1L);

        Notice notice2 = mock(Notice.class);
        given(notice2.getNoticeId()).willReturn(2L);

        Page<Notice> notices = new PageImpl<>(List.of(notice1, notice2), pageable, 2);
        given(noticeRepository.findAll(pageable)).willReturn(notices);

        // when
        Page<NoticeListResponse> responses = noticeService.getNotices(null, pageable);

        // then
        assertThat(responses.getContent()).hasSize(2);
        assertThat(responses.getContent().get(0).id()).isEqualTo(1L);
        assertThat(responses.getContent().get(0).isRead()).isFalse();
        assertThat(responses.getContent().get(1).id()).isEqualTo(2L);
        assertThat(responses.getContent().get(1).isRead()).isFalse();

        verify(notificationHistoryRepository, never()).findByUserAndNoticeIn(any(), any());
    }

    @AfterEach
    void tearDown(TestInfo testInfo) {
        System.out.println(testInfo.getDisplayName() + " 경과 시간: " + (System.currentTimeMillis() - startTime) + "ms");
    }
}
