package kz.nuraskhan.hangelo.calendar;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import kz.nuraskhan.hangelo.calendar.dto.SaveAvailabilityRequest;
import kz.nuraskhan.hangelo.model.entity.Availability;
import kz.nuraskhan.hangelo.model.enums.AvailabilityKind;
import kz.nuraskhan.hangelo.model.repository.AvailabilityRepository;
import kz.nuraskhan.hangelo.web.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Marking days in one's own calendar: busy, free, away in another city. */
@Service
public class AvailabilityService {

  /** The longest stretch one mark may cover. A year away is the most anyone plans. */
  static final long MAX_DAYS = 366;

  /** The most marks one person keeps: a calendar, not a diary. */
  static final long MAX_MARKS = 200;

  private final AvailabilityRepository availabilityRepository;

  public AvailabilityService(AvailabilityRepository availabilityRepository) {
    this.availabilityRepository = availabilityRepository;
  }

  /** @return id of the new mark */
  @Transactional
  public Long add(Long me, SaveAvailabilityRequest request) {
    if (request.toDate().isBefore(request.fromDate())) {
      throw ApiException.badRequest("bad_dates", "The last day cannot be before the first");
    }
    if (ChronoUnit.DAYS.between(request.fromDate(), request.toDate()) >= MAX_DAYS) {
      throw ApiException.badRequest("too_long", "One mark covers a year at most");
    }

    String city = blankToNull(request.city());
    if (request.kind() == AvailabilityKind.TRIP && city == null) {
      throw ApiException.badRequest("no_city", "Say which city you are going to");
    }
    if (availabilityRepository.countByUserAccountIdAndDeletedAtIsNull(me) >= MAX_MARKS) {
      throw ApiException.conflict("too_many", "Your calendar is full. Remove marks you no longer need");
    }

    Availability availability = new Availability();
    availability.userAccountId = me;
    availability.kind = request.kind();
    availability.fromDate = request.fromDate();
    availability.toDate = request.toDate();
    // A city says where a trip goes; for busy or free days there is nowhere to go to.
    availability.city = request.kind() == AvailabilityKind.TRIP ? city : null;
    availability.note = blankToNull(request.note());
    availability.createdAt = Instant.now();

    return availabilityRepository.save(availability).id;
  }

  /** Removes the caller's own mark. Somebody else's "does not exist". The row stays. */
  @Transactional
  public void remove(Long me, Long availabilityId) {
    Availability availability = availabilityRepository.findById(availabilityId).orElse(null);

    if (availability == null || !availability.userAccountId.equals(me) || availability.deletedAt != null) {
      throw ApiException.notFound("availability_not_found", "This mark does not exist");
    }
    availability.deletedAt = Instant.now();
  }

  private static String blankToNull(String text) {
    return text == null || text.isBlank() ? null : text.trim();
  }
}
