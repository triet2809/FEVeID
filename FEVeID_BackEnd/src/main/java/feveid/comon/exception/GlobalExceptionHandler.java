
package feveid.comon.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;


import java.time.LocalDateTime;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    private ResponseEntity<ApiError> buildError(HttpStatus status, String message, HttpServletRequest request) {
        ApiError apiError = new ApiError(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI()
        );
        return new ResponseEntity<>(apiError, status);
    }

    // 404 không tìm thấy dữ liệu
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> handleNotFoundException(NotFoundException ex, HttpServletRequest request) {
        return buildError(HttpStatus.NOT_FOUND, ex.getMessage(),request);
    }

    //409 dữ liệu bị trùng
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> handleConfictException(ConflictException ex, HttpServletRequest request) {
        return buildError(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    //400 lỗi body không phải JSON hợp lệ
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableException(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return buildError(HttpStatus.BAD_REQUEST, "Dữ liệu không đúng định dạng", request);
    }

    // 400 tham số trên URL sai kiểu (vd: GET /api/accounts/abc trong khi id là số)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        return buildError(HttpStatus.BAD_REQUEST, "Tham số '" + ex.getName() + "' không hợp lệ", request);
    }

    //400 DTO không thỏa điều kiện validation (@Notblank, @Email,....)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().get(0).getDefaultMessage();
        return buildError(HttpStatus.BAD_REQUEST, message, request);
    }

    //401 lỗi đăng nhập
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        String message;
        if(ex instanceof DisabledException) {
            message = "Tài khoản bị vô hiệu hóa, vui lòng liên hệ HR";
        }
        else if(ex instanceof LockedException) {
            message = "Tài khoản bị khóa, vui lòng liên hệ HR";
        }
        else {
            message = "Email hoặc mật khẩu không đúng";
        }
        return buildError(HttpStatus.UNAUTHORIZED, message, request);
     }

     //Lưới an toàn cuối cùng khi lỗi chưa có handler
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleException(Exception ex, HttpServletRequest request) {
        //Lỗi chuẩn của Spring MVC (405,404,...) đã tự mang status đúng -> giữ nguyên
        if (ex instanceof ErrorResponse errorResponse) {
            HttpStatus status = HttpStatus.valueOf(errorResponse.getStatusCode().value());
            return buildError(status,status.getReasonPhrase(), request);
        }
        //lỗi thật sự bất ngờ: ghi đầy đủ vào log cho dev, trả message chung cho client
        log.error("Lỗi không mong đợi tại {}",request.getRequestURI(), ex);
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR,"Đã có lỗi xảy ra, vui lòng thử lại sau", request);
    }

}