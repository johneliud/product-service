package io.github.johneliud.product_service.controllers;

import io.github.johneliud.product_service.dto.ApiResponse;
import io.github.johneliud.product_service.dto.StockUpdateRequest;
import io.github.johneliud.product_service.services.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/products")
@RequiredArgsConstructor
@Slf4j
public class InternalProductController {

    private final ProductService productService;

    @PatchMapping("/{id}/stock")
    public ResponseEntity<ApiResponse<Void>> decrementStock(
            @PathVariable String id,
            @Valid @RequestBody StockUpdateRequest request) {
        log.info("PATCH /internal/products/{}/stock - quantity: {}", id, request.getQuantity());
        productService.decrementStock(id, request.getQuantity());
        return ResponseEntity.ok(new ApiResponse<>(true, "Stock updated successfully", null));
    }
}
