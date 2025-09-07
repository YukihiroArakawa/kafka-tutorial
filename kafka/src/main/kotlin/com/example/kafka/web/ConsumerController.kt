package com.example.kafka.web

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.util.concurrent.ConcurrentLinkedDeque
import org.slf4j.LoggerFactory

@RestController
@RequestMapping("/consume")
class ConsumerController(
    @Value("\${app.kafka.consumer-group:sample-consumer}") private val groupId: String,
) {
    companion object {
        private val log = LoggerFactory.getLogger(ConsumerController::class.java)
    }
    data class ConsumedEvent(
        val topic: String,
        val partition: Int,
        val offset: Long,
        val timestamp: Long,
        val key: String?,
        val value: String,
        val receivedAt: Long = Instant.now().toEpochMilli(),
    )

    private val recent = ConcurrentLinkedDeque<ConsumedEvent>()
    private val maxKeep = 100

    @KafkaListener(
        topics = ["\${app.kafka.topic:sample-topic}"],
        groupId = "\${app.kafka.consumer-group:sample-consumer}"
    )
    fun listen(record: ConsumerRecord<String, String>) {
        log.info(
            "Consumed message: topic={}, partition={}, offset={}, key={}, value={}, timestamp={}, groupId={}",
            record.topic(), record.partition(), record.offset(), record.key(), record.value(), record.timestamp(), groupId
        )
        val event = ConsumedEvent(
            topic = record.topic(),
            partition = record.partition(),
            offset = record.offset(),
            timestamp = record.timestamp(),
            key = record.key(),
            value = record.value(),
        )
        recent.addFirst(event)
        while (recent.size > maxKeep) recent.removeLast()
    }

    @GetMapping("/records")
    fun records(): ResponseEntity<List<ConsumedEvent>> = ResponseEntity.ok(recent.toList())

    @DeleteMapping("/records")
    fun clear(): ResponseEntity<Void> {
        recent.clear()
        return ResponseEntity.noContent().build()
    }
}
