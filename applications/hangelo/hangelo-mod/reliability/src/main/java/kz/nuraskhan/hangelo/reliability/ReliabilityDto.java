package kz.nuraskhan.hangelo.reliability;

/**
 * How reliable a person is: of what they said they would do, how much they did.
 *
 * @param score  0 to 100 - the share of promises kept; null while there are too few promises to
 *               judge by (see {@link ReliabilityQuery#ENOUGH})
 * @param kept   promises kept: came to an event they said they would come to, or held their own
 * @param broken promises broken: left after saying "coming", did not come, or cancelled their own
 *               event that people were coming to
 */
public record ReliabilityDto(Integer score, int kept, int broken) {

  /** Nothing known yet. */
  public static final ReliabilityDto NONE = new ReliabilityDto(null, 0, 0);
}
