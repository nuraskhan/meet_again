package kz.nuraskhan.hangelo.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MailProperties.class)
public class MailConfig {

  private static final Logger log = LoggerFactory.getLogger(MailConfig.class);

  /** Resend when there is a key; otherwise letters only go to the log. */
  @Bean
  public EmailSender emailSender(MailProperties properties) {
    if (properties.hasResendApiKey()) {
      log.info("E-mail is sent through Resend, from {}", properties.from());
      return new ResendEmailSender(properties);
    }

    log.warn("HANGELO_RESEND_API_KEY is not set: e-mail is NOT sent, letters are written to the log");
    return new LogEmailSender();
  }
}
