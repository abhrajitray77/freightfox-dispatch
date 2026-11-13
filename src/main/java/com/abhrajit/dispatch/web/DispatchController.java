package com.abhrajit.dispatch.web;

import com.abhrajit.dispatch.dto.ApiResponse;
import com.abhrajit.dispatch.dto.DispatchPlanResponse;
import com.abhrajit.dispatch.dto.OrdersRequest;
import com.abhrajit.dispatch.dto.VehiclesRequest;
import com.abhrajit.dispatch.service.DispatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dispatch")
@Tag(name = "Dispatch", description = "Load delivery orders and vehicles, then get an optimised dispatch plan")
public class DispatchController {

    private final DispatchService service;

    public DispatchController(DispatchService service) {
        this.service = service;
    }

    @PostMapping("/orders")
    @Operation(summary = "Add or update delivery orders",
            description = "Orders are stored by orderId. Sending an existing orderId updates that order instead of duplicating it.")
    public ResponseEntity<ApiResponse> addOrders(@Valid @RequestBody OrdersRequest request) {
        service.saveOrders(request.orders());
        return ResponseEntity.ok(ApiResponse.success("Delivery orders accepted."));
    }

    @PostMapping("/vehicles")
    @Operation(summary = "Add or update fleet vehicles",
            description = "Vehicles are stored by vehicleId. Sending an existing vehicleId updates that vehicle instead of duplicating it.")
    public ResponseEntity<ApiResponse> addVehicles(@Valid @RequestBody VehiclesRequest request) {
        service.saveVehicles(request.vehicles());
        return ResponseEntity.ok(ApiResponse.success("Vehicle details accepted."));
    }

    @GetMapping("/plan")
    @Operation(summary = "Get the dispatch plan",
            description = "Assigns every stored order to a vehicle: high priority first, never over capacity, nearest vehicle first. "
                    + "Orders that fit no vehicle are listed in unassignedOrders with a reason.")
    public DispatchPlanResponse getPlan() {
        return service.buildPlan();
    }

    @DeleteMapping("/orders")
    @Operation(summary = "Remove all stored orders", description = "Handy for resetting between test runs.")
    public ApiResponse deleteOrders() {
        service.deleteAllOrders();
        return ApiResponse.success("All delivery orders removed.");
    }

    @DeleteMapping("/vehicles")
    @Operation(summary = "Remove all stored vehicles", description = "Handy for resetting between test runs.")
    public ApiResponse deleteVehicles() {
        service.deleteAllVehicles();
        return ApiResponse.success("All vehicles removed.");
    }
}
