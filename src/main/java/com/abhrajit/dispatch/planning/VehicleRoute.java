package com.abhrajit.dispatch.planning;

import com.abhrajit.dispatch.domain.DeliveryOrder;
import com.abhrajit.dispatch.domain.Vehicle;

import java.util.List;

/** One vehicle's planned stops, in the order it visits them. */
public record VehicleRoute(Vehicle vehicle, List<DeliveryOrder> stops, double totalLoad, double totalDistanceKm) {
}
