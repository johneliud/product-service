package io.github.johneliud.product_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
	"spring.kafka.bootstrap-servers=localhost:9092",
	"spring.mongodb.uri=mongodb://localhost:27017/test"
})
class ProductServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
