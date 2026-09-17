package ev_charger.be.common.exception;

// 클라이언트가 해결할 수 없는 서버 내부 오류 (손상된 서버 데이터 등) -> 500
public class InternalServerException extends RuntimeException {
    public InternalServerException(String message, Throwable cause) {
        super(message, cause);
    }
}
