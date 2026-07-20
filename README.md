# Standalone Spring Boot Banking API

This project is a standalone Java Spring Boot API example for a banking domain.

- No database is used.
- All data is stored in memory (`ConcurrentHashMap` + in-memory lists).
- Data resets every time the app restarts.

## Seed Data

On startup the API loads 10 customers, one account per customer, and 10
transactions per account. Customer ids run `1001`–`1010` and account ids run
`500001`–`500010`, so the examples below work against a freshly started app.

The generator uses a fixed random seed, so every restart produces identical
data. Balances are always consistent with the transaction history because the
seeder goes through the same service the API uses.

Set `nexapay.seed.enabled=false` to start with an empty store:

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments=--nexapay.seed.enabled=false
```

## Tech Stack

- Java 21
- Spring Boot 3
- Maven (via the included wrapper — no local Maven install needed)

## Run

```bash
./mvnw spring-boot:run
```

The wrapper downloads the correct Maven version on first run, so you only
need a Java 21 JDK on your `PATH`. On Windows, use `mvnw.cmd spring-boot:run`.

If you have Maven installed globally, plain `mvn spring-boot:run` also works.

The API starts at:

- `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## Run with Docker

No local JDK or Maven is needed; the image builds the app itself.

```bash
docker build -t nexapay-banking-api .
docker run --rm -p 8080:8080 nexapay-banking-api
```

Configuration is passed as environment variables, and JVM flags via
`JAVA_OPTS`:

```bash
docker run --rm -p 8080:8080 \
  -e NEXAPAY_SEED_ENABLED=false \
  -e JAVA_OPTS="-XX:MaxRAMPercentage=50.0" \
  nexapay-banking-api
```

Notes on the image:

- Multi-stage build on Alpine: the JDK and Maven stay in the build stage, and
  only a JRE ships in the final image (~336MB).
- The fat jar is split into Spring Boot layers so dependencies cache
  separately from application code. Editing a source file rebuilds in seconds
  rather than re-resolving dependencies.
- Runs as a non-root `spring` user, with `java` as PID 1 so `docker stop`
  shuts down gracefully.
- `HEALTHCHECK` polls `/api/health`, so `docker ps` reports real readiness.

## Example Endpoints

### Discover Available Endpoints

`GET /` redirects to `/api`, which lists every available endpoint. The list is
generated from the application's actual route mappings, so it stays accurate.

```bash
curl -L http://localhost:8080/
# or directly
curl http://localhost:8080/api
```

### Health

```bash
curl http://localhost:8080/api/health
```

### Create Customer

```bash
curl -X POST http://localhost:8080/api/customers \
  -H "Content-Type: application/json" \
  -d '{
    "fullName": "Ava Carter",
    "email": "ava.carter@example.com"
  }'
```

### Open Account

```bash
curl -X POST http://localhost:8080/api/accounts \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": 1001,
    "type": "CHECKING",
    "initialDeposit": 500.00,
    "currency": "USD"
  }'
```

### Deposit

```bash
curl -X POST http://localhost:8080/api/accounts/500001/deposit \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 150.00,
    "description": "Payroll"
  }'
```

### Withdraw

```bash
curl -X POST http://localhost:8080/api/accounts/500001/withdraw \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 25.00,
    "description": "ATM cash"
  }'
```

### Transfer Between Accounts

```bash
curl -X POST http://localhost:8080/api/transfers \
  -H "Content-Type: application/json" \
  -d '{
    "fromAccountId": 500001,
    "toAccountId": 500002,
    "amount": 40.00,
    "description": "Rent split"
  }'
```

### List Customer Accounts

```bash
curl http://localhost:8080/api/accounts/customer/1001
```

### List Account Transactions

```bash
curl http://localhost:8080/api/accounts/500001/transactions
```

## Notes
