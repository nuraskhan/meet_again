package kz.nuraskhan.hangelo.mail;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Sends through Resend: one HTTP call per letter, {@code POST https://api.resend.com/emails}.
 * Reference: https://resend.com/docs/api-reference/emails/send-email
 */
class ResendEmailSender implements EmailSender {

  private static final String API_URL = "https://api.resend.com";

  private final RestClient http;
  private final String from;

  ResendEmailSender(MailProperties properties) {
    // Without timeouts a slow provider would hold a request of ours for as long as it likes.
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(Duration.ofSeconds(5));
    requestFactory.setReadTimeout(Duration.ofSeconds(15));

    this.http = RestClient.builder()
        .baseUrl(API_URL)
        .requestFactory(requestFactory)
        .defaultHeader("Authorization", "Bearer " + properties.resendApiKey())
        .build();
    this.from = properties.from();
  }

  @Override
  public void send(Email email) {
    Map<String, Object> body = Map.of(
        "from", from,
        "to", List.of(email.to()),
        "subject", email.subject(),
        "html", email.html(),
        "text", email.text());

    try {
      http.post()
          .uri("/emails")
          .contentType(MediaType.APPLICATION_JSON)
          .body(body)
          .retrieve()
          .toBodilessEntity();
    } catch (RestClientException e) {
      // The message of the exception holds the provider's answer, never our key.
      throw new EmailNotSentException("Resend did not accept the letter: " + e.getMessage(), e);
    }
  }
}
