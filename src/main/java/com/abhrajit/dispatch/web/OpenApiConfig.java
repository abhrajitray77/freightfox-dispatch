package com.abhrajit.dispatch.web;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI dispatchOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Dispatch Load Balancer API")
                .version("1.0.0")
                .description("Assigns delivery orders to vehicles by priority, capacity and Haversine distance. "
                        + "Load orders and vehicles with the POST endpoints, then call GET /api/dispatch/plan."));
    }
}
