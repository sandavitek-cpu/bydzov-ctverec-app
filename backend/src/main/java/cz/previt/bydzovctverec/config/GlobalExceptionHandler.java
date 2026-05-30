package cz.previt.bydzovctverec.config;

import cz.previt.bydzovctverec.web.ApiResponse;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
    String errorCode = ErrorCodeGenerator.generate();
    List<String> errors = ex.getBindingResult().getFieldErrors().stream()
        .map(FieldError::getDefaultMessage)
        .collect(Collectors.toList());
    log.warn("ErrorCode={} Validation error: {}", errorCode, errors);
    return ResponseEntity.badRequest().body(ApiResponse.error("Validační chyba", errors, errorCode));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
    String errorCode = ErrorCodeGenerator.generate();
    log.warn("ErrorCode={} Bad request: {}", errorCode, ex.getMessage());
    return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage(), errorCode));
  }

  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<ApiResponse<Void>> handleIllegalState(IllegalStateException ex) {
    String errorCode = ErrorCodeGenerator.generate();
    log.warn("ErrorCode={} Bad state: {}", errorCode, ex.getMessage());
    return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage(), errorCode));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Void>> handleGeneral(Exception ex) {
    String errorCode = ErrorCodeGenerator.generate();
    log.error("ErrorCode={} Unhandled exception", errorCode, ex);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ApiResponse.error("Došlo k neočekávané chybě", errorCode));
  }
}
