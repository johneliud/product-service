package io.github.johneliud.product_service.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductDeletedEvent {
    private String productId;
    private String userId;
}
