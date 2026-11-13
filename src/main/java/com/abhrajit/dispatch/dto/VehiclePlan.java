package com.abhrajit.dispatch.dto;

import java.util.List;

public record VehiclePlan(String vehicleId, String currentAddress, double capacity, double totalLoad, String totalDistance,
                          double totalDistanceKm, List<OrderView> assignedOrders) {
}
