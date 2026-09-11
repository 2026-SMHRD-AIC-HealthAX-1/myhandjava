package com.smhrd.hometraining.common.exception;

import com.smhrd.hometraining.common.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 검증 실패 메시지에 필드명을 영문 그대로 노출하지 않기 위한 한글 라벨. 목록에 없는
    // 필드는 그냥 영문 필드명을 그대로 보여준다(완벽하진 않아도 "": " 조립 자체는 무너지지 않음).
    private static final Map<String, String> FIELD_LABELS = Map.of(
            "loginId", "아이디",
            "password", "비밀번호",
            "email", "이메일",
            "nickname", "닉네임",
            "gender", "성별",
            "title", "제목",
            "body", "내용",
            "message", "메시지"
    );

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException e) {
        return ResponseEntity.status(e.getStatus()).body(ApiResponse.fail(e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        FieldError fe = e.getBindingResult().getFieldError();
        String message;
        if (fe == null) {
            message = "요청 값이 올바르지 않습니다.";
        } else {
            String label = FIELD_LABELS.getOrDefault(fe.getField(), fe.getField());
            message = label + ": " + fe.getDefaultMessage();
        }
        return ResponseEntity.badRequest().body(ApiResponse.fail(message));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuth(AuthenticationException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.fail("인증에 실패했습니다."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnknown(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail("서버 오류가 발생했습니다: " + e.getMessage()));
    }
}
