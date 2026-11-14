package kz.nuraskhan.hangelo.model.repository;

import kz.nuraskhan.hangelo.model.entity.Place;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaceRepository extends JpaRepository<Place, Long> {

  /** Are the invented demonstration places loaded. */
  boolean existsBySampleTrue();
}
