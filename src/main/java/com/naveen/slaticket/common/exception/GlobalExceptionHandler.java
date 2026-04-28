package com.naveen.slaticket.common.exception;

import com.naveen.slaticket.common.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {

        List<String> details = exception.getBindingResult()
                .getAllErrors()
                .stream()
                .map(error -> error instanceof FieldError fe
                        ? fe.getField() + ": " + fe.getDefaultMessage()
                        : error.getDefaultMessage())
                .toList();

        ApiErrorResponse response = new ApiErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Validation Failed",
                "Request validation failed",
                request.getRequestURI(),
                details
        );

        return ResponseEntity.badRequest().body(response);





    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiErrorResponse> handleBadRequest(
            BadRequestException ex,
            HttpServletRequest request
    ) {
        ApiErrorResponse response = new ApiErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                ex.getMessage(),
                request.getRequestURI(),
                List.of()
        );

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleBadCredentials(
            BadCredentialsException ex,
            HttpServletRequest request
    ) {
        ApiErrorResponse response = new ApiErrorResponse(
                LocalDateTime.now(),
                HttpStatus.UNAUTHORIZED.value(),
                "Unauthorized",
                "Invalid email or password",
                request.getRequestURI(),
                List.of()
        );

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }


}
