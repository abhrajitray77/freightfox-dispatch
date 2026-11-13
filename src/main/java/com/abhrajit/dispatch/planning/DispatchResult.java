package com.abhrajit.dispatch.planning;

import java.util.List;

public record DispatchResult(List<VehicleRoute> routes, List<UnassignedOrder> unassigned) {
}
