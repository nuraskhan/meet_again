package kz.nuraskhan.meet_again.magain.auth;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param secret the key tokens are signed with; at least 32 bytes; comes from MAGAIN_JWT_SECRET
 * @param ttl    how long a token is valid
 */
@ConfigurationProperties("magain.jwt")
public record JwtProperties(String secret, Duration ttl) {
}
