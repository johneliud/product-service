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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Slf4j
public class ProductController {
    private final ProductService productService;

    @PostMapping
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @Valid @RequestBody ProductRequest request,
            Authentication authentication) {
        
        String userId = (String) authentication.getPrincipal();
        log.info("POST /api/products - Create product request by userId: {}", userId);
        
        ProductResponse productResponse = productService.createProduct(request, userId);
        
        log.info("POST /api/products - Product created successfully: {}", productResponse.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new ApiResponse<>(true, "Product created successfully", productResponse));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<java.util.List<ProductResponse>>> getAllProducts() {
        log.info("GET /api/products - Get all products request");
        
        java.util.List<ProductResponse> products = productService.getAllProducts();
        
        log.info("GET /api/products - Retrieved {} products", products.size());
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
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable String id,
            @Valid @RequestBody ProductRequest request,
            Authentication authentication) {
        
        String userId = (String) authentication.getPrincipal();
        log.info("PUT /api/products/{} - Update product request by userId: {}", id, userId);
        
        ProductResponse productResponse = productService.updateProduct(id, request, userId);
        
        log.info("PUT /api/products/{} - Product updated successfully", id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Product updated successfully", productResponse));
    }
}
