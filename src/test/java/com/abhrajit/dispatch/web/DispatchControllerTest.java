package com.abhrajit.dispatch.web;

import com.abhrajit.dispatch.repository.DeliveryOrderRepository;
import com.abhrajit.dispatch.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class DispatchControllerTest {

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private DeliveryOrderRepository orders;
    @Autowired
    private VehicleRepository vehicles;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        orders.deleteAll();
        vehicles.deleteAll();
    }

    private ResultActions postJson(String url, String body) throws Exception {
        return mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private static String resource(String name) throws Exception {
        return new ClassPathResource(name).getContentAsString(StandardCharsets.UTF_8);
    }

    private static final String ONE_ORDER = """
            {"orders":[{"orderId":"ORD001","latitude":12.9716,"longitude":77.5946,"address":"MG Road, Bangalore, Karnataka, India","packageWeight":10,"priority":"HIGH"}]}""";

    private static final String ONE_VEHICLE = """
            {"vehicles":[{"vehicleId":"VEH001","capacity":100,"currentLatitude":12.9716,"currentLongitude":77.6413,"currentAddress":"Indiranagar, Bangalore, Karnataka, India"}]}""";

    @Test
    void acceptsOrders() throws Exception {
        postJson("/api/dispatch/orders", ONE_ORDER)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Delivery orders accepted."))
                .andExpect(jsonPath("$.status").value("success"));
        assertThat(orders.count()).isEqualTo(1);
    }

    @Test
    void acceptsVehicles() throws Exception {
        postJson("/api/dispatch/vehicles", ONE_VEHICLE)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Vehicle details accepted."))
                .andExpect(jsonPath("$.status").value("success"));
        assertThat(vehicles.count()).isEqualTo(1);
    }

    @Test
    void sameOrderIdUpdatesInsteadOfDuplicating() throws Exception {
        postJson("/api/dispatch/orders", ONE_ORDER).andExpect(status().isOk());
        postJson("/api/dispatch/orders", ONE_ORDER.replace("\"packageWeight\":10", "\"packageWeight\":42")).andExpect(status().isOk());

        assertThat(orders.count()).isEqualTo(1);
        assertThat(orders.findById("ORD001")).get().extracting(o -> o.getPackageWeight()).isEqualTo(42.0);
    }

    @Test
    void rejectsInvalidFieldsWithClearMessages() throws Exception {
        String bad = """
                {"orders":[{"orderId":"","latitude":95,"longitude":77.5,"address":"x","packageWeight":-1,"priority":"HIGH"}]}""";
        postJson("/api/dispatch/orders", bad)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors", hasItem("orders[0].latitude: latitude must be between -90 and 90")))
                .andExpect(jsonPath("$.errors", hasItem("orders[0].orderId: orderId is required")))
                .andExpect(jsonPath("$.errors", hasItem("orders[0].packageWeight: packageWeight must be greater than 0")));
        assertThat(orders.count()).isZero();
    }

    @Test
    void rejectsEmptyOrderList() throws Exception {
        postJson("/api/dispatch/orders", "{\"orders\":[]}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasItem("orders: orders must contain at least one order")));
    }

    @Test
    void rejectsUnknownPriority() throws Exception {
        postJson("/api/dispatch/orders", ONE_ORDER.replace("HIGH", "URGENT"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("priority is HIGH, MEDIUM or LOW")));
    }

    @Test
    void rejectsMalformedJson() throws Exception {
        postJson("/api/dispatch/orders", "{\"orders\": [")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));
    }

    @Test
    void rejectsDuplicateIdsInOneRequest() throws Exception {
        String dup = ONE_VEHICLE.replace("]}", "," + ONE_VEHICLE.substring(ONE_VEHICLE.indexOf('[') + 1));
        postJson("/api/dispatch/vehicles", dup)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Duplicate vehicleId in request"))
                .andExpect(jsonPath("$.errors[0]").value("vehicleId 'VEH001' appears more than once"));
    }

    @Test
    void rejectsZeroCapacityVehicle() throws Exception {
        postJson("/api/dispatch/vehicles", ONE_VEHICLE.replace("\"capacity\":100", "\"capacity\":0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasItem("vehicles[0].capacity: capacity must be greater than 0")));
    }

    @Test
    void wrongContentTypeIs415() throws Exception {
        mvc.perform(post("/api/dispatch/orders").contentType(MediaType.TEXT_PLAIN).content("hi"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void planMatchesTheSpecExample() throws Exception {
        postJson("/api/dispatch/orders", ONE_ORDER).andExpect(status().isOk());
        postJson("/api/dispatch/vehicles", ONE_VEHICLE).andExpect(status().isOk());

        mvc.perform(get("/api/dispatch/plan"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dispatchPlan", hasSize(1)))
                .andExpect(jsonPath("$.dispatchPlan[0].vehicleId").value("VEH001"))
                .andExpect(jsonPath("$.dispatchPlan[0].totalLoad").value(10.0))
                .andExpect(jsonPath("$.dispatchPlan[0].totalDistance").value("5.06 km"))
                .andExpect(jsonPath("$.dispatchPlan[0].assignedOrders[0].orderId").value("ORD001"))
                .andExpect(jsonPath("$.dispatchPlan[0].assignedOrders[0].address").value("MG Road, Bangalore, Karnataka, India"))
                .andExpect(jsonPath("$.unassignedOrders", hasSize(0)));
    }

    @Test
    void sampleDatasetEndToEnd() throws Exception {
        postJson("/api/dispatch/orders", resource("sample-orders.json")).andExpect(status().isOk());
        postJson("/api/dispatch/vehicles", resource("sample-vehicles.json")).andExpect(status().isOk());

        mvc.perform(get("/api/dispatch/plan"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dispatchPlan", hasSize(5)))
                .andExpect(jsonPath("$.dispatchPlan[0].totalLoad", lessThanOrEqualTo(100.0)))
                .andExpect(jsonPath("$.unassignedOrders[*].order.priority", everyItem(not("HIGH"))))
                .andExpect(jsonPath("$.unassignedOrders[0].reason").value("No vehicle has enough remaining capacity"));
    }

    @Test
    void planWithNoDataIsEmpty() throws Exception {
        mvc.perform(get("/api/dispatch/plan"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dispatchPlan", hasSize(0)))
                .andExpect(jsonPath("$.unassignedOrders", hasSize(0)));
    }

    @Test
    void deleteEndpointsClearData() throws Exception {
        postJson("/api/dispatch/orders", ONE_ORDER).andExpect(status().isOk());
        postJson("/api/dispatch/vehicles", ONE_VEHICLE).andExpect(status().isOk());

        mvc.perform(delete("/api/dispatch/orders")).andExpect(status().isOk());
        mvc.perform(delete("/api/dispatch/vehicles")).andExpect(status().isOk());

        assertThat(orders.count()).isZero();
        assertThat(vehicles.count()).isZero();
    }

    @Test
    void swaggerDocsAreServed() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/api/dispatch/plan")));
    }
}
