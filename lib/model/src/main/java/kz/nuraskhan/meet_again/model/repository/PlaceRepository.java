package kz.nuraskhan.meet_again.model.repository;

import kz.nuraskhan.meet_again.model.entity.Place;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaceRepository extends JpaRepository<Place, Long> {

  /** Are the invented demonstration places loaded. */
  boolean existsBySampleTrue();
}
