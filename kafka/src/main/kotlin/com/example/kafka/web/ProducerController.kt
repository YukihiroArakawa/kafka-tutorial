package com.example.kafka.web

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.web.bind.annotation.*
import java.util.concurrent.TimeUnit

@RestController
@RequestMapping("/produce")
class ProducerController(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    @Value("\${app.kafka.topic:sample-topic}") private val topic: String,
) {

    data class EventRequest(
        val key: String? = null,
        val value: String,
    )

    @PostMapping
    fun publish(@RequestBody req: EventRequest): ResponseEntity<Map<String, Any?>> {
        val future = if (req.key == null) {
            kafkaTemplate.send(topic, req.value)
        } else {
            kafkaTemplate.send(topic, req.key, req.value)
        }
        val result = future.get(5, TimeUnit.SECONDS)
        val metadata = result.recordMetadata
        val body = mapOf(
            "topic" to metadata.topic(),
            "partition" to metadata.partition(),
            "offset" to metadata.offset(),
            "timestamp" to metadata.timestamp(),
            "key" to req.key,
            "value" to req.value,
        )
        return ResponseEntity.ok(body)
    }

    @GetMapping
    fun publishQuick(@RequestParam("message") message: String): ResponseEntity<Map<String, Any?>> =
        publish(EventRequest(value = message))
}
