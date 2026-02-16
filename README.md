# Product Service

A Spring Boot microservice for managing products in the marketplace, supporting CRUD operations with role-based access control where only authenticated sellers can create, update, and delete their products.

## Table of Contents
- [Features](#features)
- [Tech Stack](#tech-stack)
- [Getting Started](#getting-started)
- [API Documentation](#api-documentation)
- [Testing](#testing)

## Features

### Completed Implementations

#### PS-1: Database Schema Design & Implementation
- Product model with MongoDB document mapping
- Fields: id, name, description, price, quantity, userId
- Indexed userId for query optimization
- ProductRepository with userId lookup methods

#### PS-2: Create Product API (Sellers Only)
- POST /api/products endpoint
- JWT token validation and user extraction
- Seller-only access with @PreAuthorize
- Product associated with seller's userId
- Comprehensive field validation (name, description, price, quantity)

#### PS-3: Read Products API
- GET /api/products endpoint (list all products)
- GET /api/products/{id} endpoint (get single product)
- Public access (no authentication required)
- Returns product data with seller information

#### PS-4: Update Product API (Sellers Only)
- PUT /api/products/{id} endpoint
- Ownership verification (seller must own product)
- Prevents sellers from modifying other sellers' products
- Full field validation

#### PS-5: Delete Product API (Sellers Only)
- DELETE /api/products/{id} endpoint
- Ownership verification before deletion
- Prevents sellers from deleting other sellers' products
- Handles cascading deletion considerations

#### PS-6: Seller Product Dashboard API
- GET /api/products/my-products endpoint
- Returns only products owned by authenticated seller
- Pagination support (10 products per page)
- Search by product name
- Filter by price range (minPrice, maxPrice)
- Sort by name, price, or quantity (ascending/descending)

#### PS-7: Authorization & Access Control
- JWT validation for protected endpoints
- JwtUtil and JwtAuthenticationFilter implementation
- Seller role verification from JWT token
- Ownership verification for product modifications
- 403 Forbidden for clients attempting seller operations
- Public access to read endpoints

#### PS-8: Error Handling & Validation
- GlobalExceptionHandler with consistent error responses
- Price validation (must be positive > 0.01)
- Quantity validation (must be non-negative >= 0)
- Product not found handling
- Unauthorized access handling
- ErrorResponse DTO for consistent format

#### PS-10: Unit & Integration Testing
- ProductService unit tests (CRUD operations)
- Ownership verification tests
- Role-based scenario tests
- 10 tests passing with full coverage

### Pending Implementations

#### PS-9: Kafka Integration
- Kafka producer for product events
- Publish events for product creation, update, deletion
- Configure Kafka topics for inter-service communication

## Tech Stack

- **Java 25**
- **Spring Boot 4.0.2**
- **MongoDB** - Database
- **Spring Security** - Authentication & Authorization
- **JWT (JJWT 0.12.5)** - Token-based authentication
- **Lombok** - Boilerplate reduction
- **Maven** - Build tool

## Getting Started

### Prerequisites

- Java 25 or higher
- Maven 3.6+
- MongoDB Atlas account or local MongoDB instance

### Clone Repository

```bash
git clone https://github.com/johneliud/product-service.git
cd product-service
```

### Configuration

Create and Update `src/main/resources/application-secrets.properties`:

```properties
spring.mongodb.uri=mongodb+srv://<username>:<password>@<cluster>.mongodb.net/<database>
# Server Configuration
server.port=8082
# JWT Configuration
jwt.secret=<your-secret-key>
jwt.expiration=86400000
# Logging Configuration
logging.level.io.github.johneliud.product_service=INFO
logging.level.org.springframework.security=WARN
logging.level.org.springframework.data.mongodb=INFO
```

### Build & Run

```bash
# Build
./mvnw clean install

# Run
./mvnw spring-boot:run
```

The service will start on `http://localhost:8082`

### Run Tests

```bash
./mvnw test
```

## API Documentation

See [API_TESTING.md](docs/API_TESTING.md) for detailed endpoint documentation with Postman/Insomnia examples and cURL commands.

### Quick Reference

**Public Endpoints:**
- `GET /api/products` - List all products
- `GET /api/products/{id}` - Get single product

**Protected Endpoints (Require JWT - SELLER role):**
- `POST /api/products` - Create product
- `PUT /api/products/{id}` - Update product (owner only)
- `DELETE /api/products/{id}` - Delete product (owner only)
- `GET /api/products/my-products` - Get seller's products with pagination/filtering

## Testing

The project includes:
- ProductService unit tests (CRUD, ownership verification)
- Role-based access tests
- 10 tests with full coverage

Run tests with: `./mvnw test`
