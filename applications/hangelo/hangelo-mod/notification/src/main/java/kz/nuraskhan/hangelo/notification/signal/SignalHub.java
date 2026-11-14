package kz.nuraskhan.hangelo.notification.signal;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Pushes signals from the server to the pages people have open.
 *
 * <p>A page opens one long request ({@code GET /signal/stream}) and keeps it; the server writes a
 * line into it whenever there is something for that person (Server-Sent Events). One direction
 * only - server to page; everything the page wants to say it says with ordinary requests.
 *
 * <p>A signal is a hint, not the record: "there is a new notification", "a message came". Losing
 * one loses nothing - whatever it was about is in the database, and the page reads it on its next
 * load. So a signal is sent once, to whoever is connected, and never stored or retried.
 *
 * <p>WHY the connections are kept in the memory of this process, and what that costs: it is the
 * simplest thing that works on one server. With two servers a person connected to one would not get
 * what the other sends. When a second server appears, the sending goes through something both
 * hear - a broker, or PostgreSQL's LISTEN/NOTIFY - and only {@link #send} changes; nothing else
 * knows how a signal travels.
 */
@Component
public class SignalHub implements SmartLifecycle {

  /**
   * How long one stream lives before the server closes it and the page opens a new one. Renewing
   * now and then drops connections of pages long gone.
   */
  private static final long STREAM_LIFETIME_MS = 30 * 60 * 1000L;

  /** The most streams one person keeps: a tab each. Opening one more closes the oldest. */
  private static final int MAX_STREAMS_PER_PERSON = 5;

  private static final Logger log = LoggerFactory.getLogger(SignalHub.class);

  private final Map<Long, List<SseEmitter>> streamsByUser = new ConcurrentHashMap<>();

  private volatile boolean running;

  /** A page of this person starts listening. */
  public SseEmitter connect(Long userId) {
    SseEmitter stream = new SseEmitter(STREAM_LIFETIME_MS);

    if (!running) {
      // The server is stopping: the stream ends at once, and the page tries again later.
      stream.complete();
      return stream;
    }

    List<SseEmitter> streams = streamsByUser.computeIfAbsent(userId, id -> new CopyOnWriteArrayList<>());
    streams.add(stream);
    while (streams.size() > MAX_STREAMS_PER_PERSON) {
      streams.remove(0).complete();
    }

    Runnable forget = () -> forget(userId, stream);
    stream.onCompletion(forget);
    stream.onTimeout(forget);
    stream.onError(error -> forget.run());

    // Something at once: the page learns the stream is open, and proxies on the way start passing
    // it through instead of waiting for a first chunk.
    write(userId, stream, SseEmitter.event().name("hello").data("{}", MediaType.APPLICATION_JSON));

    return stream;
  }

  /**
   * Sends a signal to every open page of the person. Does nothing if they have none.
   *
   * <p>Called inside a transaction, the signal goes out only after the transaction is committed:
   * a page that reacts by reading the database must find there what the signal speaks of, and a
   * transaction that is rolled back must signal nothing.
   *
   * @param type    what kind of signal it is; the page chooses its reaction by it
   * @param payload the content; sent as JSON
   */
  public void send(Long userId, String type, Object payload) {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
        @Override
        public void afterCommit() {
          sendNow(userId, type, payload);
        }
      });
    } else {
      sendNow(userId, type, payload);
    }
  }

  private void sendNow(Long userId, String type, Object payload) {
    List<SseEmitter> streams = streamsByUser.get(userId);
    if (streams == null) {
      return;
    }
    for (SseEmitter stream : streams) {
      write(userId, stream, SseEmitter.event().name(type).data(payload, MediaType.APPLICATION_JSON));
    }
  }

  /**
   * A line of nothing to every stream, every twenty seconds. Routers on the way (Heroku's cuts a
   * connection silent for 55 seconds) see the stream alive, and a stream whose page is gone fails
   * here and is forgotten.
   */
  @Scheduled(fixedRate = 20_000)
  void keepAlive() {
    streamsByUser.forEach((userId, streams) -> {
      for (SseEmitter stream : streams) {
        write(userId, stream, SseEmitter.event().comment("ping"));
      }
    });
  }

  // WHY the hub takes part in the shutdown of the server: a stream is a request that never ends
  // by itself, and the server, stopping, waits for the requests in flight - thirty seconds, for
  // every restart and every deploy, and then cuts them anyway. So the hub ends its streams first.
  // The phase is the highest there is: stopping goes from the highest phase down, and the wait for
  // requests sits lower. A page whose stream was ended opens a new one - at the server that
  // comes up.

  @Override
  public void start() {
    running = true;
  }

  @Override
  public void stop() {
    running = false;
    streamsByUser.forEach((userId, streams) -> streams.forEach(SseEmitter::complete));
    streamsByUser.clear();
  }

  @Override
  public boolean isRunning() {
    return running;
  }

  @Override
  public int getPhase() {
    return Integer.MAX_VALUE;
  }

  /** How many streams are open: for tests and for a look from outside. */
  public int openStreams(Long userId) {
    List<SseEmitter> streams = streamsByUser.get(userId);
    return streams == null ? 0 : streams.size();
  }

  private void write(Long userId, SseEmitter stream, SseEmitter.SseEventBuilder event) {
    try {
      stream.send(event);
    } catch (IOException | IllegalStateException e) {
      // The page closed, or the stream has ended: normal, not worth more than a line.
      log.debug("A signal stream of user {} is gone: {}", userId, e.getMessage());
      forget(userId, stream);
    }
  }

  private void forget(Long userId, SseEmitter stream) {
    streamsByUser.computeIfPresent(userId, (id, streams) -> {
      streams.remove(stream);
      return streams.isEmpty() ? null : streams;
    });
  }
}
