package kz.nuraskhan.meet_again.web;

import org.springframework.http.HttpStatus;

/**
 * A refusal the client should understand. Becomes the HTTP status and the body
 * {@code {"code": "...", "message": "..."}} (see {@link ApiExceptionHandler}).
 *
 * <p>{@code code} is for programs: stable, snake_case. {@code message} is for people.
 */
public class ApiException extends RuntimeException {

  public final HttpStatus status;
  public final String code;

  public ApiException(HttpStatus status, String code, String message) {
    super(message);
    this.status = status;
    this.code = code;
  }

  public static ApiException badRequest(String code, String message) {
    return new ApiException(HttpStatus.BAD_REQUEST, code, message);
  }

  public static ApiException unauthorized(String code, String message) {
    return new ApiException(HttpStatus.UNAUTHORIZED, code, message);
  }

  /** Also for "exists, but is not yours": the client must not learn that it exists. */
  public static ApiException notFound(String code, String message) {
    return new ApiException(HttpStatus.NOT_FOUND, code, message);
  }

  /** The request is fine, the current state does not allow it. */
  public static ApiException conflict(String code, String message) {
    return new ApiException(HttpStatus.CONFLICT, code, message);
  }
}
