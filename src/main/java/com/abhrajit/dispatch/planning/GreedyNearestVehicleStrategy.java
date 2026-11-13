package com.abhrajit.dispatch.planning;

import com.abhrajit.dispatch.domain.DeliveryOrder;
import com.abhrajit.dispatch.domain.Vehicle;
import com.abhrajit.dispatch.geo.DistanceCalculator;
import com.abhrajit.dispatch.geo.GeoPoint;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Greedy priority-first assignment.
 *
 * 1. Orders are sorted by priority (HIGH, MEDIUM, LOW), then by weight (heaviest first, so big packages
 *    are placed while there is still room), then by orderId for a stable result.
 * 2. Each order goes to the vehicle that can still carry it and is closest to it right now. A vehicle's
 *    position is its start point, then the last stop it was given, so routes grow stop by stop.
 * 3. A vehicle's distance is the sum of the legs from its start through its stops in that order.
 * 4. Orders that fit no vehicle are returned as unassigned with a reason.
 *
 * Exact minimum total distance is a capacitated vehicle routing problem (NP-hard), so this is a fast
 * heuristic: O(n log n + n * m) for n orders and m vehicles, deterministic, and it never breaks a constraint.
 */
@Component
public class GreedyNearestVehicleStrategy implements DispatchStrategy {

    private static final Comparator<DeliveryOrder> ORDER_PRIORITY = Comparator
            .comparingInt((DeliveryOrder o) -> o.getPriority().rank())
            .thenComparing(Comparator.comparingDouble(DeliveryOrder::getPackageWeight).reversed())
            .thenComparing(DeliveryOrder::getOrderId);

    private final DistanceCalculator distanceCalculator;

    public GreedyNearestVehicleStrategy(DistanceCalculator distanceCalculator) {
        this.distanceCalculator = distanceCalculator;
    }

    @Override
    public DispatchResult plan(List<DeliveryOrder> orders, List<Vehicle> vehicles) {
        List<RouteBuilder> builders = vehicles.stream()
                .sorted(Comparator.comparing(Vehicle::getVehicleId))
                .map(RouteBuilder::new)
                .toList();
        double maxCapacity = vehicles.stream().mapToDouble(Vehicle::getCapacity).max().orElse(0);

        List<UnassignedOrder> unassigned = new ArrayList<>();
        for (DeliveryOrder order : orders.stream().sorted(ORDER_PRIORITY).toList()) {
            GeoPoint target = new GeoPoint(order.getLatitude(), order.getLongitude());
            RouteBuilder best = null;
            double bestDistance = Double.MAX_VALUE;
            for (RouteBuilder b : builders) {
                if (b.remaining() < order.getPackageWeight()) continue;
                double d = distanceCalculator.distanceKm(b.position, target);
                if (d < bestDistance) { // builders are sorted by id, so ties keep the lowest vehicleId
                    best = b;
                    bestDistance = d;
                }
            }
            if (best == null) {
                unassigned.add(new UnassignedOrder(order, reasonFor(order, vehicles.isEmpty(), maxCapacity)));
            } else {
                best.add(order, target, bestDistance);
            }
        }
        return new DispatchResult(builders.stream().map(RouteBuilder::build).toList(), unassigned);
    }

    private static String reasonFor(DeliveryOrder order, boolean noVehicles, double maxCapacity) {
        if (noVehicles) return "No vehicles available";
        if (order.getPackageWeight() > maxCapacity) {
            return "Package weight " + fmt(order.getPackageWeight()) + " exceeds the capacity of every vehicle (max " + fmt(maxCapacity) + ")";
        }
        return "No vehicle has enough remaining capacity";
    }

    private static String fmt(double v) {
        return v == Math.rint(v) ? String.valueOf((long) v) : String.valueOf(v);
    }

    /** Mutable state for one vehicle while planning. */
    private static final class RouteBuilder {
        private final Vehicle vehicle;
        private final List<DeliveryOrder> stops = new ArrayList<>();
        private GeoPoint position;
        private double load;
        private double distance;

        RouteBuilder(Vehicle vehicle) {
            this.vehicle = vehicle;
            this.position = new GeoPoint(vehicle.getCurrentLatitude(), vehicle.getCurrentLongitude());
        }

        double remaining() {
            return vehicle.getCapacity() - load;
        }

        void add(DeliveryOrder order, GeoPoint at, double legKm) {
            stops.add(order);
            load += order.getPackageWeight();
            distance += legKm;
            position = at;
        }

        VehicleRoute build() {
            return new VehicleRoute(vehicle, List.copyOf(stops), load, distance);
        }
    }
}
