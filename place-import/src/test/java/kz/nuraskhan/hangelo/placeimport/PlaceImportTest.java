package kz.nuraskhan.hangelo.placeimport;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PlaceImportTest {

  /**
   * Guards the rule "a password never reaches a log" for this tool: Heroku puts the user and the
   * password into the address it gives, and a driver may print the address it was given.
   */
  @Test
  void withoutCredentials__takesTheUserAndThePasswordOutOfTheAddress_andKeepsTheRest() {
    //
    //
    String heroku = PlaceImport.withoutCredentials(
        "jdbc:postgresql://db.example:5432/app?user=u1&password=s3cr3t&sslmode=require");
    String passwordLast = PlaceImport.withoutCredentials(
        "jdbc:postgresql://db.example:5432/app?sslmode=require&password=s3cr3t");
    String onlyCredentials = PlaceImport.withoutCredentials(
        "jdbc:postgresql://db.example:5432/app?password=s3cr3t&user=u1");
    String plain = PlaceImport.withoutCredentials("jdbc:postgresql://localhost:20000/hangelo");
    //
    //

    assertThat(heroku).isEqualTo("jdbc:postgresql://db.example:5432/app?sslmode=require");
    assertThat(passwordLast).isEqualTo("jdbc:postgresql://db.example:5432/app?sslmode=require");
    assertThat(onlyCredentials).isEqualTo("jdbc:postgresql://db.example:5432/app");
    assertThat(plain).isEqualTo("jdbc:postgresql://localhost:20000/hangelo");
  }
}
