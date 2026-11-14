package kz.nuraskhan.hangelo.model.repository;

import java.util.List;
import java.util.Optional;
import kz.nuraskhan.hangelo.model.entity.ProfilePhoto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfilePhotoRepository extends JpaRepository<ProfilePhoto, Long> {

  /** The photo an address leads to. */
  Optional<ProfilePhoto> findByToken(String token);

  /** The photos of a person, the main one first. */
  List<ProfilePhoto> findByUserAccountIdOrderByPosition(Long userAccountId);
}
