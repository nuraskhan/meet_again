package kz.nuraskhan.meet_again.web;

import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class ApiExceptionHandler {

  @ExceptionHandler(ApiException.class)
  public ResponseEntity<ErrorResponse> onApiException(ApiException e) {
    return ResponseEntity.status(e.status).body(new ErrorResponse(e.code, e.getMessage()));
  }

  /**
   * An upload larger than the server accepts (spring.servlet.multipart.* in application.yaml). It is
   * stopped before any controller runs, so the answer is given here.
   */
  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public ResponseEntity<ErrorResponse> onTooLargeUpload(MaxUploadSizeExceededException e) {
    return ResponseEntity.status(HttpStatus.valueOf(413))
        .body(new ErrorResponse("file_too_large", "The file is too large"));
  }

  /** A request body that failed its validation annotations. */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> onInvalidBody(MethodArgumentNotValidException e) {
    String message = e.getBindingResult().getFieldErrors().stream()
        .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
        .sorted()
        .collect(Collectors.joining("; "));

    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(new ErrorResponse("invalid_request", message));
  }
}
