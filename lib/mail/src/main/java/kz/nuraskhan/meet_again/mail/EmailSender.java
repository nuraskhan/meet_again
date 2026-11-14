package kz.nuraskhan.meet_again.mail;

/**
 * The only way a letter leaves the system. Which service delivers it is decided in
 * {@link MailConfig}.
 */
public interface EmailSender {

  /**
   * Hands the letter over for delivery.
   *
   * @throws EmailNotSentException the provider refused it or could not be reached; nothing was sent
   */
  void send(Email email);
}
