package kz.nuraskhan.meet_again;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The server "magain".
 *
 * <p>It sits in the root package on purpose: components, JPA entities and repositories are found by
 * scanning from here down, so everything in {@code kz.nuraskhan.meet_again.*} that is on the
 * classpath is picked up without listing packages.
 */
@SpringBootApplication
public class MagainApplication {

  public static void main(String[] args) {
    SpringApplication.run(MagainApplication.class, args);
  }
}
