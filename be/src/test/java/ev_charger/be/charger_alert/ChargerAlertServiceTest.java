package ev_charger.be.charger_alert;

import ev_charger.be.charger_alert.dto.request.ChargerStatusRequest;
import ev_charger.be.charger_alert.dto.response.UserChargerAlertResponse;
import ev_charger.be.common.enums.YN;
import ev_charger.be.config.FcmService;
import ev_charger.be.notification.NotificationHistory;
import ev_charger.be.notification.NotificationHistoryService;
import ev_charger.be.station.Station;
import ev_charger.be.station.charger.Charger;
import ev_charger.be.station.charger.ChargerRepository;
import ev_charger.be.station.charger.enums.ChgerStat;
import ev_charger.be.station.charger.enums.ChgerType;
import ev_charger.be.station.enums.FloorType;
import ev_charger.be.station.enums.Kind;
import ev_charger.be.station.stationOperator.StationOperator;
import ev_charger.be.user.User;
import ev_charger.be.user.enums.Provider;
import ev_charger.be.user.fcmToken.FcmToken;
import ev_charger.be.user.fcmToken.FcmTokenRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChargerAlertServiceTest {

    @Mock
    private ChargerRepository chargerRepository;
    @Mock
    private ChargerAlertRepository chargerAlertRepository;
    @Mock
    private FcmTokenRepository fcmTokenRepository;
    @Mock
    private FcmService fcmService;
    @Mock
    private NotificationHistoryService notificationHistoryService;

    private ChargerAlertService chargerAlertService;

    private User user;

    private long startTime;

    @BeforeEach
    void setUp() {
        startTime = System.currentTimeMillis();
        chargerAlertService = new ChargerAlertService(
                chargerRepository,
                chargerAlertRepository,
                fcmTokenRepository,
                fcmService,
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
    void 알림_추가_성공() {
        // given
        String statId = "ST1";
        String chgerId = "01";
        Charger charger = mock(Charger.class);

        given(chargerRepository.findByStatIdAndChgerId(statId, chgerId)).willReturn(Optional.of(charger));
        given(chargerAlertRepository.existsByUserAndCharger(user, charger)).willReturn(false);

        // when
        chargerAlertService.addChargerAlert(user, statId, chgerId);

        // then
        ArgumentCaptor<ChargerAlert> captor = ArgumentCaptor.forClass(ChargerAlert.class);
        verify(chargerAlertRepository).save(captor.capture());

        ChargerAlert saved = captor.getValue();
        assertThat(saved.getUser()).isEqualTo(user);
        assertThat(saved.getCharger()).isEqualTo(charger);
    }

    @Test
    void 충전기가_유효하지_않으면_추가시_예외_발생() {
        // given
        String statId = "ST1";
        String chgerId = "01";

        given(chargerRepository.findByStatIdAndChgerId(statId, chgerId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> chargerAlertService.addChargerAlert(user, statId, chgerId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("충전소 또는 충전기가 유효하지 않습니다.");

        verify(chargerAlertRepository, never()).save(any());
    }

    @Test
    void 이미_등록된_알림이면_추가시_예외_발생() {
        // given
        String statId = "ST1";
        String chgerId = "01";
        Charger charger = mock(Charger.class);

        given(chargerRepository.findByStatIdAndChgerId(statId, chgerId)).willReturn(Optional.of(charger));
        given(chargerAlertRepository.existsByUserAndCharger(user, charger)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> chargerAlertService.addChargerAlert(user, statId, chgerId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이미 등록된 알림입니다.");

        verify(chargerAlertRepository, never()).save(any());
    }

    @Test
    void 알림_삭제_성공() {
        // given
        String statId = "ST1";
        String chgerId = "01";
        Charger charger = mock(Charger.class);

        given(chargerRepository.findByStatIdAndChgerId(statId, chgerId)).willReturn(Optional.of(charger));
        given(chargerAlertRepository.existsByUserAndCharger(user, charger)).willReturn(true);

        // when
        chargerAlertService.deleteChargerAlert(user, statId, chgerId);

        // then
        verify(chargerAlertRepository).deleteByUserAndCharger(user, charger);
    }

    @Test
    void 충전기가_유효하지_않으면_삭제시_예외_발생() {
        // given
        String statId = "ST1";
        String chgerId = "01";

        given(chargerRepository.findByStatIdAndChgerId(statId, chgerId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> chargerAlertService.deleteChargerAlert(user, statId, chgerId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("충전소 또는 충전기가 유효하지 않습니다.");

        verify(chargerAlertRepository, never()).deleteByUserAndCharger(any(), any());
    }

    @Test
    void 등록되지_않은_알림이면_삭제시_예외_발생() {
        // given
        String statId = "ST1";
        String chgerId = "01";
        Charger charger = mock(Charger.class);

        given(chargerRepository.findByStatIdAndChgerId(statId, chgerId)).willReturn(Optional.of(charger));
        given(chargerAlertRepository.existsByUserAndCharger(user, charger)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> chargerAlertService.deleteChargerAlert(user, statId, chgerId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("등록되지 않은 알림입니다.");

        verify(chargerAlertRepository, never()).deleteByUserAndCharger(any(), any());
    }

    @Test
    void 유저_알림_목록_조회_성공() {
        // given
        StationOperator operator = StationOperator.builder()
                .busiId("01")
                .busiNm("테스트운영사")
                .busiCall("02-1234-5678")
                .build();

        Station station = Station.builder()
                .statId("ST1")
                .statNm("테스트충전소")
                .useTime("24시간")
                .stationOperator(operator)
                .zcode("11")
                .kind(Kind.PUBLIC)
                .parkingFree(YN.Y)
                .limitYn(YN.N) // openToPublic = true
                .floorType(FloorType.F)
                .build();

        Charger charger = mock(Charger.class);
        given(charger.getStatId()).willReturn("ST1");
        given(charger.getChgerId()).willReturn("01");
        given(charger.getStation()).willReturn(station);
        given(charger.getChgerType()).willReturn(ChgerType.DC_COMBO); // 급속
        given(charger.getChgerStat()).willReturn(ChgerStat.WAITING);
        given(charger.getOutput()).willReturn("50kW");

        ChargerAlert alert = ChargerAlert.builder()
                .user(user)
                .charger(charger)
                .build();

        given(chargerAlertRepository.findByUser(user)).willReturn(List.of(alert));
        given(chargerRepository.findByStatIdInWithStation(Set.of("ST1"))).willReturn(List.of(charger));

        // when
        List<UserChargerAlertResponse> responses = chargerAlertService.getChargerAlertsByUser(user);

        // then
        assertThat(responses).hasSize(1);
        UserChargerAlertResponse response = responses.get(0);
        assertThat(response.statId()).isEqualTo("ST1");
        assertThat(response.statNm()).isEqualTo("테스트충전소");
        assertThat(response.useTime()).isEqualTo("24시간");
        assertThat(response.parkingFree()).isTrue();
        assertThat(response.openToPublic()).isTrue();
        assertThat(response.kind()).isEqualTo("공공시설");
        assertThat(response.floorType()).isEqualTo("지상");
        assertThat(response.hasFast()).isTrue();
        assertThat(response.busiNm()).isEqualTo("테스트운영사");
        assertThat(response.alertedChargers()).hasSize(1);
        assertThat(response.alertedChargers().get(0).chgerId()).isEqualTo("01");
        assertThat(response.alertedChargers().get(0).chgerType()).isEqualTo(ChgerType.DC_COMBO);
        assertThat(response.alertedChargers().get(0).output()).isEqualTo("50kW");
        assertThat(response.alertedChargers().get(0).chgerStat()).isEqualTo(ChgerStat.WAITING);
    }

    @Test
    void 알림_충전소에_급속_충전기가_없으면_hasFast는_false() {
        // given
        StationOperator operator = StationOperator.builder()
                .busiId("01")
                .busiNm("테스트운영사")
                .busiCall("02-1234-5678")
                .build();

        Station station = Station.builder()
                .statId("ST1")
                .statNm("테스트충전소")
                .stationOperator(operator)
                .build();

        // 알림 설정한 충전기: AC완속
        Charger alertedCharger = mock(Charger.class);
        given(alertedCharger.getStatId()).willReturn("ST1");
        given(alertedCharger.getChgerId()).willReturn("01");
        given(alertedCharger.getStation()).willReturn(station);
        given(alertedCharger.getChgerType()).willReturn(ChgerType.AC_SLOW);
        given(alertedCharger.getChgerStat()).willReturn(ChgerStat.CHARGING);
        given(alertedCharger.getOutput()).willReturn("7kW");

        // 같은 충전소의 알림 설정하지 않은 충전기: AC3상, DC콤보(완속) -> 모두 완속
        Charger ac3Charger = mock(Charger.class);
        given(ac3Charger.getStatId()).willReturn("ST1");
        given(ac3Charger.getChgerType()).willReturn(ChgerType.AC3);

        Charger dcComboSlowCharger = mock(Charger.class);
        given(dcComboSlowCharger.getStatId()).willReturn("ST1");
        given(dcComboSlowCharger.getChgerType()).willReturn(ChgerType.DC_COMBO_SLOW);

        ChargerAlert alert = ChargerAlert.builder()
                .user(user)
                .charger(alertedCharger)
                .build();

        given(chargerAlertRepository.findByUser(user)).willReturn(List.of(alert));
        given(chargerRepository.findByStatIdInWithStation(Set.of("ST1")))
                .willReturn(List.of(alertedCharger, ac3Charger, dcComboSlowCharger));

        // when
        List<UserChargerAlertResponse> responses = chargerAlertService.getChargerAlertsByUser(user);

        // then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).hasFast()).isFalse();
        assertThat(responses.get(0).alertedChargers()).hasSize(1);
    }

    @Test
    void 충전소_부가정보가_null이면_응답도_null() {
        // given
        StationOperator operator = StationOperator.builder()
                .busiId("01")
                .busiNm("테스트운영사")
                .busiCall("02-1234-5678")
                .build();

        // parkingFree, limitYn, kind, floorType 미설정
        Station station = Station.builder()
                .statId("ST1")
                .statNm("테스트충전소")
                .stationOperator(operator)
                .build();

        Charger charger = mock(Charger.class);
        given(charger.getStatId()).willReturn("ST1");
        given(charger.getChgerId()).willReturn("01");
        given(charger.getStation()).willReturn(station);
        given(charger.getChgerType()).willReturn(ChgerType.DC_COMBO);
        given(charger.getChgerStat()).willReturn(ChgerStat.WAITING);
        given(charger.getOutput()).willReturn("50kW");

        ChargerAlert alert = ChargerAlert.builder()
                .user(user)
                .charger(charger)
                .build();

        given(chargerAlertRepository.findByUser(user)).willReturn(List.of(alert));
        given(chargerRepository.findByStatIdInWithStation(Set.of("ST1"))).willReturn(List.of(charger));

        // when
        List<UserChargerAlertResponse> responses = chargerAlertService.getChargerAlertsByUser(user);

        // then
        UserChargerAlertResponse response = responses.get(0);
        assertThat(response.parkingFree()).isNull();
        assertThat(response.openToPublic()).isNull();
        assertThat(response.kind()).isNull();
        assertThat(response.floorType()).isNull();
    }

    @Test
    void 대기중이던_충전기_알림_발송_성공() {
        // given
        String statId = "ST1";
        String chgerId = "01";
        ChargerStatusRequest request = new ChargerStatusRequest(statId, chgerId);

        Charger charger = mock(Charger.class);
        ChargerAlert alert = ChargerAlert.builder()
                .user(user)
                .charger(charger)
                .build();

        NotificationHistory history = mock(NotificationHistory.class);
        given(history.getId()).willReturn(100L);

        FcmToken fcmToken = FcmToken.builder()
                .user(user)
                .token("token-1")
                .build();

        given(chargerAlertRepository.findByCharger_StatIdAndCharger_ChgerId(statId, chgerId)).willReturn(List.of(alert));
        given(notificationHistoryService.save(alert)).willReturn(history);
        given(fcmTokenRepository.findByUser(user)).willReturn(List.of(fcmToken));

        // when
        chargerAlertService.notifyWaitingChargers(List.of(request));

        // then
        verify(fcmService).send("token-1", "충전기 사용 가능", "알림 설정한 충전기가 사용 가능합니다.", 100L);
        verify(chargerAlertRepository).delete(alert);
    }

    @Test
    void 알림_설정된_충전기가_없으면_발송하지_않음() {
        // given
        ChargerStatusRequest request = new ChargerStatusRequest("ST1", "01");

        given(chargerAlertRepository.findByCharger_StatIdAndCharger_ChgerId("ST1", "01")).willReturn(List.of());

        // when
        chargerAlertService.notifyWaitingChargers(List.of(request));

        // then
        verify(notificationHistoryService, never()).save(any());
        verify(fcmService, never()).send(any(), any(), any(), any());
        verify(chargerAlertRepository, never()).delete(any());
    }

    @Test
    void FCM_토큰이_여러_개면_모두_발송() {
        // given
        String statId = "ST1";
        String chgerId = "01";
        ChargerStatusRequest request = new ChargerStatusRequest(statId, chgerId);

        Charger charger = mock(Charger.class);
        ChargerAlert alert = ChargerAlert.builder()
                .user(user)
                .charger(charger)
                .build();

        NotificationHistory history = mock(NotificationHistory.class);
        given(history.getId()).willReturn(100L);

        FcmToken phoneToken = FcmToken.builder()
                .user(user)
                .token("token-phone")
                .build();
        FcmToken tabletToken = FcmToken.builder()
                .user(user)
                .token("token-tablet")
                .build();

        given(chargerAlertRepository.findByCharger_StatIdAndCharger_ChgerId(statId, chgerId)).willReturn(List.of(alert));
        given(notificationHistoryService.save(alert)).willReturn(history);
        given(fcmTokenRepository.findByUser(user)).willReturn(List.of(phoneToken, tabletToken));

        // when
        chargerAlertService.notifyWaitingChargers(List.of(request));

        // then
        verify(fcmService).send("token-phone", "충전기 사용 가능", "알림 설정한 충전기가 사용 가능합니다.", 100L);
        verify(fcmService).send("token-tablet", "충전기 사용 가능", "알림 설정한 충전기가 사용 가능합니다.", 100L);
        verify(chargerAlertRepository).delete(alert);
    }

    @Test
    void FCM_토큰이_없어도_알림_기록_저장과_알림_삭제는_진행() {
        // given
        String statId = "ST1";
        String chgerId = "01";
        ChargerStatusRequest request = new ChargerStatusRequest(statId, chgerId);

        Charger charger = mock(Charger.class);
        ChargerAlert alert = ChargerAlert.builder()
                .user(user)
                .charger(charger)
                .build();

        NotificationHistory history = mock(NotificationHistory.class);

        given(chargerAlertRepository.findByCharger_StatIdAndCharger_ChgerId(statId, chgerId)).willReturn(List.of(alert));
        given(notificationHistoryService.save(alert)).willReturn(history);
        given(fcmTokenRepository.findByUser(user)).willReturn(List.of());

        // when
        chargerAlertService.notifyWaitingChargers(List.of(request));

        // then
        verify(notificationHistoryService).save(alert);
        verify(fcmService, never()).send(any(), any(), any(), any());
        verify(chargerAlertRepository).delete(alert);
    }

    @AfterEach
    void tearDown(TestInfo testInfo) {
        System.out.println(testInfo.getDisplayName() + " 경과 시간: " + (System.currentTimeMillis() - startTime) + "ms");
    }
}
