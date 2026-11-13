# Dispatch Load Balancer

A Spring Boot REST API that assigns delivery orders to a fleet of vehicles. It respects vehicle capacity, serves high priority orders first, and keeps travel distance low using the Haversine formula.

- **Live URL:** _add after deploying_
- **Swagger UI:** `/swagger-ui.html` (OpenAPI JSON at `/v3/api-docs`)
- **Postman collection:** [`samples/dispatch.postman_collection.json`](samples/dispatch.postman_collection.json)

## Tech

Java 21, Spring Boot 4.1, Spring Web MVC, Spring Data JPA, Bean Validation, H2 (default) or PostgreSQL, springdoc-openapi, JUnit 5, AssertJ, MockMvc, JaCoCo.

## Run it

You need JDK 21. No database setup is needed, it uses an in-memory H2 database by default.

```bash
./mvnw spring-boot:run          # macOS / Linux
mvnw.cmd spring-boot:run        # Windows
```

The API starts on `http://localhost:8080`. Open `http://localhost:8080/swagger-ui.html` to try every endpoint.

To use PostgreSQL instead, set these environment variables before starting:

```
SPRING_DATASOURCE_URL=jdbc:postgresql://<host>:5432/<db>
SPRING_DATASOURCE_USERNAME=<user>
SPRING_DATASOURCE_PASSWORD=<password>
```

With Docker:

```bash
docker build -t dispatch .
docker run -p 8080:8080 dispatch
```

## Try it in 30 seconds

```bash
curl -X POST localhost:8080/api/dispatch/orders   -H "Content-Type: application/json" -d @samples/sample-orders.json
curl -X POST localhost:8080/api/dispatch/vehicles -H "Content-Type: application/json" -d @samples/sample-vehicles.json
curl localhost:8080/api/dispatch/plan
```

`samples/` holds the 30 orders and 5 vehicles from the assignment brief.

## API

| Method | Endpoint | What it does |
|---|---|---|
| POST | `/api/dispatch/orders` | Add or update delivery orders |
| POST | `/api/dispatch/vehicles` | Add or update vehicles |
| GET | `/api/dispatch/plan` | Build and return the dispatch plan |
| DELETE | `/api/dispatch/orders` | Remove all orders (handy between test runs) |
| DELETE | `/api/dispatch/vehicles` | Remove all vehicles |

Request and success response bodies follow the assignment spec exactly. The plan adds a few fields that make it easier to read and test:

```json
{
  "dispatchPlan": [
    {
      "vehicleId": "VEH001",
      "currentAddress": "Indiranagar, Bangalore, Karnataka, India",
      "capacity": 100.0,
      "totalLoad": 10.0,
      "totalDistance": "5.06 km",
      "totalDistanceKm": 5.06,
      "assignedOrders": [
        { "orderId": "ORD001", "latitude": 12.9716, "longitude": 77.5946,
          "address": "MG Road, Bangalore, Karnataka, India", "packageWeight": 10.0, "priority": "HIGH" }
      ]
    }
  ],
  "unassignedOrders": []
}
```

- `assignedOrders` are listed in the order the vehicle visits them.
- `unassignedOrders` lists every order no vehicle could take, each with a `reason`.

### Errors

Every error has the same shape and a 4xx status:

```json
{
  "message": "Validation failed",
  "status": "error",
  "errors": [
    "orders[0].latitude: latitude must be between -90 and 90",
    "orders[0].packageWeight: packageWeight must be greater than 0"
  ]
}
```

Handled cases: missing or blank fields, latitude outside -90..90, longitude outside -180..180, zero or negative weight or capacity, unknown priority, an empty list, duplicate ids in one request, malformed JSON, wrong content type, unknown endpoints and wrong HTTP methods.

## How the plan is built

The planner is a `DispatchStrategy` (strategy pattern), so a smarter algorithm can be swapped in without touching the API. The current one, `GreedyNearestVehicleStrategy`, works like this:

1. **Sort orders** by priority (HIGH, MEDIUM, LOW), then heaviest first, then by `orderId`. Placing heavy packages early, while there is still room, wastes less capacity.
2. **Pick a vehicle for each order.** Of the vehicles that still have room for it, choose the one closest to the order right now. A vehicle starts at its current location and moves to each stop it is given, so its route grows stop by stop. Ties go to the lowest `vehicleId`, which keeps the result deterministic.
3. **Total distance** for a vehicle is the sum of the legs from its start through its stops, in visit order, using Haversine (Earth radius 6371 km).
4. **Unassignable orders** are never forced onto a vehicle. They go to `unassignedOrders` with a reason, such as "No vehicle has enough remaining capacity" or "Package weight 500 exceeds the capacity of every vehicle (max 120)".

**Why a heuristic.** Finding the true minimum total distance with capacities is the capacitated vehicle routing problem, which is NP-hard. The greedy approach is fast, predictable and never breaks a constraint. It runs in O(n log n + n × m) for n orders and m vehicles, and a test plans 10,000 orders across 200 vehicles in well under a second.

With the sample data, the fleet can carry 500 kg but the orders weigh 645 kg. So all 275 kg of HIGH orders are assigned, the MEDIUM and LOW orders fill the remaining room, and 8 LOW orders (145 kg) come back as unassigned with a reason.

### Assumptions

- Weights and capacities are in the same unit (kg), and decimals are allowed.
- "High priority first" means high priority orders get first claim on capacity and are visited first on each route.
- Distance is straight-line Haversine distance, not road distance. `DistanceCalculator` is an interface, so a maps API could replace it later.
- Posting an existing `orderId` or `vehicleId` updates it (upsert) rather than creating a duplicate. The plan is always built from everything currently stored.
- Vehicles do not need to return to their start point.

## Project layout

```
src/main/java/com/abhrajit/dispatch
  domain/       DeliveryOrder and Vehicle entities, Priority enum
  repository/   Spring Data JPA repositories
  dto/          Request and response records with validation
  geo/          DistanceCalculator and the Haversine implementation
  planning/     DispatchStrategy and the greedy implementation
  service/      DispatchService (saving, upsert, building the plan)
  web/          Controller, global error handler, OpenAPI config
```

## Tests

```bash
./mvnw test
```

31 tests:

- **Haversine:** known city distances, zero distance, symmetry, antipodal points.
- **Planner:** nearest vehicle wins, priority wins when capacity is tight, capacity is never exceeded, oversized and unassignable orders, no vehicles, no orders, distance equals the sum of legs, deterministic output, every constraint on the full sample dataset, 10,000 orders performance check.
- **API (MockMvc against the full app):** success responses, upsert behaviour, each validation error, duplicate ids, malformed JSON, wrong content type, the spec example end to end, the sample dataset end to end, delete endpoints, Swagger docs.

A coverage report is written to `target/site/jacoco/index.html`. Current line coverage is about 96%.
