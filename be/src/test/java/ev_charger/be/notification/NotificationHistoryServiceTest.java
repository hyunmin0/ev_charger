package ev_charger.be.notification;

import ev_charger.be.charger_alert.ChargerAlert;
import ev_charger.be.notice.Notice;
import ev_charger.be.notification.dto.NotificationHistoryResponse;
import ev_charger.be.station.charger.Charger;
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
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationHistoryServiceTest {

    @Mock
    private NotificationHistoryRepository notificationHistoryRepository;

    private NotificationHistoryService notificationHistoryService;

    private User user;

    private long startTime;

    @BeforeEach
    void setUp() {
        startTime = System.currentTimeMillis();
        notificationHistoryService = new NotificationHistoryService(notificationHistoryRepository);

        user = User.builder()
                .nickname("테스터")
                .email("test@example.com")
                .provider(Provider.GOOGLE)
                .providerId("google-1234")
                .build();
    }

    @Test
    void 충전기_알림_기록_저장_성공() {
        // given
        Charger charger = mock(Charger.class);
        given(charger.getStatId()).willReturn("ST1");
        given(charger.getChgerId()).willReturn("01");

        ChargerAlert alert = ChargerAlert.builder()
                .user(user)
                .charger(charger)
                .build();

        given(notificationHistoryRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        // when
        NotificationHistory saved = notificationHistoryService.save(alert);

        // then
        assertThat(saved.getStatId()).isEqualTo("ST1");
        assertThat(saved.getChgerId()).isEqualTo("01");
        assertThat(saved.getUser()).isEqualTo(user);
        assertThat(saved.isRead()).isFalse();
    }

    @Test
    void 공지_알림_기록_저장_성공() {
        // given
        Notice notice = mock(Notice.class);
        given(notificationHistoryRepository.existsByUserAndNotice(user, notice)).willReturn(false);

        // when
        notificationHistoryService.save(user, notice);

        // then
        ArgumentCaptor<NotificationHistory> captor = ArgumentCaptor.forClass(NotificationHistory.class);
        verify(notificationHistoryRepository).save(captor.capture());

        NotificationHistory saved = captor.getValue();
        assertThat(saved.getNotice()).isEqualTo(notice);
        assertThat(saved.getUser()).isEqualTo(user);
        assertThat(saved.isRead()).isTrue(); // noticeBuilder는 저장 시점에 이미 읽은 것으로 처리
    }

    @Test
    void 이미_읽은_공지면_중복_저장_안함() {
        // given
        Notice notice = mock(Notice.class);
        given(notificationHistoryRepository.existsByUserAndNotice(user, notice)).willReturn(true);

        // when
        notificationHistoryService.save(user, notice);

        // then
        verify(notificationHistoryRepository, never()).save(any());
    }

    @Test
    void 읽음_처리_성공() {
        // given
        Long id = 1L;
        NotificationHistory history = mock(NotificationHistory.class);
        given(notificationHistoryRepository.findByIdAndUser(id, user)).willReturn(Optional.of(history));

        // when
        notificationHistoryService.markAsRead(user, id);

        // then
        verify(history).updateIsRead();
    }

    @Test
    void 알림_기록이_없으면_읽음처리시_예외_발생() {
        // given
        Long id = 1L;
        given(notificationHistoryRepository.findByIdAndUser(id, user)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> notificationHistoryService.markAsRead(user, id))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("알림 기록이 없습니다.");
    }

    @Test
    void 충전기_알림_기록_조회_성공() {
        // given
        NotificationHistory history = mock(NotificationHistory.class);
        given(history.getId()).willReturn(1L);
        given(history.getStatId()).willReturn("ST1");
        given(history.getChgerId()).willReturn("01");
        LocalDateTime createdAt = LocalDateTime.now();
        given(history.getCreatedAt()).willReturn(createdAt);
        given(history.isRead()).willReturn(false);

        given(notificationHistoryRepository.findByUserAndChgerIdIsNotNull(eq(user), any(Sort.class)))
                .willReturn(List.of(history));

        // when
        List<NotificationHistoryResponse> responses = notificationHistoryService.getChargerAlertHistories(user);

        // then
        assertThat(responses).hasSize(1);
        NotificationHistoryResponse response = responses.get(0);
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.statId()).isEqualTo("ST1");
        assertThat(response.chgerId()).isEqualTo("01");
        assertThat(response.createdAt()).isEqualTo(createdAt);
        assertThat(response.isRead()).isFalse();
    }

    @Test
    void 단건_알림_기록_삭제_성공() {
        // given
        Long id = 1L;
        NotificationHistory history = mock(NotificationHistory.class);
        given(notificationHistoryRepository.findByIdAndUser(id, user)).willReturn(Optional.of(history));

        // when
        notificationHistoryService.deleteHistory(user, id);

        // then
        verify(notificationHistoryRepository).delete(history);
    }

    @Test
    void 알림_기록이_없으면_삭제시_예외_발생() {
        // given
        Long id = 1L;
        given(notificationHistoryRepository.findByIdAndUser(id, user)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> notificationHistoryService.deleteHistory(user, id))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("알림 기록이 없습니다.");

        verify(notificationHistoryRepository, never()).delete(any());
    }

    @Test
    void 모든_충전기_알림_기록_삭제_성공() {
        // when
        notificationHistoryService.deleteAllChargerAlertHistories(user);

        // then
        verify(notificationHistoryRepository).deleteByUserAndChgerIdIsNotNull(user);
    }

    @Test
    void 일주일_지난_충전기_알림_기록을_삭제한다() {
        // given
        LocalDateTime before = LocalDateTime.now().minusWeeks(1);

        // when
        notificationHistoryService.deleteExpiredAlertHistories();

        // then
        ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(notificationHistoryRepository).deleteByChgerIdIsNotNullAndCreatedAtBefore(captor.capture());

        LocalDateTime after = LocalDateTime.now().minusWeeks(1);
        assertThat(captor.getValue()).isBetween(before, after);
    }

    @AfterEach
    void tearDown(TestInfo testInfo) {
        System.out.println(testInfo.getDisplayName() + " 경과 시간: " + (System.currentTimeMillis() - startTime) + "ms");
    }
}
