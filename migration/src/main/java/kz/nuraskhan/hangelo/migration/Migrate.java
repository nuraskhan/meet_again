package kz.nuraskhan.hangelo.migration;

import java.util.Arrays;
import java.util.stream.Collectors;
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

    // WHY the url is cleaned: Heroku gives a url with the user and the password in its query, and
    // Flyway writes the url it was given into the log ("Database: jdbc:..."). The first deploy put
    // the database password into the release log this way. The credentials are passed separately.
    String url = withoutCredentials(args[0]);

    Flyway flyway = Flyway.configure()
        .dataSource(url, args[1], args[2])
        .locations("classpath:db/migration")
        .load();

    MigrateResult result = flyway.migrate();

    System.out.println(url + " : " + (result.migrationsExecuted == 0
        ? "already up to date"
        : "applied " + result.migrationsExecuted + ", now at version " + result.targetSchemaVersion));
  }

  /** The url without the query parameters {@code user} and {@code password}; the others stay. */
  static String withoutCredentials(String jdbcUrl) {
    String[] parts = jdbcUrl.split("\\?", 2);
    if (parts.length == 1) {
      return jdbcUrl;
    }

    String query = Arrays.stream(parts[1].split("&"))
        .filter(parameter -> !parameter.startsWith("user=") && !parameter.startsWith("password="))
        .collect(Collectors.joining("&"));

    return query.isEmpty() ? parts[0] : parts[0] + "?" + query;
  }
}
