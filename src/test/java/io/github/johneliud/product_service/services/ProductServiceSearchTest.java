package io.github.johneliud.product_service.services;

import io.github.johneliud.product_service.dto.PagedResponse;
import io.github.johneliud.product_service.dto.ProductResponse;
import io.github.johneliud.product_service.models.Product;
import io.github.johneliud.product_service.repositories.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceSearchTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private ProductService productService;

    private Product product(String id, String name, String description, String category, int qty) {
        Product p = new Product();
        p.setId(id);
        p.setName(name);
        p.setDescription(description);
        p.setPrice(new BigDecimal("50.00"));
        p.setQuantity(qty);
        p.setUserId("seller1");
        p.setCategory(category);
        return p;
    }

    private void stubMongoTemplate(List<Product> results) {
        when(mongoTemplate.count(any(Query.class), eq(Product.class))).thenReturn((long) results.size());
        when(mongoTemplate.find(any(Query.class), eq(Product.class))).thenReturn(results);
    }

    // ── getAllProductsPaged ───────────────────────────────────────────────────

    @Test
    void noFilters_returnsAllPaged() {
        Product p = product("p1", "Phone", "A smartphone", "Electronics", 5);
        stubMongoTemplate(List.of(p));

        PagedResponse<ProductResponse> result = productService.getAllProductsPaged(
                0, 10, null, null, null, "name", "asc", null, false, null);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo("p1");
        verify(mongoTemplate).find(any(Query.class), eq(Product.class));
    }

    @Test
    void search_byKeyword_queryContainsOrOperator() {
        stubMongoTemplate(List.of());
        ArgumentCaptor<Query> captor = ArgumentCaptor.forClass(Query.class);

        productService.getAllProductsPaged(0, 10, "bluetooth", null, null, "name", "asc", null, false, null);

        verify(mongoTemplate).find(captor.capture(), eq(Product.class));
        String queryJson = captor.getValue().getQueryObject().toJson();
        assertThat(queryJson).contains("$or");
        assertThat(queryJson).contains("name");
        assertThat(queryJson).contains("description");
    }

    @Test
    void filter_byCategory_queryContainsCategoryField() {
        stubMongoTemplate(List.of());
        ArgumentCaptor<Query> captor = ArgumentCaptor.forClass(Query.class);

        productService.getAllProductsPaged(0, 10, null, null, null, "name", "asc", "electronics", false, null);

        verify(mongoTemplate).find(captor.capture(), eq(Product.class));
        String queryJson = captor.getValue().getQueryObject().toJson();
        assertThat(queryJson).contains("category");
    }

    @Test
    void filter_byAvailabilityOnly_queryContainsQuantityGt() {
        stubMongoTemplate(List.of());
        ArgumentCaptor<Query> captor = ArgumentCaptor.forClass(Query.class);

        productService.getAllProductsPaged(0, 10, null, null, null, "name", "asc", null, true, null);

        verify(mongoTemplate).find(captor.capture(), eq(Product.class));
        String queryJson = captor.getValue().getQueryObject().toJson();
        assertThat(queryJson).contains("quantity");
        assertThat(queryJson).contains("$gt");
    }

    @Test
    void filter_bySeller_queryContainsUserId() {
        stubMongoTemplate(List.of());
        ArgumentCaptor<Query> captor = ArgumentCaptor.forClass(Query.class);

        productService.getAllProductsPaged(0, 10, null, null, null, "name", "asc", null, false, "seller1");

        verify(mongoTemplate).find(captor.capture(), eq(Product.class));
        String queryJson = captor.getValue().getQueryObject().toJson();
        assertThat(queryJson).contains("userId");
        assertThat(queryJson).contains("seller1");
    }

    @Test
    void filter_combinedCategoryAndAvailability_bothInQuery() {
        stubMongoTemplate(List.of());
        ArgumentCaptor<Query> captor = ArgumentCaptor.forClass(Query.class);

        productService.getAllProductsPaged(0, 10, null, null, null, "name", "asc", "books", true, null);

        verify(mongoTemplate).find(captor.capture(), eq(Product.class));
        String queryJson = captor.getValue().getQueryObject().toJson();
        assertThat(queryJson).contains("category");
        assertThat(queryJson).contains("quantity");
    }

    // ── getSellerProductsPaged ────────────────────────────────────────────────

    @Test
    void sellerProducts_withCategoryFilter_sellerIdAndCategoryInQuery() {
        stubMongoTemplate(List.of());
        ArgumentCaptor<Query> captor = ArgumentCaptor.forClass(Query.class);

        productService.getSellerProductsPaged("seller1", 0, 10, null, null, null, "name", "asc", "books", false);

        verify(mongoTemplate).find(captor.capture(), eq(Product.class));
        String queryJson = captor.getValue().getQueryObject().toJson();
        assertThat(queryJson).contains("userId");
        assertThat(queryJson).contains("seller1");
        assertThat(queryJson).contains("category");
    }

    @Test
    void sellerProducts_noFilters_onlySellerIdInQuery() {
        Product p = product("p2", "Shirt", "Cotton shirt", "Clothing", 3);
        stubMongoTemplate(List.of(p));
        ArgumentCaptor<Query> captor = ArgumentCaptor.forClass(Query.class);

        PagedResponse<ProductResponse> result = productService.getSellerProductsPaged(
                "seller1", 0, 10, null, null, null, "name", "asc", null, false);

        verify(mongoTemplate).find(captor.capture(), eq(Product.class));
        assertThat(result.getContent()).hasSize(1);
        String queryJson = captor.getValue().getQueryObject().toJson();
        assertThat(queryJson).contains("userId");
    }
}
