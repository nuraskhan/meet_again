package kz.nuraskhan.meet_again.model.rnd;

import static org.assertj.core.api.Assertions.assertThat;

import kz.nuraskhan.meet_again.model.entity.Event;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

class RNDTest {

  @Test
  void str__hasRequestedLengthAndStartsWithLetter() {
    //
    //
    String actual = RND.str(12);
    //
    //

    assertThat(actual).hasSize(12).matches("[a-z][a-z0-9]{11}");
  }

  @RepeatedTest(50)
  void intBetween__includesBothEnds() {
    //
    //
    int actual = RND.intBetween(3, 4);
    //
    //

    assertThat(actual).isBetween(3, 4);
  }

  @RepeatedTest(50)
  void event__satisfiesTheCheckConstraintsOfTheTable() {
    //
    //
    Event actual = RND.event(1L, 2L);
    //
    //

    assertThat(actual.distanceMinKm).isBetween(0, actual.distanceMaxKm);
    assertThat(actual.priceMin).isBetween(0, actual.priceMax);
    assertThat(actual.shareToken).hasSize(32);
  }
}
