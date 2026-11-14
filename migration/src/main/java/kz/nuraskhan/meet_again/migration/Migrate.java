package kz.nuraskhan.meet_again.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;

/**
 * Applies the migrations from {@code db/migration} to one database.
 *
 * <p>Arguments: jdbc url, user, password.
 */
public class Migrate {

  public static void main(String[] args) {
    if (args.length != 3) {
      throw new IllegalArgumentException("Expected: <jdbc url> <user> <password>");
    }

    Flyway flyway = Flyway.configure()
        .dataSource(args[0], args[1], args[2])
        .locations("classpath:db/migration")
        .load();

    MigrateResult result = flyway.migrate();

    // Never print the url as given: on Heroku it carries the user and the password in its query.
    String urlWithoutQuery = args[0].split("\\?", 2)[0];

    System.out.println(urlWithoutQuery + " : " + (result.migrationsExecuted == 0
        ? "already up to date"
        : "applied " + result.migrationsExecuted + ", now at version " + result.targetSchemaVersion));
  }
}
