# Product Service

Microservice responsible for product management and catalog operations.

## Overview

- **Port**: 8082
- **Technology**: Spring Boot 4.0.3
- **Database**: MongoDB Atlas (collection `products`)
- **Purpose**: Product CRUD, filtering, sorting, and pagination

## Features

### Product Management
- Create products (sellers only)
- Update products (sellers only)
- Delete products (sellers only)
- View all products (public)
- View seller's products

### Filtering & Sorting
- Filter by name (search)
- Filter by price range (min/max)
- Sort by name or price
- Sort direction (ascending/descending)
- Pagination support

## API Endpoints

### Public Endpoints

#### Get All Products
```http
GET /api/products?page=0&size=10&search=laptop&minPrice=100&maxPrice=2000&sortBy=price&sortDir=asc
```

Query Parameters:
- `page` - Page number (default: 0)
- `size` - Page size (default: 10)
- `search` - Search term for product name
- `minPrice` - Minimum price filter
- `maxPrice` - Maximum price filter
- `sortBy` - Sort field: name or price (default: name)
- `sortDir` - Sort direction: asc or desc (default: asc)

Response:
```json
{
  "success": true,
  "message": "Products retrieved successfully",
  "data": {
    "content": [
      {
        "id": "...",
        "name": "Product Name",
        "description": "Description",
        "price": 1299.99,
        "quantity": 10,
        "userId": "seller-id"
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 25,
    "totalPages": 3,
    "last": false
  }
}
```

#### Get Product by ID
```http
GET /api/products/{id}
```

### Protected Endpoints (Sellers Only)

Require `Authorization: Bearer <token>` header and X-User-Id, X-User-Role headers (added by gateway).

#### Get Seller's Products
```http
GET /api/products/my-products?page=0&size=10
```

#### Create Product
```http
POST /api/products
Content-Type: application/json

{
  "name": "Product Name",
  "description": "Product description",
  "price": 1299.99,
  "quantity": 10
}
```

#### Update Product
```http
PUT /api/products/{id}
Content-Type: application/json

{
  "name": "Updated Name",
  "description": "Updated description",
  "price": 1499.99,
  "quantity": 15
}
```

#### Delete Product
```http
DELETE /api/products/{id}
```

This triggers a Kafka event that notifies Media Service to delete associated media files.

## Data Model

### Product
```json
{
  "id": "string",
  "name": "string",
  "description": "string",
  "price": "number (BigDecimal)",
  "quantity": "number (Integer)",
  "userId": "string (seller's user ID)"
}
```

## Configuration

### Application Properties
```properties
server.port=8082
spring.data.mongodb.uri=mongodb://localhost:27017/buy01
```

## Running the Service

```bash
cd backend/product-service
mvn spring-boot:run
```

Ensure MongoDB is running on port 27017.

## Filtering Logic

The service supports flexible filtering:
- **Search only**: Filters by name containing search term
- **Price only**: Filters by price range (min, max, or both)
- **Search + Price**: Combines both filters
- **No filters**: Returns all products

Examples:
- `?maxPrice=1500` - Products ≤ 1500
- `?minPrice=500` - Products ≥ 500
- `?minPrice=500&maxPrice=1500` - Products between 500-1500
- `?search=laptop&maxPrice=2000` - Laptops under 2000

## Security

- Only sellers can create, update, or delete products
- Sellers can only modify their own products
- Product ownership verified via userId field
- Role verification via X-User-Role header

## Dependencies

- Spring Boot 4.0.3
- Spring Data MongoDB
- Spring Kafka 4.0.3
- Lombok
- Validation API

## Kafka Integration

### Producer
Publishes to `product-deleted` when a seller deletes a product.

**Event**:
```json
{
  "productId": "string",
  "timestamp": "ISO-8601 datetime"
}
```

Producer configuration includes `RETRIES_CONFIG = 3` and `RETRY_BACKOFF_MS_CONFIG = 1000ms`.

### Consumer
Subscribes to `order-placed` (consumer group: `product-service`) to update stock counters.

Consumer configuration uses `StringDeserializer` + `StringJsonMessageConverter` with `DefaultErrorHandler(FixedBackOff(1000ms, 3 retries))`.

```properties
spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.consumer.group-id=product-service
```

## Error Responses

```json
{
  "success": false,
  "message": "Error message",
  "data": null
}
```

Common errors:
- 400 - Invalid request data
- 403 - Not authorized (not a seller or not product owner)
- 404 - Product not found

## Database Indexes

Recommended indexes for performance:
```javascript
db.products.createIndex({ "name": "text" })
db.products.createIndex({ "price": 1 })
db.products.createIndex({ "userId": 1 })
```
