# OrderFlow

OrderFlow is an event-driven microservices order management platform built with Java, Spring Boot, Spring Cloud, Kafka, Redis, MySQL, Eureka, API Gateway, Prometheus, Grafana, Docker, and React.

It demonstrates a real backend interview story: a synchronous order-to-inventory flow was replaced with asynchronous Kafka events so services can scale and fail independently.

## Links

- Live Vercel console: [https://orderflow-console-surya.vercel.app](https://orderflow-console-surya.vercel.app)
- Local Docker console: [http://localhost:5173](http://localhost:5173)

## Architecture

```text
React Console
  |
  v
API Gateway :8080
  |-- JWT validation
  |-- Redis-backed rate limiting
  |-- Circuit breaker fallbacks
  |
  +-- /api/orders/**     -> order-service :8081
  +-- /api/inventory/**  -> inventory-service :8082

Eureka Server :8761
  |
  +-- service discovery for gateway and services

Kafka
  |
  +-- orders.placed
  +-- inventory.updated
  +-- inventory.low-stock
  +-- orders.placed.DLQ

order-service
  |-- own database: order_db
  |-- saves orders as PENDING
  |-- publishes orders.placed
  |-- consumes inventory.updated

inventory-service
  |-- own database: inventory_db
  |-- consumes orders.placed
  |-- decrements stock
  |-- Redis cache-aside for product lookup
  |-- publishes inventory.updated and inventory.low-stock

notification-service
  |-- consumes order and inventory events
  |-- logs customer notifications and low-stock alerts

Prometheus + Grafana
  |-- scrape /actuator/prometheus from every service
```

## Tech Stack

| Area | Technology |
| --- | --- |
| Backend | Java 17, Spring Boot 3.3, Spring MVC/WebFlux |
| Microservices | Spring Cloud Gateway, Eureka Discovery |
| Messaging | Apache Kafka, Spring Kafka |
| Persistence | MySQL, H2 local defaults, Spring Data JPA, Hibernate |
| Cache | Redis, Spring Cache |
| Security | Gateway JWT validation, user context headers |
| Resilience | Gateway circuit breakers, Kafka retry + DLQ |
| Observability | Spring Actuator, Micrometer, Prometheus, Grafana |
| Frontend | React, Vite, CSS |
| Testing | JUnit 5, Mockito |
| DevOps | Docker Compose, Dockerfiles, GitHub Actions |

## Services

| Service | Port | Purpose |
| --- | --- | --- |
| Eureka Server | `8761` | Service registry |
| API Gateway | `8080` | Routing, JWT, rate limiting, circuit breakers |
| Order Service | `8081` | Order lifecycle and order status |
| Inventory Service | `8082` | Product stock, Redis cache, inventory events |
| Notification Service | `8083` | Event-driven notifications and alerts |
| Frontend | `5173` | Demo console |
| Prometheus | `9090` | Metrics |
| Grafana | `3000` | Dashboards |

## Run With Docker

```powershell
cd orderflow
docker compose up -d
```

This starts Eureka, API Gateway, Order Service, Inventory Service, Notification Service, React frontend, MySQL for each service, Apache Kafka in KRaft mode, Redis, Prometheus, and Grafana.

Open the console:

```text
http://localhost:5173
```

Check containers:

```powershell
docker compose ps
```

Stop the stack:

```powershell
docker compose down
```

Grafana login:

```text
admin / admin
```

## Run Backend Services

Start each service in a separate terminal:

```powershell
cd orderflow
..\mvnw.cmd -pl eureka-server spring-boot:run
..\mvnw.cmd -pl inventory-service spring-boot:run
..\mvnw.cmd -pl order-service spring-boot:run
..\mvnw.cmd -pl notification-service spring-boot:run
..\mvnw.cmd -pl api-gateway spring-boot:run
```

Open Eureka:

```text
http://localhost:8761
```

## Run Frontend

```powershell
cd orderflow/frontend
npm install
npm run dev
```

Open:

```text
http://localhost:5173
```

Set the gateway URL if needed:

```powershell
$env:VITE_API_URL="http://localhost:8080"
npm run dev
```

For a static Vercel demo build:

```powershell
$env:VITE_DEMO_MODE="true"
npm run build
```

## Demo Flow

Issue a demo JWT:

```powershell
$tokenResponse = Invoke-RestMethod `
  -Uri "http://localhost:8080/api/auth/token" `
  -Method Post `
  -ContentType "application/json" `
  -Body '{
    "userId": "surya-demo",
    "email": "surya@example.com",
    "role": "CUSTOMER"
  }'

$token = $tokenResponse.accessToken
```

Check product stock:

```powershell
Invoke-RestMethod `
  -Uri "http://localhost:8080/api/inventory/1" `
  -Headers @{ Authorization = "Bearer $token" }
```

Place an order:

```powershell
$order = Invoke-RestMethod `
  -Uri "http://localhost:8080/api/orders" `
  -Method Post `
  -ContentType "application/json" `
  -Headers @{ Authorization = "Bearer $token" } `
  -Body '{
    "customerEmail": "customer@example.com",
    "items": [
      { "productId": 1, "quantity": 2 }
    ]
  }'
```

Immediately after creation, the order is usually:

```text
PENDING
```

After Kafka processing:

```powershell
Invoke-RestMethod `
  -Uri "http://localhost:8080/api/orders/$($order.id)" `
  -Headers @{ Authorization = "Bearer $token" }
```

Expected status:

```text
CONFIRMED
```

If stock is insufficient, Inventory Service publishes a failed `inventory.updated` event and Order Service marks the order:

```text
FAILED
```

## What Happens When An Order Is Placed

1. Client sends `POST /api/orders` through the API Gateway.
2. Gateway validates the JWT and applies Redis-backed rate limiting.
3. Order Service saves the order as `PENDING`.
4. Order Service publishes `orders.placed`.
5. Inventory Service consumes `orders.placed`.
6. Inventory Service validates stock, decrements stock, evicts product cache, and publishes `inventory.updated`.
7. If stock falls below threshold, Inventory Service publishes `inventory.low-stock`.
8. Order Service consumes `inventory.updated` and changes order status to `CONFIRMED` or `FAILED`.
9. Notification Service consumes events and logs customer/alert notifications.
10. Prometheus scrapes metrics and Grafana visualizes system health.

## Resilience Features

- Gateway circuit breakers return fallback JSON when downstream services are unavailable.
- Kafka consumers use retry with fixed backoff.
- Failed Kafka records are recoverable through `orders.placed.DLQ`.
- Gateway rate limiting is backed by Redis.
- Inventory uses cache-aside reads for product lookup and evicts cache after stock changes.

## Observability

Every backend service exposes:

```text
/actuator/health
/actuator/prometheus
```

Prometheus config:

[observability/prometheus/prometheus.yml](observability/prometheus/prometheus.yml)

Suggested Grafana panels:

- HTTP request latency per service
- Request rate per endpoint
- Kafka listener throughput
- JVM memory usage
- Order creation throughput
- Inventory cache hit/miss ratio

## Build And Test

```powershell
cd orderflow
..\mvnw.cmd test
```

Frontend:

```powershell
cd orderflow/frontend
npm install
npm run build
```

## CI

GitHub Actions runs Maven tests for all backend modules:

[.github/workflows/ci.yml](.github/workflows/ci.yml)

## Deployment

### GitHub

This repository is ready to publish as a standalone project. The `.gitignore` excludes generated build output, logs, local environment files, and dependencies, while GitHub Actions runs the Maven test suite on every push and pull request.

### Vercel

Vercel hosts the React console from `frontend/`. The deployed console uses `VITE_DEMO_MODE=true` so visitors can click through the order workflow without needing local Docker services.

Live demo:

[https://orderflow-console-surya.vercel.app](https://orderflow-console-surya.vercel.app)

The complete backend stack runs through Docker Compose or any container platform that supports Java services, Kafka, Redis, and MySQL.

## Resume Bullets

- Architected OrderFlow, an event-driven microservices order management platform with Spring Boot, Spring Cloud Gateway, Eureka, Kafka, Redis, MySQL, Docker, and React.
- Implemented asynchronous order processing using Kafka topics `orders.placed`, `inventory.updated`, and `inventory.low-stock`, decoupling Order, Inventory, and Notification services.
- Added centralized JWT validation, user context propagation, Redis-backed rate limiting, gateway circuit breaker fallbacks, and Docker Compose orchestration.
- Implemented Redis cache-aside inventory lookup with cache eviction after stock updates.
- Added Kafka retry and dead-letter queue handling for resilient event consumption.
- Integrated Prometheus and Grafana observability using Spring Actuator and Micrometer metrics.
- Added a Vercel-deployed React console with a safe demo mode plus a local Docker mode connected to the real gateway.

## Interview Explanation

The key story is the before/after improvement. The first version placed an order by synchronously calling Inventory Service. That was simple, but tightly coupled: if Inventory was down, Order could not proceed, and Order had to wait for Inventory.

The final version uses Kafka. Order Service saves the order as `PENDING` and publishes `orders.placed`. Inventory Service consumes the event, updates stock in its own database, and publishes `inventory.updated`. Order Service consumes that result and marks the order `CONFIRMED` or `FAILED`. This makes the system more scalable, fault-tolerant, and closer to real production architecture.
