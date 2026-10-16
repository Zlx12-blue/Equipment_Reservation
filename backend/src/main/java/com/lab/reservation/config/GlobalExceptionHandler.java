package com.lab.reservation.config;

import com.lab.reservation.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 权限不足(@PreAuthorize 拦截)返回标准 403,避免被兜底成"服务器错误" */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> denied(AccessDeniedException e) {
        return ResponseEntity.status(403).body(ApiResponse.error("无权限访问"));
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ApiResponse<Void> badRequest(RuntimeException e) {
        return ApiResponse.error(e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Void> invalid(MethodArgumentNotValidException e) {
        var error = e.getBindingResult().getFieldError();
        return ApiResponse.error(error == null ? "参数不合法" : error.getField() + " " + error.getDefaultMessage());
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> serverError(Exception e) {
        return ApiResponse.error("服务器错误:" + e.getMessage());
    }
}
