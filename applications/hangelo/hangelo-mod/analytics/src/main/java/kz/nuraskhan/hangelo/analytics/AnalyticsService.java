package kz.nuraskhan.hangelo.analytics;

import static kz.nuraskhan.hangelo.jooq.Tables.EVENT;
import static kz.nuraskhan.hangelo.jooq.Tables.PAGE_VIEW;
import static kz.nuraskhan.hangelo.jooq.Tables.USER_ACCOUNT;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import kz.nuraskhan.hangelo.analytics.dto.AnalyticsDto;
import kz.nuraskhan.hangelo.web.ApiException;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record3;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Records visits, and counts them for the people allowed to look. */
@Service
public class AnalyticsService {

  private static final int TOP_PAGES = 15;

  private final DSLContext dsl;
  /** The e-mails of those who may see the figures, lower case. */
  private final Set<String> adminEmails;

  public AnalyticsService(DSLContext dsl, @Value("${hangelo.admin.emails:}") String adminEmails) {
    this.dsl = dsl;
    this.adminEmails = Arrays.stream(adminEmails.split(","))
        .map(email -> email.trim().toLowerCase())
        .filter(email -> !email.isEmpty())
        .collect(Collectors.toUnmodifiableSet());
  }

  /**
   * One page opened.
   *
   * @param userId who is signed in; null for a visitor
   */
  @Transactional
  public void visit(String visitorId, String path, Long userId) {
    dsl.insertInto(PAGE_VIEW)
        .set(PAGE_VIEW.VISITOR_ID, visitorId)
        .set(PAGE_VIEW.USER_ACCOUNT_ID, userId)
        .set(PAGE_VIEW.PATH, path)
        .set(PAGE_VIEW.CREATED_AT, Instant.now())
        .execute();
  }

  public boolean isAdmin(Long userId) {
    if (userId == null || adminEmails.isEmpty()) {
      return false;
    }
    String email = dsl.select(USER_ACCOUNT.EMAIL).from(USER_ACCOUNT)
        .where(USER_ACCOUNT.ID.eq(userId))
        .and(USER_ACCOUNT.DEACTIVATED_AT.isNull())
        .fetchOne(USER_ACCOUNT.EMAIL);
    return email != null && adminEmails.contains(email.toLowerCase());
  }

  /** For anyone but an admin the page "does not exist". */
  public void requireAdmin(Long userId) {
    if (!isAdmin(userId)) {
      throw ApiException.notFound("not_found", "Not found");
    }
  }

  /** The figures for the last {@code days} days, today (UTC) included. */
  @Transactional(readOnly = true)
  public AnalyticsDto figures(int days) {
    LocalDate today = LocalDate.now(ZoneOffset.UTC);
    LocalDate first = today.minusDays(days - 1L);
    Instant from = first.atStartOfDay(ZoneOffset.UTC).toInstant();

    Field<Integer> visits = DSL.count();
    Field<Integer> visitors = DSL.countDistinct(PAGE_VIEW.VISITOR_ID);

    var totals = dsl
        .select(visits, visitors, DSL.count(PAGE_VIEW.USER_ACCOUNT_ID))
        .from(PAGE_VIEW)
        .where(PAGE_VIEW.CREATED_AT.ge(from))
        .fetchOne();

    // The UTC day of a visit. Written out in SQL: the day depends on the zone, and it must be
    // the same whatever zone the server and the database session are in.
    Field<LocalDate> day = DSL.field("(({0}) at time zone 'UTC')::date", LocalDate.class, PAGE_VIEW.CREATED_AT);

    Map<LocalDate, Record3<LocalDate, Integer, Integer>> byDay = dsl
        .select(day, visits, visitors)
        .from(PAGE_VIEW)
        .where(PAGE_VIEW.CREATED_AT.ge(from))
        .groupBy(day)
        .fetchMap(day);

    // Every day of the stretch, also the ones nobody came on: a chart with holes lies.
    List<AnalyticsDto.Day> everyDay = new ArrayList<>();
    for (LocalDate date = first; !date.isAfter(today); date = date.plusDays(1)) {
      var row = byDay.get(date);
      everyDay.add(new AnalyticsDto.Day(date, row == null ? 0 : row.value2(), row == null ? 0 : row.value3()));
    }

    List<AnalyticsDto.Page> topPages = dsl
        .select(PAGE_VIEW.PATH, visits, visitors)
        .from(PAGE_VIEW)
        .where(PAGE_VIEW.CREATED_AT.ge(from))
        .groupBy(PAGE_VIEW.PATH)
        .orderBy(visits.desc(), PAGE_VIEW.PATH)
        .limit(TOP_PAGES)
        .fetch(row -> new AnalyticsDto.Page(row.value1(), row.value2(), row.value3()));

    return new AnalyticsDto(
        days,
        totals.value1(),
        totals.value2(),
        totals.value3(),
        dsl.fetchCount(USER_ACCOUNT),
        dsl.fetchCount(USER_ACCOUNT, USER_ACCOUNT.CREATED_AT.ge(from)),
        dsl.fetchCount(EVENT, EVENT.CREATED_AT.ge(from)),
        everyDay,
        topPages);
  }
}
