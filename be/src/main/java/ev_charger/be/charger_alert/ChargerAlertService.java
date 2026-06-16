package ev_charger.be.charger_alert;

import ev_charger.be.charger_alert.dto.response.UserChargerAlertResponse;
import ev_charger.be.common.enums.YN;
import ev_charger.be.station.Station;
import ev_charger.be.station.StationRepository;
import ev_charger.be.station.charger.Charger;
import ev_charger.be.station.charger.ChargerRepository;
import ev_charger.be.station.charger.enums.ChgerType;
import ev_charger.be.station.dto.response.StationResponse;
import ev_charger.be.station.enums.FloorType;
import ev_charger.be.station.enums.Kind;
import ev_charger.be.station.stationOperator.StationOperator;
import ev_charger.be.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ChargerAlertService {

    private final ChargerRepository  chargerRepository;
    private final ChargerAlertRepository  chargerAlertRepository;
    private final StationRepository stationRepository;

    /**
     * 알림 추가
     * @param user
     * @param statId
     * @param chgerId
     */
    @Transactional
    public void addChargerAlert(User user, String statId, String chgerId) {
        Charger charger = chargerRepository.findByStatIdAndChgerId(statId, chgerId)
                .orElseThrow(() -> new IllegalArgumentException("충전소 또는 충전기가 유효하지 않습니다."));

        if (chargerAlertRepository.existsByUserAndCharger(user, charger)) {
            throw new IllegalArgumentException("이미 등록된 알림입니다.");
        }

        chargerAlertRepository.save(ChargerAlert.builder()
                .user(user)
                .charger(charger)
                .build());
    }

    /**
     * 알림 취소
     * @param user
     * @param statId
     * @param chgerId
     */
    @Transactional
    public void deleteChargerAlert(User user, String statId, String chgerId) {
        Charger charger = chargerRepository.findByStatIdAndChgerId(statId, chgerId)
                .orElseThrow(() -> new IllegalArgumentException("충전소 또는 충전기가 유효하지 않습니다."));

        if (!chargerAlertRepository.existsByUserAndCharger(user, charger)) {
            throw new IllegalArgumentException("등록되지 않은 알림입니다.");
        }

        chargerAlertRepository.deleteByUserAndCharger(user, charger);
    }

    /**
     * 해당 유저의 알림 조회
     * @param user
     * @return List<UserChargerAlertResponse>
     */
    public List<UserChargerAlertResponse> getChargerAlertsByUser(User user) {
        // 충전기 알림을 statId로 매핑
        Map<String, List<ChargerAlert>> alertsByStation = chargerAlertRepository.findByUser(user).stream().collect(Collectors.groupingBy(a -> a.getCharger().getStatId()));

        // 알림 설정된 각 충전소의 모든 충전기를 매핑
        // hasFast(급속여부)를 구하기 위해 알림 설정이 되지 않은 충전기 정보도 필요
        Map<String, List<Charger>> chargersByStation = chargerRepository.findByStatIdInWithStation(alertsByStation.keySet()).stream().collect(Collectors.groupingBy(Charger::getStatId));

        return alertsByStation.keySet().stream()
                .map(statId -> {
                    // 해당 충전소의 모든 충전기 정보
                    List<Charger> stationChargers = chargersByStation.get(statId);

                    // stationChargers의 statId는 모두 동일하므로 첫번쨰 원소에서 get
                    // findById를 하지 않으므로서 추가적인 db 쿼리를 배제
                    Station station = stationChargers.get(0).getStation();

                    // chgerType in (2, 7, 8)을 제회하면 모두 급속
                    boolean hasFast = stationChargers.stream()
                            .anyMatch(c -> c.getChgerType() != ChgerType.AC_SLOW
                            && c.getChgerType() != ChgerType.AC3
                            && c.getChgerType() != ChgerType.DC_COMBO_SLOW);

                    // 알림 설정된 충전기 response
                    List<UserChargerAlertResponse.AlertedCharger> alertedChargers = alertsByStation.get(statId).stream()
                            .map(a -> new UserChargerAlertResponse.AlertedCharger(
                                    a.getCharger().getChgerId(),
                                    a.getCharger().getChgerType(),
                                    a.getCharger().getOutput(),
                                    a.getCharger().getChgerStat()
                            )).toList();

                    return new UserChargerAlertResponse(
                            station.getStatId(),
                            station.getStatNm(),
                            station.getUseTime(),
                            station.getParkingFree() != null ? YN.Y.equals(station.getParkingFree()) : null,
                            station.getLimitYn() != null ? YN.N.equals(station.getLimitYn()) : null, // = openToPubilc
                            station.getKind() != null ? Kind.descriptionOf(station.getKind().getCode()) : null,
                            station.getFloorType() != null ? FloorType.descriptionOf(station.getFloorType().name()) : null,
                            hasFast,
                            station.getStationOperator().getBusiNm(),
                            alertedChargers
                            );
                }).toList();
    }
}
