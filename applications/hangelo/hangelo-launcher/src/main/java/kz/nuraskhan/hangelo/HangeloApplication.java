package kz.nuraskhan.hangelo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The server "hangelo".
 *
 * <p>It sits in the root package on purpose: components, JPA entities and repositories are found by
 * scanning from here down, so everything in {@code kz.nuraskhan.hangelo.*} that is on the
 * classpath is picked up without listing packages.
 */
@SpringBootApplication
public class HangeloApplication {

  public static void main(String[] args) {
    SpringApplication.run(HangeloApplication.class, args);
  }
}
