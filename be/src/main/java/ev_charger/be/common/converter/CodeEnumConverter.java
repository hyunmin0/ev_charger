package ev_charger.be.common.converter;
// 코드값(string)과 enum을 상호 변환하는 공통 추상 converter

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;

// AttributeConverter<T, String>
    // T = Java 타입(ChgerType, ChgerStat 등)
    // String = DB 타입
// T는 enum이면서, CodeEnum 인터페이스를 구현해야 함
@Converter(autoApply = true)
public abstract class CodeEnumConverter<T extends Enum<T> & CodeEnum> implements AttributeConverter<T, String> {

    private final Class<T> enumClass;

    // protected: 추상클래스라 직접 생성 불가, 항상 자식 클래스를 통해서만 생성되므로(super) protected면 충분함
    protected CodeEnumConverter(Class<T> enumClass) {
        this.enumClass = enumClass;
    }

    // Java -> DB
    @Override
    public String convertToDatabaseColumn(T attr) {
        return attr.getCode(); // ChgerType.DB_DEMO.getCode() = "01"
    }

    // DB -> Java
    @Override
    public T convertToEntityAttribute(String dbData) {
        // getEnumConstants(): 해당 enum의 모든 값을 배열로 반환
        return Arrays.stream(enumClass.getEnumConstants())
                .filter(e -> e.getCode().equals(dbData)) // "01"인 enum 찾기
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 값입니다."));
    }
}
