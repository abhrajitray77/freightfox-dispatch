package com.abhrajit.dispatch.geo;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class HaversineDistanceCalculatorTest {

    private final HaversineDistanceCalculator calc = new HaversineDistanceCalculator();

    @Test
    void samePointIsZero() {
        GeoPoint p = new GeoPoint(28.6139, 77.2090);
        assertThat(calc.distanceKm(p, p)).isEqualTo(0.0);
    }

    @Test
    void connaughtPlaceToNoidaSector18() {
        double d = calc.distanceKm(new GeoPoint(28.6139, 77.2090), new GeoPoint(28.5355, 77.3910));
        assertThat(d).isCloseTo(19.795, within(0.01));
    }

    @Test
    void mgRoadBangaloreToAnnaSalaiChennai() {
        double d = calc.distanceKm(new GeoPoint(12.9716, 77.5946), new GeoPoint(13.0827, 80.2707));
        assertThat(d).isCloseTo(290.172, within(0.01));
    }

    @Test
    void isSymmetric() {
        GeoPoint a = new GeoPoint(28.7041, 77.1025);
        GeoPoint b = new GeoPoint(28.4595, 77.0266);
        assertThat(calc.distanceKm(a, b)).isCloseTo(calc.distanceKm(b, a), within(1e-9));
    }

    @Test
    void antipodalPointsAreHalfTheEarthsCircumference() {
        double d = calc.distanceKm(new GeoPoint(0, 0), new GeoPoint(0, 180));
        assertThat(d).isCloseTo(Math.PI * HaversineDistanceCalculator.EARTH_RADIUS_KM, within(0.001));
    }
}
