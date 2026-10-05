package ev_charger.be.station.charger.enums;
// 충전기 상태 enum

import ev_charger.be.common.converter.CodeEnum;
import lombok.Getter;

@Getter
public enum ChgerStat implements CodeEnum {
    UNKNOWN("0"), // 알수없음
    COMM_ERROR("1"), // 통신이상
    WAITING("2"), // 충전대기
    CHARGING("3"), // 충전중
    SUSPENDED("4"), // 운영중지
    INSPECTION("5"), // 점검중
    RESERVED("6"), // 예약중
    UNCONFIRMED("9"); // 상태미확인

    private final String code;


    ChgerStat(String code) {
        this.code = code;
    }
}
