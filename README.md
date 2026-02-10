# APEX - AI-Assisted Algorithmic Trading Platform

A production-style monolithic Java Spring Boot application for algorithmic trading with AI-powered sentiment analysis.

## Architecture

APEX follows clean architecture principles with clear separation of concerns:

```
┌─────────────┐
│ Controllers │  REST API Layer
└──────┬──────┘
       │
┌──────▼──────┐
│  Services   │  Business Logic & Orchestration
└──────┬──────┘
       │
┌──────▼──────┐
│  Strategy   │  Trading Decision Logic
│    Risk     │  Risk Management
│  Execution  │  Order Execution
│  Portfolio  │  Position Management
└──────┬──────┘
       │
┌──────▼──────┐
│   Clients   │  External Integrations (Broker, AI)
│    Repos    │  Data Access Layer
└─────────────┘
```

## Tech Stack

- **Java 17** - Modern LTS version
- **Spring Boot 4.0.2** - Application framework
- **Spring Data JPA** - Database access
- **PostgreSQL** - Relational database
- **Lombok** - Boilerplate reduction
- **Maven** - Build tool

## Package Structure

```
com.example.Apex/
├── controller/      REST endpoints
├── service/         Business orchestration
├── strategy/        Trading strategies
├── risk/            Risk management
├── execution/       Order execution
├── portfolio/       Portfolio management
├── client/          External integrations (mock implementations)
├── repo/            Data access (Spring Data JPA)
├── model/           Entities & DTOs
├── config/          Application configuration
└── exception/       Custom exceptions
```

## Core Trading Flow

```
User Request
    ↓
Fetch Price (Broker)
    ↓
Get AI Sentiment
    ↓
Strategy Decision
    ↓
Risk Validation
    ↓
Execute Order (Broker)
    ↓
Update Portfolio
```

## Prerequisites

1. **Java 17** installed
2. **PostgreSQL** running locally
3. Database setup:
```sql
CREATE DATABASE apex_db;
```

## Configuration

Update `src/main/resources/application.properties` with your PostgreSQL credentials:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/apex_db
spring.datasource.username=your_username
spring.datasource.password=your_password
```

## Running the Application

```bash
# Compile
./mvnw clean compile

# Run
./mvnw spring-boot:run
```

The application will start on `http://localhost:8080`

## API Endpoints

### User Management

**Create User**
```bash
POST http://localhost:8080/user/create
Content-Type: application/json

{
  "username": "trader1",
  "email": "trader1@apex.com",
  "initialBalance": "50000.00"
}
```

**Get User**
```bash
GET http://localhost:8080/user/{userId}
```

### Trading

**Execute Trade**
```bash
POST http://localhost:8080/trade/execute
Content-Type: application/json

{
  "userId": 1,
  "symbol": "AAPL",
  "quantity": 10,
  "side": "BUY",
  "strategyType": "SIMPLE_MOMENTUM"
}
```

**Get Order History**
```bash
GET http://localhost:8080/trade/history/{userId}
```

### Portfolio

**Get Portfolio Summary**
```bash
GET http://localhost:8080/portfolio/{userId}
```

## Trading Strategies

### SIMPLE_MOMENTUM (default)
- Executes BUY on POSITIVE sentiment
- Skips on NEUTRAL or NEGATIVE

### CONSERVATIVE
- Executes only on POSITIVE sentiment
- Additional check: price < $300
- More risk-averse

## Risk Management

The `RiskGuard` enforces:
- Sufficient account balance
- Minimum balance requirement ($100)
- Daily trade limit (50 trades/day)

## Mock Implementations

### MockBrokerClient
- Returns random prices ($50-$500)
- Simulates successful order execution
- **Replace with real broker API in production**

### MockAIClient
- Returns random sentiment (POSITIVE/NEGATIVE/NEUTRAL)
- **Replace with real AI/ML service in production**

## Database Schema

**Users Table**
- id, username, email, balance, created_at

**Orders Table**
- id, user_id, symbol, quantity, price, side, status, created_at, executed_at

**Holdings Table**
- id, user_id, symbol, quantity, average_price

**Trade Logs Table**
- id, order_id, action, details, timestamp

## Extension Points

1. **Add Real Broker Integration**
   - Implement `BrokerClient` interface
   - Configure via `ClientConfig` with `@Profile("prod")`

2. **Add Real AI Service**
   - Implement `AIClient` interface
   - Integrate ML models or external APIs

3. **Add New Strategies**
   - Implement `TradingStrategy` interface
   - Register as Spring `@Component`

4. **Add Authentication**
   - Integrate Spring Security
   - Add JWT tokens

## Testing Example Flow

```bash
# 1. Create a user
curl -X POST http://localhost:8080/user/create \
  -H "Content-Type: application/json" \
  -d '{"username":"john","email":"john@test.com","initialBalance":"10000"}'

# 2. Execute a trade
curl -X POST http://localhost:8080/trade/execute \
  -H "Content-Type: application/json" \
  -d '{"userId":1,"symbol":"TSLA","quantity":5,"side":"BUY"}'

# 3. View portfolio
curl http://localhost:8080/portfolio/1

# 4. View order history
curl http://localhost:8080/trade/history/1
```

## Production Considerations

- Replace mock clients with real integrations
- Add comprehensive unit and integration tests
- Implement authentication and authorization
- Add rate limiting
- Set up monitoring and observability
- Configure connection pooling
- Add caching where appropriate
- Implement proper secrets management
- Set up CI/CD pipeline

## License

This is a demo/educational project.
# Apex
