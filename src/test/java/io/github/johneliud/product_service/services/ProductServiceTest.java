package io.github.johneliud.product_service.services;

import io.github.johneliud.product_service.dto.ProductRequest;
import io.github.johneliud.product_service.dto.ProductResponse;
import io.github.johneliud.product_service.models.Product;
import io.github.johneliud.product_service.repositories.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private ProductService productService;

    private Product testProduct;
    private ProductRequest testRequest;

    @BeforeEach
    void setUp() {
        testProduct = new Product();
        testProduct.setId("prod123");
        testProduct.setName("Test Product");
        testProduct.setDescription("Test Description");
        testProduct.setPrice(new BigDecimal("99.99"));
        testProduct.setQuantity(10);
        testProduct.setUserId("seller123");

        testRequest = new ProductRequest();
        testRequest.setName("Test Product");
        testRequest.setDescription("Test Description");
        testRequest.setPrice(new BigDecimal("99.99"));
        testRequest.setQuantity(10);
    }

    @Test
    void createProduct_Success() {
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);

        ProductResponse response = productService.createProduct(testRequest, "seller123");

        assertNotNull(response);
        assertEquals("prod123", response.getId());
        assertEquals("Test Product", response.getName());
        assertEquals(new BigDecimal("99.99"), response.getPrice());
        assertEquals(10, response.getQuantity());
        assertEquals("seller123", response.getUserId());
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void getProductById_Success() {
        when(productRepository.findById("prod123")).thenReturn(Optional.of(testProduct));

        ProductResponse response = productService.getProductById("prod123");

        assertNotNull(response);
        assertEquals("prod123", response.getId());
        verify(productRepository).findById("prod123");
    }

    @Test
    void getProductById_NotFound_ThrowsException() {
        when(productRepository.findById("prod123")).thenReturn(Optional.empty());

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            productService.getProductById("prod123");
        });
        assertEquals("Product not found", exception.getMessage());
    }

    @Test
    void updateProduct_Success() {
        when(productRepository.findById("prod123")).thenReturn(Optional.of(testProduct));
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);

        ProductResponse response = productService.updateProduct("prod123", testRequest, "seller123");

        assertNotNull(response);
        assertEquals("prod123", response.getId());
        verify(productRepository).findById("prod123");
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void updateProduct_WrongOwner_ThrowsException() {
        when(productRepository.findById("prod123")).thenReturn(Optional.of(testProduct));

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            productService.updateProduct("prod123", testRequest, "wrongSeller");
        });
        assertEquals("You do not have permission to update this product", exception.getMessage());
    }

    @Test
    void updateProduct_NotFound_ThrowsException() {
        when(productRepository.findById("prod123")).thenReturn(Optional.empty());

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            productService.updateProduct("prod123", testRequest, "seller123");
        });
        assertEquals("Product not found", exception.getMessage());
    }

//    @Test
//    void deleteProduct_Success() {
//        when(productRepository.findById("prod123")).thenReturn(Optional.of(testProduct));
//
//        productService.deleteProduct("prod123", "seller123");
//
//        verify(productRepository).findById("prod123");
//        verify(productRepository).deleteById("prod123");
//    }

    @Test
    void deleteProduct_WrongOwner_ThrowsException() {
        when(productRepository.findById("prod123")).thenReturn(Optional.of(testProduct));

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            productService.deleteProduct("prod123", "wrongSeller");
        });
        assertEquals("You do not have permission to delete this product", exception.getMessage());
    }

    @Test
    void deleteProduct_NotFound_ThrowsException() {
        when(productRepository.findById("prod123")).thenReturn(Optional.empty());

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            productService.deleteProduct("prod123", "seller123");
        });
        assertEquals("Product not found", exception.getMessage());
    }

    @Test
    void decrementStock_Success() {
        when(mongoTemplate.findAndModify(any(), any(), eq(Product.class))).thenReturn(testProduct);

        assertDoesNotThrow(() -> productService.decrementStock("prod123", 3));
        verify(mongoTemplate).findAndModify(any(), any(), eq(Product.class));
    }

    @Test
    void decrementStock_InsufficientStock_ThrowsException() {
        when(mongoTemplate.findAndModify(any(), any(), eq(Product.class))).thenReturn(null);
        when(productRepository.existsById("prod123")).thenReturn(true);

        assertThatThrownBy(() -> productService.decrementStock("prod123", 15))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Insufficient stock");
    }

    @Test
    void decrementStock_ProductNotFound_ThrowsException() {
        when(mongoTemplate.findAndModify(any(), any(), eq(Product.class))).thenReturn(null);
        when(productRepository.existsById("prod123")).thenReturn(false);

        assertThatThrownBy(() -> productService.decrementStock("prod123", 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Product not found");
    }
}