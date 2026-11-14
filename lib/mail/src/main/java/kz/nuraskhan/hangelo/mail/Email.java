package kz.nuraskhan.hangelo.mail;

/**
 * One letter to one person.
 *
 * @param to      the address
 * @param subject one line
 * @param html    the body as HTML; everything that came from a user must already be escaped
 *                (see {@link Letter})
 * @param text    the same body as plain text, for mail programs that show no HTML
 */
public record Email(String to, String subject, String html, String text) {
}
