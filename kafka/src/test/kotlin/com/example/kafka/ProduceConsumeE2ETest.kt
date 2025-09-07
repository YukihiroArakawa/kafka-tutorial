package com.example.kafka

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.context.TestPropertySource
import java.util.UUID

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EmbeddedKafka(partitions = 1, topics = ["sample-topic"])
@TestPropertySource(properties = [
    "spring.kafka.bootstrap-servers=\${spring.embedded.kafka.brokers}",
    "app.kafka.consumer-group=e2e-consumer-group"
])
class ProduceConsumeE2ETest {

    @Autowired
    lateinit var rest: TestRestTemplate

    @LocalServerPort
    var port: Int = 0

    private fun url(path: String) = "http://localhost:$port$path"

    @Test
    fun `produce then consume via REST reflects message`() {
        // Clear previous in-memory records on the consumer side
        rest.delete(url("/consume/records"))

        val message = "e2e-" + UUID.randomUUID()

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        val entity = HttpEntity(mapOf("value" to message), headers)

        val produceResp = rest.postForEntity(url("/produce"), entity, Map::class.java)
        assertTrue(produceResp.statusCode.is2xxSuccessful, "produce endpoint should succeed")

        // Poll the consumer records REST endpoint until the message appears
        val typeRef = object : ParameterizedTypeReference<List<Map<String, Any>>>() {}
        var found = false
        val deadline = System.currentTimeMillis() + 10_000 // up to 10 seconds
        while (System.currentTimeMillis() < deadline && !found) {
            val resp = rest.exchange(url("/consume/records"), HttpMethod.GET, null, typeRef)
            if (resp.statusCode.is2xxSuccessful) {
                val list = resp.body ?: emptyList()
                found = list.any { (it["value"] as? String) == message }
            }
            if (!found) Thread.sleep(250)
        }

        assertTrue(found, "Consumer should have received the produced message via Kafka")
    }
}

