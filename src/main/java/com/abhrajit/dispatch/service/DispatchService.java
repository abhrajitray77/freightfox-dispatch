package com.abhrajit.dispatch.service;

import com.abhrajit.dispatch.domain.DeliveryOrder;
import com.abhrajit.dispatch.domain.Vehicle;
import com.abhrajit.dispatch.dto.DispatchPlanResponse;
import com.abhrajit.dispatch.dto.OrderRequest;
import com.abhrajit.dispatch.dto.OrderView;
import com.abhrajit.dispatch.dto.UnassignedOrderView;
import com.abhrajit.dispatch.dto.VehiclePlan;
import com.abhrajit.dispatch.dto.VehicleRequest;
import com.abhrajit.dispatch.planning.DispatchResult;
import com.abhrajit.dispatch.planning.DispatchStrategy;
import com.abhrajit.dispatch.repository.DeliveryOrderRepository;
import com.abhrajit.dispatch.repository.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;

@Service
public class DispatchService {

    private final DeliveryOrderRepository orderRepository;
    private final VehicleRepository vehicleRepository;
    private final DispatchStrategy strategy;

    public DispatchService(DeliveryOrderRepository orderRepository, VehicleRepository vehicleRepository, DispatchStrategy strategy) {
        this.orderRepository = orderRepository;
        this.vehicleRepository = vehicleRepository;
        this.strategy = strategy;
    }

    /** Saves orders. An order whose id already exists is updated (upsert), not duplicated. */
    @Transactional
    public int saveOrders(List<OrderRequest> requests) {
        rejectDuplicates(requests, OrderRequest::orderId, "orderId");
        List<DeliveryOrder> orders = requests.stream()
                .map(r -> new DeliveryOrder(r.orderId().trim(), r.latitude(), r.longitude(), r.address().trim(), r.packageWeight(), r.priority()))
                .toList();
        orderRepository.saveAll(orders);
        return orders.size();
    }

    /** Saves vehicles. A vehicle whose id already exists is updated (upsert), not duplicated. */
    @Transactional
    public int saveVehicles(List<VehicleRequest> requests) {
        rejectDuplicates(requests, VehicleRequest::vehicleId, "vehicleId");
        List<Vehicle> vehicles = requests.stream()
                .map(r -> new Vehicle(r.vehicleId().trim(), r.capacity(), r.currentLatitude(), r.currentLongitude(), r.currentAddress().trim()))
                .toList();
        vehicleRepository.saveAll(vehicles);
        return vehicles.size();
    }

    @Transactional(readOnly = true)
    public DispatchPlanResponse buildPlan() {
        DispatchResult result = strategy.plan(orderRepository.findAll(), vehicleRepository.findAll());
        List<VehiclePlan> plans = result.routes().stream()
                .map(r -> new VehiclePlan(
                        r.vehicle().getVehicleId(),
                        r.vehicle().getCurrentAddress(),
                        r.vehicle().getCapacity(),
                        r.totalLoad(),
                        String.format(Locale.ROOT, "%.2f km", r.totalDistanceKm()),
                        Math.round(r.totalDistanceKm() * 100.0) / 100.0,
                        r.stops().stream().map(OrderView::of).toList()))
                .toList();
        List<UnassignedOrderView> unassigned = result.unassigned().stream()
                .map(u -> new UnassignedOrderView(OrderView.of(u.order()), u.reason()))
                .toList();
        return new DispatchPlanResponse(plans, unassigned);
    }

    @Transactional
    public void deleteAllOrders() {
        orderRepository.deleteAllInBatch();
    }

    @Transactional
    public void deleteAllVehicles() {
        vehicleRepository.deleteAllInBatch();
    }

    private static <T> void rejectDuplicates(List<T> items, Function<T, String> id, String field) {
        Set<String> seen = new HashSet<>();
        Set<String> dupes = new LinkedHashSet<>();
        for (T item : items) {
            String key = id.apply(item).trim();
            if (!seen.add(key)) dupes.add(key);
        }
        if (!dupes.isEmpty()) {
            throw new InvalidRequestException("Duplicate " + field + " in request",
                    dupes.stream().map(d -> field + " '" + d + "' appears more than once").toList());
        }
    }
}
