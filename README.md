# Pharmacy Management API

A production-ready REST API for pharmacy inventory management, built with **Spring Boot 3.2** and **raw JDBC**. Manage medicines, categories, stock movements, and generate business reports — all through a clean, well-structured API.

---

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                      Client (Postman / Frontend)        │
└──────────────────────────┬──────────────────────────────┘
                           │ HTTP/JSON
┌──────────────────────────▼──────────────────────────────┐
│                     Controllers                         │
│  CategoryController · MedicineController                │
│  StockController    · ReportController                  │
├─────────────────────────────────────────────────────────┤
│                      Services                           │
│  CategoryService · MedicineService · StockService       │
│              (Business Logic + Validation)               │
├─────────────────────────────────────────────────────────┤
│                    Repositories                         │
│  CategoryRepo · MedicineRepo · StockMovementRepo        │
│              (Raw SQL via JdbcTemplate)                  │
├─────────────────────────────────────────────────────────┤
│                    MySQL Database                        │
│       med_categories · medicines · stock_movements       │
└─────────────────────────────────────────────────────────┘
```

## Tech Stack

| Layer          | Technology                     |
|----------------|--------------------------------|
| Framework      | Spring Boot 3.2.5              |
| Language       | Java 17+                       |
| Database       | MySQL 8.0                      |
| Data Access    | Spring JDBC (JdbcTemplate)     |
| Validation     | Jakarta Bean Validation        |
| Build Tool     | Maven                          |
| Server         | Embedded Apache Tomcat         |

## Prerequisites

- **Java** 17 or higher
- **Maven** 3.8+
- **MySQL** 8.0+



## API Reference

All responses follow a unified format:

```json
{
  "success": true,
  "message": "OK",
  "data": { }
}
```

### Categories

| Method   | Endpoint              | Description            |
|----------|-----------------------|------------------------|
| `GET`    | `/api/categories`     | List all categories    |
| `GET`    | `/api/categories/{id}`| Get category by ID     |
| `POST`   | `/api/categories`     | Create new category    |
| `PUT`    | `/api/categories/{id}`| Update category        |
| `DELETE` | `/api/categories/{id}`| Delete category        |

**Request body** (POST / PUT):
```json
{
  "name": "Painkillers",
  "description": "Pain relief medication"
}
```

---

### Medicines

| Method   | Endpoint                              | Description              |
|----------|---------------------------------------|--------------------------|
| `GET`    | `/api/medicines`                      | List all medicines       |
| `GET`    | `/api/medicines/{id}`                 | Get medicine by ID       |
| `GET`    | `/api/medicines/search?q={keyword}`   | Search by name or code   |
| `GET`    | `/api/medicines/category/{categoryId}`| Filter by category      |
| `GET`    | `/api/medicines/low-stock?threshold=10`| Get low stock items     |
| `POST`   | `/api/medicines`                      | Create new medicine      |
| `PUT`    | `/api/medicines/{id}`                 | Update medicine          |
| `DELETE` | `/api/medicines/{id}`                 | Delete medicine          |

**Request body** (POST / PUT):
```json
{
  "code": "ASP-500",
  "name": "Aspirin 500mg",
  "price": 3.50,
  "categoryId": 1
}
```

---

### Stock Movements

| Method   | Endpoint                                    | Description                   |
|----------|---------------------------------------------|-------------------------------|
| `POST`   | `/api/stock/movement`                       | Record stock movement (IN/OUT)|
| `GET`    | `/api/stock/movements?limit=50`             | Get recent movements          |
| `GET`    | `/api/stock/movements/medicine/{medicineId}` | Movements by medicine        |
| `GET`    | `/api/stock/movements/range?from=...&to=...`| Movements by date range       |

**Request body** (POST):
```json
{
  "medicineId": 1,
  "type": "IN",
  "quantity": 100,
  "note": "Supplier delivery"
}
```

> The `stock_qty` of the medicine is automatically updated when a movement is recorded. OUT movements are rejected if insufficient stock is available.

---

### Reports

| Method   | Endpoint                                 | Description                        |
|----------|------------------------------------------|------------------------------------|
| `GET`    | `/api/reports/stock-summary`             | Overall stock overview             |
| `GET`    | `/api/reports/category-stats`            | Statistics per category            |
| `GET`    | `/api/reports/monthly-movements?months=6`| Monthly IN vs OUT summary          |

**Stock Summary response:**
```json
{
  "totalMedicines": 45,
  "outOfStock": 3,
  "lowStock": 8,
  "totalStockValue": 12540.00
}
```

**Category Stats response:**
```json
{
  "categoryId": 1,
  "categoryName": "Painkillers",
  "medicineCount": 12,
  "totalStock": 580,
  "totalValue": 4350.00
}
```

---

## Project Structure

```
src/main/java/com/pharmacy/
│
├── PharmacyApplication.java          # Application entry point
│
├── controller/                       # REST endpoints
│   ├── CategoryController.java       #   /api/categories
│   ├── MedicineController.java       #   /api/medicines
│   ├── StockController.java          #   /api/stock
│   └── ReportController.java         #   /api/reports
│
├── service/                          # Business logic layer
│   ├── CategoryService.java
│   ├── MedicineService.java
│   └── StockService.java             #   @Transactional stock management
│
├── repository/                       # Data access layer (raw JDBC)
│   ├── CategoryRepository.java
│   ├── MedicineRepository.java
│   └── StockMovementRepository.java
│
├── model/                            # Domain entities
│   ├── MedCategory.java
│   ├── Medicine.java
│   └── StockMovement.java
│
├── dto/                              # Request/Response objects
│   └── Dtos.java                     #   Java records with validation
│
└── exception/                        # Error handling
    ├── ResourceNotFoundException.java
    ├── BusinessException.java
    └── GlobalExceptionHandler.java   #   Unified error responses
```
## Docker

Run everything with one command:

```bash
docker-compose up --build
```

Opens at `http://localhost`. Runs MySQL, Spring Boot, and React in containers. No local installations needed.

```bash
docker-compose down -v   # Stop and reset data
```
##  Design Decisions

| Decision | Rationale |
|----------|-----------|
| **Raw JDBC (JdbcTemplate)** | Full control over SQL queries with no ORM overhead. Every query is explicit and optimized. |
| **Java Records for DTOs** | Immutable, concise request/response objects with built-in validation annotations. |
| **@Transactional on StockService** | Ensures atomicity — if the movement record fails, the stock quantity rolls back automatically. |
| **Global Exception Handler** | Every error returns a consistent JSON response instead of raw stack traces. |
| **JOIN queries in repositories** | Medicines always return with their category name, avoiding N+1 query problems. |
| **Unified ApiResponse wrapper** | All endpoints return the same `{success, message, data}` structure for predictable client-side handling. |

## Error Handling

The API returns structured error responses for all failure scenarios:

| Status | Scenario | Example |
|--------|----------|---------|
| `400`  | Validation failure | `"Validation failed: Name is required"` |
| `400`  | Business rule violation | `"Insufficient stock. Available: 5, requested: 20"` |
| `404`  | Resource not found | `"Medicine not found: 99"` |
| `500`  | Unexpected error | `"Internal error: ..."` |
