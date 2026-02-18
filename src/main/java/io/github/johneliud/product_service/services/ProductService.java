package io.github.johneliud.product_service.services;

import io.github.johneliud.product_service.dto.ProductRequest;
import io.github.johneliud.product_service.dto.ProductResponse;
import io.github.johneliud.product_service.event.ProductDeletedEvent;
import io.github.johneliud.product_service.models.Product;
import io.github.johneliud.product_service.repositories.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {
    private final ProductRepository productRepository;
    private final KafkaTemplate<String, ProductDeletedEvent> kafkaTemplate;

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

    public io.github.johneliud.product_service.dto.PagedResponse<ProductResponse> getAllProductsPaged(
            int page, 
            int size, 
            String search, 
            java.math.BigDecimal minPrice, 
            java.math.BigDecimal maxPrice, 
            String sortBy, 
            String sortDir) {
        
        log.info("Fetching paged products - page: {}, size: {}, search: {}, minPrice: {}, maxPrice: {}, sortBy: {}, sortDir: {}", 
                page, size, search, minPrice, maxPrice, sortBy, sortDir);
        
        org.springframework.data.domain.Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) 
            ? org.springframework.data.domain.Sort.Direction.DESC 
            : org.springframework.data.domain.Sort.Direction.ASC;
        
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(
            page, size, org.springframework.data.domain.Sort.by(direction, sortBy)
        );
        
        org.springframework.data.domain.Page<Product> productPage;
        
        if (search != null && !search.isBlank() && minPrice != null && maxPrice != null) {
            productPage = productRepository.findByNameContainingIgnoreCaseAndPriceBetween(
                search, minPrice, maxPrice, pageable
            );
        } else if (search != null && !search.isBlank()) {
            productPage = productRepository.findByNameContainingIgnoreCase(search, pageable);
        } else if (minPrice != null && maxPrice != null) {
            productPage = productRepository.findByPriceBetween(minPrice, maxPrice, pageable);
        } else {
            productPage = productRepository.findAll(pageable);
        }
        
        java.util.List<ProductResponse> content = productPage.getContent().stream()
            .map(this::toProductResponse)
            .collect(java.util.stream.Collectors.toList());
        
        log.info("Retrieved {} products (page {}/{})", 
                content.size(), page + 1, productPage.getTotalPages());
        
        return new io.github.johneliud.product_service.dto.PagedResponse<>(
            content,
            productPage.getNumber(),
            productPage.getSize(),
            productPage.getTotalElements(),
            productPage.getTotalPages(),
            productPage.isLast()
        );
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

    public ProductResponse updateProduct(String id, ProductRequest request, String userId) {
        log.info("Attempting to update product ID: {} by userId: {}", id, userId);
        
        Product product = productRepository.findById(id)
            .orElseThrow(() -> {
                log.warn("Product update failed: Product not found - {}", id);
                return new IllegalArgumentException("Product not found");
            });
        
        if (!product.getUserId().equals(userId)) {
            log.warn("Product update failed: User {} does not own product {}", userId, id);
            throw new IllegalArgumentException("You do not have permission to update this product");
        }
        
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setQuantity(request.getQuantity());
        
        Product updatedProduct = productRepository.save(product);
        log.info("Product updated successfully: {}", id);
        
        return toProductResponse(updatedProduct);
    }

    public void deleteProduct(String id, String userId) {
        log.info("Attempting to delete product ID: {} by userId: {}", id, userId);
        
        Product product = productRepository.findById(id)
            .orElseThrow(() -> {
                log.warn("Product deletion failed: Product not found - {}", id);
                return new IllegalArgumentException("Product not found");
            });
        
        if (!product.getUserId().equals(userId)) {
            log.warn("Product deletion failed: User {} does not own product {}", userId, id);
            throw new IllegalArgumentException("You do not have permission to delete this product");
        }
        
        productRepository.deleteById(id);
        log.info("Product deleted successfully: {}", id);
        
        // Publish product deleted event
        ProductDeletedEvent event = new ProductDeletedEvent(id, userId);
        kafkaTemplate.send("product-deleted", event);
        log.info("Published product-deleted event for productId: {}", id);
    }

    public java.util.List<ProductResponse> getSellerProducts(String userId) {
        log.info("Fetching products for userId: {}", userId);
        
        java.util.List<Product> products = productRepository.findByUserId(userId);
        
        log.info("Retrieved {} products for userId: {}", products.size(), userId);
        return products.stream()
            .map(this::toProductResponse)
            .collect(java.util.stream.Collectors.toList());
    }

    public io.github.johneliud.product_service.dto.PagedResponse<ProductResponse> getSellerProductsPaged(
            String userId, 
            int page, 
            int size, 
            String search, 
            java.math.BigDecimal minPrice, 
            java.math.BigDecimal maxPrice, 
            String sortBy, 
            String sortDir) {
        
        log.info("Fetching paged products for userId: {}, page: {}, size: {}, search: {}, minPrice: {}, maxPrice: {}, sortBy: {}, sortDir: {}", 
                userId, page, size, search, minPrice, maxPrice, sortBy, sortDir);
        
        org.springframework.data.domain.Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) 
            ? org.springframework.data.domain.Sort.Direction.DESC 
            : org.springframework.data.domain.Sort.Direction.ASC;
        
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(
            page, size, org.springframework.data.domain.Sort.by(direction, sortBy)
        );
        
        org.springframework.data.domain.Page<Product> productPage;
        
        if (search != null && !search.isBlank() && minPrice != null && maxPrice != null) {
            productPage = productRepository.findByUserIdAndNameContainingIgnoreCaseAndPriceBetween(
                userId, search, minPrice, maxPrice, pageable
            );
        } else if (search != null && !search.isBlank()) {
            productPage = productRepository.findByUserIdAndNameContainingIgnoreCase(userId, search, pageable);
        } else if (minPrice != null && maxPrice != null) {
            productPage = productRepository.findByUserIdAndPriceBetween(userId, minPrice, maxPrice, pageable);
        } else {
            productPage = productRepository.findByUserId(userId, pageable);
        }
        
        java.util.List<ProductResponse> content = productPage.getContent().stream()
            .map(this::toProductResponse)
            .collect(java.util.stream.Collectors.toList());
        
        log.info("Retrieved {} products (page {}/{}) for userId: {}", 
                content.size(), page + 1, productPage.getTotalPages(), userId);
        
        return new io.github.johneliud.product_service.dto.PagedResponse<>(
            content,
            productPage.getNumber(),
            productPage.getSize(),
            productPage.getTotalElements(),
            productPage.getTotalPages(),
            productPage.isLast()
        );
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
