package ev_charger.be.common.exception;

// 사용 횟수 상한 초과 (챗봇 하루 질문 수 등) -> 429
public class TooManyRequestsException extends RuntimeException {
    public TooManyRequestsException(String message) {
        super(message);
    }
}
