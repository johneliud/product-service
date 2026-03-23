package io.github.johneliud.product_service.event;

import io.github.johneliud.product_service.services.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderStatusChangedEventListener {

    private final ProductService productService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "order-status-changed", groupId = "product-service")
    public void onOrderStatusChangedMessage(String message) {
        try {
            onOrderStatusChanged(objectMapper.readValue(message, OrderStatusChangedEvent.class));
        } catch (Exception e) {
            log.error("Failed to deserialize order-status-changed event: {}", e.getMessage());
        }
    }

    public void onOrderStatusChanged(OrderStatusChangedEvent event) {
        if (!"DELIVERED".equals(event.getNewStatus())) {
            return;
        }

        log.info("Order {} marked DELIVERED — decrementing stock for {} item(s)", event.getOrderId(), event.getItems().size());

        for (OrderStatusChangedEvent.OrderItem item : event.getItems()) {
            try {
                productService.decrementStock(item.getProductId(), item.getQuantity());
            } catch (Exception e) {
                log.error("Failed to decrement stock for productId {} (orderId {}): {}",
                        item.getProductId(), event.getOrderId(), e.getMessage());
            }
        }
    }
}
