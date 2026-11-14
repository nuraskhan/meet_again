package kz.nuraskhan.meet_again.magain.auth;

import java.time.Instant;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class TokenService {

  private final JwtEncoder jwtEncoder;
  private final JwtProperties properties;

  public TokenService(JwtEncoder jwtEncoder, JwtProperties properties) {
    this.jwtEncoder = jwtEncoder;
    this.properties = properties;
  }

  /** @return a token whose subject is the user id */
  public String issue(Long userAccountId) {
    Instant now = Instant.now();

    JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
    JwtClaimsSet claims = JwtClaimsSet.builder()
        .subject(String.valueOf(userAccountId))
        .issuedAt(now)
        .expiresAt(now.plus(properties.ttl()))
        .build();

    return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
  }
}
