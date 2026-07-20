# Standalone Spring Boot Banking API

This project is a standalone Java Spring Boot API example for a banking domain.

- No database is used.
- All data is stored in memory (`ConcurrentHashMap` + in-memory lists).
- Data resets every time the app restarts.

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

## Example Endpoints

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

- This is an educational starter API and omits authentication/authorization.
- `BigDecimal` is used for money handling with 2 decimal places.
- Transfer locking is implemented to avoid race conditions in this in-memory example.
# Nexapay
# Nexapay
