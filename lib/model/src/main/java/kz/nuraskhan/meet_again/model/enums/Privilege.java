package kz.nuraskhan.meet_again.model.enums;

/**
 * The privilege level of a person: the base of the future subscription plans. Everyone starts at
 * {@link #STANDARD}; a person cannot change their own level.
 *
 * <p>What a level allows is written here, next to the level, and nowhere else: code asks the level
 * ("how many photos may this person have?") and never compares levels by name. A new allowance is
 * a new field here.
 *
 * <p>A new level is added here AND to the check constraint of {@code profile.privilege} (a new
 * migration).
 */
public enum Privilege {
  STANDARD(3),
  PRO(6),
  MAX(9),
  ;

  /** How many photos the profile may hold. */
  public final int maxPhotos;

  Privilege(int maxPhotos) {
    this.maxPhotos = maxPhotos;
  }
}
