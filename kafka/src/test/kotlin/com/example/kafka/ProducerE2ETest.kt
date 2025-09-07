package com.example.kafka

import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer
import org.springframework.kafka.test.EmbeddedKafkaBroker
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.kafka.test.utils.KafkaTestUtils
import org.springframework.test.context.TestPropertySource
import java.time.Duration

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EmbeddedKafka(partitions = 1, topics = ["sample-topic"])
@TestPropertySource(properties = [
    "spring.kafka.bootstrap-servers=\${spring.embedded.kafka.brokers}"
])
class ProducerE2ETest {

    @Autowired
    lateinit var rest: TestRestTemplate

    @Autowired
    lateinit var embeddedKafka: EmbeddedKafkaBroker

    @LocalServerPort
    var port: Int = 0

    @Test
    fun `POST to produce publishes to Kafka`() {
        val message = "hello-e2e"

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        val body = mapOf("value" to message)
        val entity = HttpEntity(body, headers)

        val response = rest.postForEntity("http://localhost:$port/produce", entity, Map::class.java)
        assertTrue(response.statusCode.is2xxSuccessful, "Request to /produce should succeed")

        val consumerProps = KafkaTestUtils.consumerProps("e2e-consumer", "true", embeddedKafka).toMutableMap()
        consumerProps[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = ErrorHandlingDeserializer::class.java
        consumerProps[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = ErrorHandlingDeserializer::class.java
        consumerProps[ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS] = StringDeserializer::class.java
        consumerProps[ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS] = StringDeserializer::class.java
        consumerProps[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"

        val consumerFactory = DefaultKafkaConsumerFactory<String, String>(consumerProps)
        val consumer = consumerFactory.createConsumer()
        consumer.subscribe(listOf("sample-topic"))

        val records = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(5))
        val values = records.records("sample-topic").map { it.value() }

        assertTrue(values.contains(message), "Kafka should contain the produced message")

        consumer.close()
    }
}

