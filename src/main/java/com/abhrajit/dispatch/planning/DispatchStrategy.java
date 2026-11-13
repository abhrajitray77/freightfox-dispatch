package com.abhrajit.dispatch.planning;

import com.abhrajit.dispatch.domain.DeliveryOrder;
import com.abhrajit.dispatch.domain.Vehicle;

import java.util.List;

/**
 * Strategy for turning orders and vehicles into a dispatch plan.
 * Implementations must never exceed a vehicle's capacity and must serve higher priority orders first.
 */
public interface DispatchStrategy {
    DispatchResult plan(List<DeliveryOrder> orders, List<Vehicle> vehicles);
}
