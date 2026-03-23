package io.github.johneliud.product_service.services;

import io.github.johneliud.product_service.dto.PagedResponse;
import io.github.johneliud.product_service.dto.ProductRequest;
import io.github.johneliud.product_service.dto.ProductResponse;
import io.github.johneliud.product_service.event.ProductDeletedEvent;
import io.github.johneliud.product_service.models.Product;
import io.github.johneliud.product_service.repositories.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {
    private final ProductRepository productRepository;
    private final KafkaTemplate<String, ProductDeletedEvent> kafkaTemplate;
    private final MongoTemplate mongoTemplate;

    public ProductResponse createProduct(ProductRequest request, String userId) {
        log.info("Attempting to create product for userId: {}", userId);

        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setQuantity(request.getQuantity());
        product.setUserId(userId);
        product.setCategory(request.getCategory());

        Product savedProduct = productRepository.save(product);
        log.info("Product created successfully with ID: {} for userId: {}", savedProduct.getId(), userId);

        return toProductResponse(savedProduct);
    }

    public PagedResponse<ProductResponse> getAllProductsPaged(
            int page, int size, String search, BigDecimal minPrice, BigDecimal maxPrice,
            String sortBy, String sortDir, String category, boolean availableOnly, String sellerId) {

        log.info("Fetching paged products - page: {}, size: {}, search: {}, minPrice: {}, maxPrice: {}, " +
                "category: {}, availableOnly: {}, sellerId: {}, sortBy: {}, sortDir: {}",
                page, size, search, minPrice, maxPrice, category, availableOnly, sellerId, sortBy, sortDir);

        Pageable pageable = PageRequest.of(page, size, Sort.by(
                "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC, sortBy));

        Query query = buildFilterQuery(search, minPrice, maxPrice, category, availableOnly, sellerId);
        long total = mongoTemplate.count(Query.of(query).limit(-1).skip(-1), Product.class);
        List<Product> products = mongoTemplate.find(query.with(pageable), Product.class);
        Page<Product> productPage = new PageImpl<>(products, pageable, total);

        List<ProductResponse> content = productPage.getContent().stream()
                .map(this::toProductResponse)
                .collect(Collectors.toList());

        log.info("Retrieved {} products (page {}/{})", content.size(), page + 1, productPage.getTotalPages());

        return new PagedResponse<>(
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
        product.setCategory(request.getCategory());

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

        // Do not fail the delete operation if Kafka publish fails
        try {
            ProductDeletedEvent event = new ProductDeletedEvent(id, userId);
            kafkaTemplate.send("product-deleted", event);
            log.info("Published product-deleted event for productId: {}", id);
        } catch (Exception e) {
            log.error("Failed to publish product-deleted event for productId: {}, error: {}", id, e.getMessage());
        }
    }

    public List<ProductResponse> getSellerProducts(String userId) {
        log.info("Fetching products for userId: {}", userId);

        List<Product> products = productRepository.findByUserId(userId);

        log.info("Retrieved {} products for userId: {}", products.size(), userId);
        return products.stream()
                .map(this::toProductResponse)
                .collect(Collectors.toList());
    }

    public PagedResponse<ProductResponse> getSellerProductsPaged(
            String userId, int page, int size, String search, BigDecimal minPrice, BigDecimal maxPrice,
            String sortBy, String sortDir, String category, boolean availableOnly) {

        log.info("Fetching paged products for userId: {}, page: {}, size: {}, search: {}, minPrice: {}, " +
                "maxPrice: {}, category: {}, availableOnly: {}, sortBy: {}, sortDir: {}",
                userId, page, size, search, minPrice, maxPrice, category, availableOnly, sortBy, sortDir);

        Pageable pageable = PageRequest.of(page, size, Sort.by(
                "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC, sortBy));

        Query query = buildFilterQuery(search, minPrice, maxPrice, category, availableOnly, userId);
        long total = mongoTemplate.count(Query.of(query).limit(-1).skip(-1), Product.class);
        List<Product> products = mongoTemplate.find(query.with(pageable), Product.class);
        Page<Product> productPage = new PageImpl<>(products, pageable, total);

        List<ProductResponse> content = productPage.getContent().stream()
                .map(this::toProductResponse)
                .collect(Collectors.toList());

        log.info("Retrieved {} products (page {}/{}) for userId: {}",
                content.size(), page + 1, productPage.getTotalPages(), userId);

        return new PagedResponse<>(
                content,
                productPage.getNumber(),
                productPage.getSize(),
                productPage.getTotalElements(),
                productPage.getTotalPages(),
                productPage.isLast()
        );
    }

    public void decrementStock(String productId, int quantity) {
        log.info("Attempting to decrement stock for productId: {} by {}", productId, quantity);

        Query query = Query.query(
                Criteria.where("_id").is(productId).and("quantity").gte(quantity)
        );
        Update update = new Update().inc("quantity", -quantity);
        Product previous = mongoTemplate.findAndModify(query, update, Product.class);

        if (previous == null) {
            boolean exists = productRepository.existsById(productId);
            if (!exists) {
                log.warn("Stock decrement failed: product not found - {}", productId);
                throw new IllegalArgumentException("Product not found: " + productId);
            }
            log.warn("Stock decrement failed: insufficient stock for productId: {}, requested: {}", productId, quantity);
            throw new IllegalArgumentException("Insufficient stock for product: " + productId);
        }

        log.info("Stock decremented for productId: {} by {}. Previous quantity: {}", productId, quantity, previous.getQuantity());
    }

    private Query buildFilterQuery(String search, BigDecimal minPrice, BigDecimal maxPrice,
                                   String category, boolean availableOnly, String sellerId) {
        List<Criteria> criteriaList = new ArrayList<>();

        if (search != null && !search.isBlank()) {
            criteriaList.add(new Criteria().orOperator(
                    Criteria.where("name").regex(search, "i"),
                    Criteria.where("description").regex(search, "i")
            ));
        }
        if (minPrice != null) {
            criteriaList.add(Criteria.where("price").gte(minPrice));
        }
        if (maxPrice != null) {
            criteriaList.add(Criteria.where("price").lte(maxPrice));
        }
        if (category != null && !category.isBlank()) {
            criteriaList.add(Criteria.where("category").regex(category, "i"));
        }
        if (availableOnly) {
            criteriaList.add(Criteria.where("quantity").gt(0));
        }
        if (sellerId != null && !sellerId.isBlank()) {
            criteriaList.add(Criteria.where("userId").is(sellerId));
        }

        Criteria criteria = criteriaList.isEmpty() ? new Criteria()
                : new Criteria().andOperator(criteriaList.toArray(new Criteria[0]));
        return Query.query(criteria);
    }

    private ProductResponse toProductResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getQuantity(),
                product.getUserId(),
                product.getCategory()
        );
    }
}
