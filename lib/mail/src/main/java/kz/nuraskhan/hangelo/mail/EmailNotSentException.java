package kz.nuraskhan.hangelo.mail;

/** A letter was not handed over to the provider. The caller decides whether that matters. */
public class EmailNotSentException extends RuntimeException {

  public EmailNotSentException(String message, Throwable cause) {
    super(message, cause);
  }
}
