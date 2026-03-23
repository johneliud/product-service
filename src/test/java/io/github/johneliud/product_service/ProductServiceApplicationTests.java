package io.github.johneliud.product_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@TestPropertySource(properties = {
	"spring.mongodb.uri=mongodb://localhost:27017/test",
	"jwt.secret=testSecretKeyForTestingPurposeOnly123456",
	"jwt.expiration=86400000",
	"spring.kafka.bootstrap-servers=localhost:9092",
	"spring.kafka.consumer.group-id=product-service",
	"spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration,org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration,org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration"
})
class ProductServiceApplicationTests {

	@MockitoBean
	ConsumerFactory<String, String> consumerFactory;

	@MockitoBean
	ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory;

	@Test
	void contextLoads() {
	}

}
