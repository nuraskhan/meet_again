package kz.nuraskhan.meet_again.mail;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param resendApiKey the key of the Resend account (MAGAIN_RESEND_API_KEY). Empty = letters are
 *                     not sent, only written to the log: that is how it works on a developer's
 *                     machine
 * @param from         who the letters are from, e.g. {@code Meet Again <hello@example.com>}
 *                     (MAGAIN_MAIL_FROM). The domain must be verified in Resend
 * @param publicUrl    the address of the site as people open it, without a slash at the end
 *                     (MAGAIN_PUBLIC_URL). Links in letters start with it
 */
@ConfigurationProperties("magain.mail")
public record MailProperties(String resendApiKey, String from, String publicUrl) {

  public boolean hasResendApiKey() {
    return resendApiKey != null && !resendApiKey.isBlank();
  }

  /** A full address on the site for a path that starts with a slash. */
  public String link(String path) {
    String base = publicUrl == null ? "" : publicUrl.replaceAll("/+$", "");
    return base + path;
  }
}
