package kz.nuraskhan.meet_again.magain.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

class GeoTest {

  @Test
  void distanceMi__isZeroForTheSamePoint() {
    //
    //
    double actual = Geo.distanceMi(40.7128, -74.0060, 40.7128, -74.0060);
    //
    //

    assertThat(actual).isZero();
  }

  @Test
  void distanceMi__oneDegreeOfLatitude_isAboutSixtyNineMiles() {
    //
    //
    double actual = Geo.distanceMi(40.0, -74.0, 41.0, -74.0);
    //
    //

    assertThat(actual).isCloseTo(69.09, within(0.05));
  }

  @Test
  void distanceMi__oneDegreeOfLongitude_shrinksWithLatitude() {
    //
    //
    double atTheEquator = Geo.distanceMi(0.0, 10.0, 0.0, 11.0);
    double atSixtyNorth = Geo.distanceMi(60.0, 10.0, 60.0, 11.0);
    //
    //

    assertThat(atTheEquator).isCloseTo(69.09, within(0.05));
    assertThat(atSixtyNorth).isCloseTo(34.55, within(0.05)); // half: cos 60 = 0.5
  }

  @Test
  void distanceMi__newYorkToLosAngeles_isAboutTwentyFourHundredFiftyMiles() {
    //
    //
    double actual = Geo.distanceMi(40.7128, -74.0060, 34.0522, -118.2437);
    //
    //

    assertThat(actual).isCloseTo(2446, within(5.0));
  }

  @Test
  void distanceMi__doesNotDependOnTheDirection() {
    //
    //
    double there = Geo.distanceMi(40.7128, -74.0060, 34.0522, -118.2437);
    double back = Geo.distanceMi(34.0522, -118.2437, 40.7128, -74.0060);
    //
    //

    assertThat(there).isCloseTo(back, within(0.0001));
  }

  @Test
  void round1__keepsOneDecimal() {
    //
    //
    double actual = Geo.round1(2.449);
    //
    //

    assertThat(actual).isEqualTo(2.4);
  }
}
