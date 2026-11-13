package com.abhrajit.dispatch.geo;

/** Distance between two points, in kilometres. Kept as an interface so a road-distance API could replace it later. */
public interface DistanceCalculator {
    double distanceKm(GeoPoint from, GeoPoint to);
}
