package com.abhrajit.dispatch.planning;

import com.abhrajit.dispatch.domain.DeliveryOrder;
import com.abhrajit.dispatch.domain.Priority;
import com.abhrajit.dispatch.domain.Vehicle;
import com.abhrajit.dispatch.geo.HaversineDistanceCalculator;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class GreedyNearestVehicleStrategyTest {

    private static final double[] CP = {28.6139, 77.2090};
    private static final double[] KB = {28.7041, 77.1025};
    private static final double[] NOIDA = {28.5355, 77.3910};
    private static final double[] GURGAON = {28.4595, 77.0266};

    private final GreedyNearestVehicleStrategy strategy = new GreedyNearestVehicleStrategy(new HaversineDistanceCalculator());

    private static DeliveryOrder order(String id, double[] at, double weight, Priority p) {
        return new DeliveryOrder(id, at[0], at[1], "addr " + id, weight, p);
    }

    private static Vehicle vehicle(String id, double capacity, double[] at) {
        return new Vehicle(id, capacity, at[0], at[1], "start " + id);
    }

    private static Map<String, VehicleRoute> byVehicle(DispatchResult r) {
        return r.routes().stream().collect(Collectors.toMap(v -> v.vehicle().getVehicleId(), v -> v));
    }

    @Test
    void assignsOrderToNearestVehicle() {
        DispatchResult r = strategy.plan(
                List.of(order("O1", NOIDA, 10, Priority.HIGH)),
                List.of(vehicle("V1", 100, KB), vehicle("V2", 100, NOIDA)));

        assertThat(byVehicle(r).get("V2").stops()).extracting(DeliveryOrder::getOrderId).containsExactly("O1");
        assertThat(byVehicle(r).get("V1").stops()).isEmpty();
        assertThat(r.unassigned()).isEmpty();
    }

    @Test
    void highPriorityWinsWhenCapacityIsTight() {
        // one vehicle, room for only one of the two orders
        DispatchResult r = strategy.plan(
                List.of(order("LOW1", CP, 60, Priority.LOW), order("HIGH1", CP, 60, Priority.HIGH)),
                List.of(vehicle("V1", 100, CP)));

        assertThat(r.routes().get(0).stops()).extracting(DeliveryOrder::getOrderId).containsExactly("HIGH1");
        assertThat(r.unassigned()).extracting(u -> u.order().getOrderId()).containsExactly("LOW1");
        assertThat(r.unassigned().get(0).reason()).isEqualTo("No vehicle has enough remaining capacity");
    }

    @Test
    void neverExceedsCapacityAndFallsBackToNextNearestVehicle() {
        DispatchResult r = strategy.plan(
                List.of(order("O1", NOIDA, 70, Priority.HIGH), order("O2", NOIDA, 70, Priority.HIGH)),
                List.of(vehicle("NEAR", 100, NOIDA), vehicle("FAR", 100, GURGAON)));

        Map<String, VehicleRoute> routes = byVehicle(r);
        assertThat(routes.get("NEAR").totalLoad()).isEqualTo(70);
        assertThat(routes.get("FAR").totalLoad()).isEqualTo(70);
        assertThat(r.unassigned()).isEmpty();
    }

    @Test
    void orderHeavierThanEveryVehicleIsUnassignedWithReason() {
        DispatchResult r = strategy.plan(
                List.of(order("BIG", CP, 500, Priority.HIGH)),
                List.of(vehicle("V1", 100, CP), vehicle("V2", 120, KB)));

        assertThat(r.unassigned()).hasSize(1);
        assertThat(r.unassigned().get(0).reason()).isEqualTo("Package weight 500 exceeds the capacity of every vehicle (max 120)");
    }

    @Test
    void noVehiclesMeansEverythingUnassigned() {
        DispatchResult r = strategy.plan(List.of(order("O1", CP, 10, Priority.LOW)), List.of());

        assertThat(r.routes()).isEmpty();
        assertThat(r.unassigned()).singleElement().extracting(UnassignedOrder::reason).isEqualTo("No vehicles available");
    }

    @Test
    void noOrdersGivesEmptyRoutes() {
        DispatchResult r = strategy.plan(List.of(), List.of(vehicle("V1", 100, CP)));

        assertThat(r.routes()).singleElement().satisfies(v -> {
            assertThat(v.stops()).isEmpty();
            assertThat(v.totalLoad()).isZero();
            assertThat(v.totalDistanceKm()).isZero();
        });
    }

    @Test
    void distanceIsTheSumOfLegsInVisitOrder() {
        // vehicle at Karol Bagh: first leg to CP, second leg CP to Noida
        DispatchResult r = strategy.plan(
                List.of(order("A", CP, 10, Priority.HIGH), order("B", NOIDA, 10, Priority.MEDIUM)),
                List.of(vehicle("V1", 100, KB)));

        VehicleRoute route = r.routes().get(0);
        assertThat(route.stops()).extracting(DeliveryOrder::getOrderId).containsExactly("A", "B");
        assertThat(route.totalDistanceKm()).isCloseTo(14.442 + 19.795, within(0.01));
    }

    @Test
    void sameInputGivesSamePlan() {
        List<DeliveryOrder> orders = List.of(order("O1", CP, 10, Priority.HIGH), order("O2", CP, 10, Priority.HIGH));
        List<Vehicle> vehicles = List.of(vehicle("V2", 100, CP), vehicle("V1", 100, CP));

        assertThat(strategy.plan(orders, vehicles)).usingRecursiveComparison().isEqualTo(strategy.plan(orders, vehicles));
        // tie on distance goes to the lowest vehicleId
        assertThat(byVehicle(strategy.plan(orders, vehicles)).get("V1").stops()).hasSize(2);
    }

    @Test
    void sampleDatasetRespectsEveryConstraint() {
        List<DeliveryOrder> orders = SampleData.orders();
        List<Vehicle> vehicles = SampleData.vehicles();

        DispatchResult r = strategy.plan(orders, vehicles);

        // capacity is never exceeded
        for (VehicleRoute route : r.routes()) {
            assertThat(route.totalLoad()).isLessThanOrEqualTo(route.vehicle().getCapacity());
            assertThat(route.totalLoad()).isEqualTo(route.stops().stream().mapToDouble(DeliveryOrder::getPackageWeight).sum());
        }
        // every order is either assigned once or unassigned, never both
        List<String> placed = new ArrayList<>();
        r.routes().forEach(v -> v.stops().forEach(o -> placed.add(o.getOrderId())));
        r.unassigned().forEach(u -> placed.add(u.order().getOrderId()));
        assertThat(placed).hasSize(30).doesNotHaveDuplicates();
        // 645 kg of orders against 500 kg of fleet capacity: all HIGH orders (275 kg) fit and are assigned
        assertThat(r.unassigned()).noneMatch(u -> u.order().getPriority() == Priority.HIGH);
        double assigned = r.routes().stream().mapToDouble(VehicleRoute::totalLoad).sum();
        assertThat(assigned).isLessThanOrEqualTo(500);
    }

    @Test
    void handlesTenThousandOrdersQuickly() {
        List<DeliveryOrder> orders = IntStream.range(0, 10_000)
                .mapToObj(i -> order("O" + i, new double[]{28.4 + (i % 50) * 0.01, 77.0 + (i % 70) * 0.01}, 1 + i % 5, Priority.values()[i % 3]))
                .toList();
        List<Vehicle> vehicles = IntStream.range(0, 200)
                .mapToObj(i -> vehicle("V" + i, 200, new double[]{28.4 + (i % 20) * 0.02, 77.0 + (i % 10) * 0.05}))
                .toList();

        long start = System.nanoTime();
        DispatchResult r = strategy.plan(orders, vehicles);
        long ms = (System.nanoTime() - start) / 1_000_000;

        assertThat(r.routes()).allMatch(v -> v.totalLoad() <= v.vehicle().getCapacity());
        assertThat(ms).isLessThan(3_000);
    }
}
