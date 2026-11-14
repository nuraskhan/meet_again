package kz.nuraskhan.meet_again.migration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Guard for a mistake already made once: the database password reached the release log on Heroku
 * through the jdbc url. See the WHY comment in {@link Migrate}.
 */
class MigrateTest {

  @Test
  void withoutCredentials__removesUserAndPasswordAndKeepsTheRest() {
    String herokuUrl = "jdbc:postgresql://host:5432/db?password=secret&sslmode=require&user=someone";

    //
    //
    String actual = Migrate.withoutCredentials(herokuUrl);
    //
    //

    assertThat(actual).isEqualTo("jdbc:postgresql://host:5432/db?sslmode=require");
  }

  @Test
  void withoutCredentials__leavesNoQuestionMarkWhenOnlyCredentialsWereThere() {
    //
    //
    String actual = Migrate.withoutCredentials("jdbc:postgresql://host:5432/db?user=someone&password=secret");
    //
    //

    assertThat(actual).isEqualTo("jdbc:postgresql://host:5432/db");
  }

  @Test
  void withoutCredentials__leavesLocalUrlUntouched() {
    //
    //
    String actual = Migrate.withoutCredentials("jdbc:postgresql://localhost:20000/magain");
    //
    //

    assertThat(actual).isEqualTo("jdbc:postgresql://localhost:20000/magain");
  }
}
