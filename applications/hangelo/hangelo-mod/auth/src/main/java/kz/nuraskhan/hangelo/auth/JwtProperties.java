package kz.nuraskhan.hangelo.auth;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param secret the key tokens are signed with; at least 32 bytes; comes from HANGELO_JWT_SECRET
 * @param ttl    how long a token is valid
 */
@ConfigurationProperties("hangelo.jwt")
public record JwtProperties(String secret, Duration ttl) {
}
