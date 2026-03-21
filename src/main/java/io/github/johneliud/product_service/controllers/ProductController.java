package io.github.johneliud.product_service.controllers;

import io.github.johneliud.product_service.dto.ApiResponse;
import io.github.johneliud.product_service.dto.ProductRequest;
import io.github.johneliud.product_service.dto.ProductResponse;
import io.github.johneliud.product_service.services.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Slf4j
public class ProductController {
    private final ProductService productService;

    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @Valid @RequestBody ProductRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        
        log.info("POST /api/products - Create product request - userId: {}, role: {}, request: {}", userId, role, request);
        
        if (userId == null || role == null) {
            log.error("Missing required headers - X-User-Id: {}, X-User-Role: {}", userId, role);
            throw new IllegalArgumentException("Authentication required");
        }
        
        if (!role.equals("SELLER")) {
            throw new IllegalArgumentException("Only sellers can create products");
        }
        
        ProductResponse productResponse = productService.createProduct(request, userId);
        
        log.info("POST /api/products - Product created successfully: {}", productResponse.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new ApiResponse<>(true, "Product created successfully", productResponse));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<io.github.johneliud.product_service.dto.PagedResponse<ProductResponse>>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) java.math.BigDecimal minPrice,
            @RequestParam(required = false) java.math.BigDecimal maxPrice,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "false") boolean availableOnly,
            @RequestParam(required = false) String sellerId) {

        log.info("GET /api/products - Get all products request with filters");

        io.github.johneliud.product_service.dto.PagedResponse<ProductResponse> products =
            productService.getAllProductsPaged(page, size, search, minPrice, maxPrice, sortBy, sortDir,
                    category, availableOnly, sellerId);

        log.info("GET /api/products - Retrieved {} products", products.getContent().size());
        return ResponseEntity.ok(new ApiResponse<>(true, "Products retrieved successfully", products));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(@PathVariable String id) {
        log.info("GET /api/products/{} - Get product by ID request", id);
        
        ProductResponse productResponse = productService.getProductById(id);
        
        log.info("GET /api/products/{} - Product retrieved successfully", id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Product retrieved successfully", productResponse));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable String id,
            @Valid @RequestBody ProductRequest request,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String role) {
        
        if (!role.equals("SELLER")) {
            throw new IllegalArgumentException("Only sellers can update products");
        }
        
        log.info("PUT /api/products/{} - Update product request by userId: {}", id, userId);
        
        ProductResponse productResponse = productService.updateProduct(id, request, userId);
        
        log.info("PUT /api/products/{} - Product updated successfully", id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Product updated successfully", productResponse));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(
            @PathVariable String id,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String role) {
        
        if (!role.equals("SELLER")) {
            throw new IllegalArgumentException("Only sellers can delete products");
        }
        
        log.info("DELETE /api/products/{} - Delete product request by userId: {}", id, userId);
        
        productService.deleteProduct(id, userId);
        
        log.info("DELETE /api/products/{} - Product deleted successfully", id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Product deleted successfully", null));
    }

    @GetMapping("/my-products")
    public ResponseEntity<ApiResponse<io.github.johneliud.product_service.dto.PagedResponse<ProductResponse>>> getSellerProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) java.math.BigDecimal minPrice,
            @RequestParam(required = false) java.math.BigDecimal maxPrice,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "false") boolean availableOnly,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String role) {

        if (!role.equals("SELLER")) {
            throw new IllegalArgumentException("Only sellers can access this endpoint");
        }

        log.info("GET /api/products/my-products - Get seller products request by userId: {}", userId);

        io.github.johneliud.product_service.dto.PagedResponse<ProductResponse> products =
            productService.getSellerProductsPaged(userId, page, size, search, minPrice, maxPrice, sortBy, sortDir,
                    category, availableOnly);

        log.info("GET /api/products/my-products - Retrieved {} products", products.getContent().size());
        return ResponseEntity.ok(new ApiResponse<>(true, "Products retrieved successfully", products));
    }
}
