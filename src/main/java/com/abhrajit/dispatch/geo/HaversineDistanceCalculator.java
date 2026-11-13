package com.abhrajit.dispatch.geo;

import org.springframework.stereotype.Component;

/** Great-circle distance with the Haversine formula, using a mean Earth radius of 6371 km. */
@Component
public class HaversineDistanceCalculator implements DistanceCalculator {

    static final double EARTH_RADIUS_KM = 6371.0;

    @Override
    public double distanceKm(GeoPoint from, GeoPoint to) {
        double lat1 = Math.toRadians(from.latitude());
        double lat2 = Math.toRadians(to.latitude());
        double dLat = lat2 - lat1;
        double dLon = Math.toRadians(to.longitude() - from.longitude());

        double h = Math.pow(Math.sin(dLat / 2), 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(dLon / 2), 2);
        return 2 * EARTH_RADIUS_KM * Math.asin(Math.min(1.0, Math.sqrt(h)));
    }
}
