package io.github.johneliud.product_service.services;

import io.github.johneliud.product_service.dto.ProductRequest;
import io.github.johneliud.product_service.dto.ProductResponse;
import io.github.johneliud.product_service.models.Product;
import io.github.johneliud.product_service.repositories.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {
    private final ProductRepository productRepository;

    public ProductResponse createProduct(ProductRequest request, String userId) {
        log.info("Attempting to create product for userId: {}", userId);

        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setQuantity(request.getQuantity());
        product.setUserId(userId);

        Product savedProduct = productRepository.save(product);
        log.info("Product created successfully with ID: {} for userId: {}", savedProduct.getId(), userId);

        return toProductResponse(savedProduct);
    }

    public java.util.List<ProductResponse> getAllProducts() {
        log.info("Fetching all products");
        
        java.util.List<Product> products = productRepository.findAll();
        
        log.info("Retrieved {} products", products.size());
        return products.stream()
            .map(this::toProductResponse)
            .collect(java.util.stream.Collectors.toList());
    }

    public ProductResponse getProductById(String id) {
        log.info("Fetching product by ID: {}", id);
        
        Product product = productRepository.findById(id)
            .orElseThrow(() -> {
                log.warn("Product not found with ID: {}", id);
                return new IllegalArgumentException("Product not found");
            });
        
        log.info("Product retrieved successfully: {}", id);
        return toProductResponse(product);
    }

    private ProductResponse toProductResponse(Product product) {
        return new ProductResponse(
            product.getId(),
            product.getName(),
            product.getDescription(),
            product.getPrice(),
            product.getQuantity(),
            product.getUserId()
        );
    }
}
