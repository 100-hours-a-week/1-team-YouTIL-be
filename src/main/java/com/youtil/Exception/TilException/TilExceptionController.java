package com.youtil.Exception.TilException;

import com.youtil.Common.ApiResponse;
import com.youtil.Common.Enums.ErrorMessageCode;
import com.youtil.Exception.ExceptionResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;

@RestControllerAdvice
@Slf4j
public class TilExceptionController {

    @ExceptionHandler(TilException.TilAIHealthxception.class)
    public ResponseEntity<ExceptionResponse> TilAIUnHealthException(
            TilException.TilAIHealthxception e) {
        ExceptionResponse response = new ExceptionResponse();
        response.setCode(ErrorMessageCode.AI_SEVER_NOT_HEALTH.getCode());
        response.setMessage(e.getMessage());
        return new ResponseEntity<>(response, HttpStatus.SERVICE_UNAVAILABLE);
    }

    @ExceptionHandler(TilException.TilSerializationException.class)
    public ResponseEntity<ExceptionResponse> TilSerializationException(
            TilException.TilSerializationException e) {
        ExceptionResponse response = new ExceptionResponse();
        response.setCode(ErrorMessageCode.TIL_QUEUE_SERIALIZATION_FAILED.getCode());
        response.setMessage(e.getMessage());
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(TilException.TilCreateTimeOutException.class)
    public ResponseEntity<ExceptionResponse> TilCreateTimeOutException(
            TilException.TilCreateTimeOutException e
    ) {
        ExceptionResponse response = new ExceptionResponse();
        response.setCode(ErrorMessageCode.TIL_CREATED_TIMEOUT.getCode());
        response.setMessage(e.getMessage());
        return new ResponseEntity<>(response, HttpStatus.GATEWAY_TIMEOUT);
    }

    @ExceptionHandler(TilException.TilNotFoundException.class)
    public ResponseEntity<ExceptionResponse> TilNotFoundException(
            TilException.TilNotFoundException e
    ) {
        ExceptionResponse response = new ExceptionResponse();
        response.setCode(ErrorMessageCode.TIL_NOT_FOUND.getCode());
        response.setMessage(e.getMessage());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * 인증 실패 (JWT 토큰 없음, 유효하지 않음)
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<String>> handleAuthenticationException(AuthenticationException e) {
        log.warn("인증 실패: {}", e.getMessage());

        ApiResponse<String> response = ApiResponse.<String>builder()
                .success(false)
                .code("401")
                .message("인증이 필요합니다. 토큰을 확인해주세요.")
                .data(null)
                .responseAt(OffsetDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    /**
     * 권한 없음 (인증은 되었지만 해당 리소스에 접근 권한 없음)
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<String>> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("접근 권한 없음: {}", e.getMessage());

        ApiResponse<String> response = ApiResponse.<String>builder()
                .success(false)
                .code("403")
                .message("해당 리소스에 접근할 권한이 없습니다.")
                .data(null)
                .responseAt(OffsetDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    /**
     * ResponseStatusException 처리 (컨트롤러에서 던지는 예외)
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<String>> handleResponseStatusException(ResponseStatusException e) {
        log.warn("응답 상태 예외: {} - {}", e.getStatusCode(), e.getReason());

        String statusCode = String.valueOf(e.getStatusCode().value());

        ApiResponse<String> response = ApiResponse.<String>builder()
                .success(false)
                .code(statusCode)
                .message(e.getReason() != null ? e.getReason() : "요청 처리 중 오류가 발생했습니다.")
                .data(null)
                .responseAt(OffsetDateTime.now())
                .build();

        return ResponseEntity.status(e.getStatusCode()).body(response);
    }

    /**
     * 잘못된 자격 증명
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<String>> handleBadCredentialsException(BadCredentialsException e) {
        log.warn("잘못된 자격 증명: {}", e.getMessage());

        ApiResponse<String> response = ApiResponse.<String>builder()
                .success(false)
                .code("401")
                .message("아이디 또는 비밀번호가 올바르지 않습니다.")
                .data(null)
                .responseAt(OffsetDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    /**
     * 일반적인 런타임 예외
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<String>> handleRuntimeException(RuntimeException e) {
        log.error("런타임 예외 발생: {}", e.getMessage(), e);

        // TIL 관련 메시지인지 확인하고 적절한 상태 코드 설정
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String code = "500";

        if (e.getMessage() != null) {
            if (e.getMessage().contains("찾을 수 없습니다")) {
                status = HttpStatus.NOT_FOUND;
                code = "404";
            } else if (e.getMessage().contains("권한이 없습니다") || e.getMessage().contains("삭제 권한이 없습니다") || e.getMessage().contains("수정 권한이 없습니다")) {
                status = HttpStatus.FORBIDDEN;
                code = "403";
            } else if (e.getMessage().contains("삭제된 TIL입니다")) {
                status = HttpStatus.GONE;
                code = "410";
            } else if (e.getMessage().contains("필수입니다") || e.getMessage().contains("형식이") || e.getMessage().contains("유효하지 않습니다")) {
                status = HttpStatus.BAD_REQUEST;
                code = "400";
            }
        }

        ApiResponse<String> response = ApiResponse.<String>builder()
                .success(false)
                .code(code)
                .message(e.getMessage() != null ? e.getMessage() : "서버 내부 오류가 발생했습니다.")
                .data(null)
                .responseAt(OffsetDateTime.now())
                .build();

        return ResponseEntity.status(status).body(response);
    }

    /**
     * 예상치 못한 모든 예외
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<String>> handleException(Exception e) {
        log.error("예상치 못한 예외 발생: {}", e.getMessage(), e);

        ApiResponse<String> response = ApiResponse.<String>builder()
                .success(false)
                .code("500")
                .message("예상치 못한 오류가 발생했습니다.")
                .data(null)
                .responseAt(OffsetDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}