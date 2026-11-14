package kz.nuraskhan.meet_again;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import kz.nuraskhan.meet_again.model.rnd.RND;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Base of the API tests: the whole server is started on a free port against the test database
 * (magain_test), and the tests talk to it over real HTTP, as the front end will.
 *
 * <p>A test creates everything it needs itself - new users with random e-mails - and depends on
 * nothing already in the database, so the tests can run in any order and the database is never
 * cleaned.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class ApiTestBase {

  private static final HttpClient HTTP = HttpClient.newHttpClient();
  private static final JsonMapper JSON = JsonMapper.builder().build();

  @Value("${local.server.port}")
  private int port;

  /** A registered user and their token. */
  public record TestUser(long id, String token, String email, String password, String displayName) {
  }

  /** @param body the parsed JSON; a "missing node" when the response has no body */
  public record Response(int status, JsonNode body) {
  }

  protected Response get(String path, TestUser as) {
    return send("GET", path, as == null ? null : as.token(), null);
  }

  protected Response post(String path, TestUser as, Object body) {
    return send("POST", path, as == null ? null : as.token(), body);
  }

  protected Response post(String path, TestUser as) {
    return post(path, as, null);
  }

  /** A response whose body is not JSON: a picture. */
  public record RawResponse(int status, java.net.http.HttpHeaders headers, byte[] body) {
  }

  /**
   * Sends a file the way a browser form does: one part named {@code file}.
   *
   * @param fileName    what the uploader claims the file is called
   * @param contentType what the uploader claims the file is
   */
  protected Response upload(String path, TestUser as, String fileName, String contentType,
                            byte[] content) {
    String boundary = "----test" + RND.str(24);

    byte[] head = ("--" + boundary + "\r\n"
        + "Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"\r\n"
        + "Content-Type: " + contentType + "\r\n\r\n").getBytes(java.nio.charset.StandardCharsets.UTF_8);
    byte[] tail = ("\r\n--" + boundary + "--\r\n").getBytes(java.nio.charset.StandardCharsets.UTF_8);

    HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
        .header("Content-Type", "multipart/form-data; boundary=" + boundary)
        .POST(HttpRequest.BodyPublishers.ofByteArrays(List.of(head, content, tail)));
    if (as != null) {
      request.header("Authorization", "Bearer " + as.token());
    }

    try {
      HttpResponse<String> response = HTTP.send(request.build(), HttpResponse.BodyHandlers.ofString());
      String text = response.body();
      JsonNode json = text == null || text.isBlank() ? JSON.missingNode() : JSON.readTree(text);
      return new Response(response.statusCode(), json);
    } catch (java.io.IOException e) {
      throw new java.io.UncheckedIOException(e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(e);
    }
  }

  /** A GET without a token, for something that is not JSON. */
  protected RawResponse getRaw(String path) {
    try {
      HttpResponse<byte[]> response = HTTP.send(
          HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build(),
          HttpResponse.BodyHandlers.ofByteArray());
      return new RawResponse(response.statusCode(), response.headers(), response.body());
    } catch (java.io.IOException e) {
      throw new java.io.UncheckedIOException(e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(e);
    }
  }

  /** Registers a new person with a random e-mail and name. */
  protected TestUser register() {
    String email = RND.email();
    String password = RND.str(12);
    String displayName = "Test " + RND.str(10);

    Response response = post("/auth/register", null,
        Map.of("email", email, "password", password, "displayName", displayName));
    assertThat(response.status()).isEqualTo(200);

    return new TestUser(response.body().get("userId").asLong(),
        response.body().get("token").asString(), email, password, displayName);
  }

  /** A request from {@code a}, accepted by {@code b}. */
  protected void makeFriends(TestUser a, TestUser b) {
    Response request = post("/friend/request", a, Map.of("userId", b.id()));
    assertThat(request.status()).isEqualTo(200);

    Response accept = post("/friend/request/" + request.body().get("requestId").asLong() + "/accept", b);
    assertThat(accept.status()).isEqualTo(200);
  }

  /** The values of one field of every element of a JSON array. */
  protected static List<Long> longs(JsonNode array, String field) {
    return array.valueStream().map(node -> node.get(field).asLong()).toList();
  }

  private Response send(String method, String path, String token, Object body) {
    try {
      HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));

      if (token != null) {
        request.header("Authorization", "Bearer " + token);
      }
      if (body == null) {
        request.method(method, HttpRequest.BodyPublishers.noBody());
      } else {
        request.header("Content-Type", "application/json");
        request.method(method, HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(body)));
      }

      HttpResponse<String> response = HTTP.send(request.build(), HttpResponse.BodyHandlers.ofString());

      String text = response.body();
      JsonNode json = text == null || text.isBlank() ? JSON.missingNode() : JSON.readTree(text);
      return new Response(response.statusCode(), json);
    } catch (java.io.IOException e) {
      throw new java.io.UncheckedIOException(e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(e);
    }
  }
}
