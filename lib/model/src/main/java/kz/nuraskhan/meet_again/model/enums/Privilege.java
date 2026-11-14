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
  MAX(Privilege.PHOTO_CEILING),
  ;

  /**
   * The most photos any profile may ever hold, whatever its level. The same number is the upper
   * bound of {@code profile_photo.position} in the database (migration V009): raising it means a
   * new migration too.
   */
  public static final int PHOTO_CEILING = 10;

  /** How many photos the profile may hold. Never more than {@link #PHOTO_CEILING}. */
  public final int maxPhotos;

  Privilege(int maxPhotos) {
    // A level that promises more than a profile can hold is a mistake in this file: fail at
    // startup, not when somebody uploads their eleventh photo.
    if (maxPhotos < 0 || maxPhotos > PHOTO_CEILING) {
      throw new IllegalArgumentException(
          "maxPhotos must be between 0 and " + PHOTO_CEILING + ", got " + maxPhotos);
    }
    this.maxPhotos = maxPhotos;
  }
}
