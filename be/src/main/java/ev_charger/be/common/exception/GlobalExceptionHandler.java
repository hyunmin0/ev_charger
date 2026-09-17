package ev_charger.be.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

// 모든 에러 응답을 ErrorResponse { message } 형식으로 통일
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    // 잘못된 요청/유효성 검증 실패 (존재하지 않는 리소스, 중복 등록, 권한 없음 등)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(e.getMessage()));
    }

    // 현재 상태와 충돌하는 요청 (이미 가입된 사용자 등)
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(e.getMessage()));
    }

    // 서버 내부 오류 (손상된 서버 데이터 등) - 원인은 로그로 남기고, 메시지는 서비스에서 사용자용으로 작성
    @ExceptionHandler(InternalServerException.class)
    public ResponseEntity<ErrorResponse> handleInternalServer(InternalServerException e) {
        log.error("서버 내부 오류: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse(e.getMessage()));
    }

    // 컨트롤러 안에서 발생한 시큐리티 예외(@PreAuthorize 등)
    // 따로 처리하지 않으면 아래 Exception 핸들러에 잡혀 500이 되어버림
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse("접근 권한이 없습니다."));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("인증이 필요합니다."));
    }

    // 위에서 처리되지 않은 모든 예외 -> 500 (내부 정보가 노출되지 않도록 고정 메시지)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        log.error("처리되지 않은 예외", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse("일시적인 서버 오류가 발생했습니다."));
    }

    // 스프링 MVC 예외(파라미터 누락, JSON 형식 오류, 타입 불일치, 지원하지 않는 메서드 등)
    // 상태 코드(400, 405, 415 등)는 스프링이 정한 값을 유지하고, 본문만 ErrorResponse로 변환
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        if (statusCode.is5xxServerError()) {
            log.error("스프링 MVC 내부 오류", ex);
            return ResponseEntity.status(statusCode).headers(headers).body(new ErrorResponse("일시적인 서버 오류가 발생했습니다."));
        }
        return ResponseEntity.status(statusCode).headers(headers).body(new ErrorResponse("잘못된 요청입니다."));
    }
}
