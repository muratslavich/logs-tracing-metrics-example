package com.slavich.microservice2

import io.micrometer.observation.ObservationTextPublisher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Hooks
import reactor.core.publisher.Mono
import java.io.IOException
import kotlin.random.Random


private val logger = io.github.oshai.kotlinlogging.KotlinLogging.logger {}

@SpringBootApplication
class Microservice2Application

fun main(args: Array<String>) {
    Hooks.enableAutomaticContextPropagation()
    runApplication<Microservice2Application>(*args)
}

@Configuration
class ObservationConfig {

    @Bean
    fun kafkaTemplate(producerFactory: ProducerFactory<String, String>): KafkaTemplate<String, String> {
        val stringStringKafkaTemplate = KafkaTemplate(producerFactory)
        stringStringKafkaTemplate.setObservationEnabled(true)
        return stringStringKafkaTemplate
    }

    @Bean
    fun kafkaListenerContainerFactory(consumerFactory: ConsumerFactory<String, String>): ConcurrentKafkaListenerContainerFactory<String, String> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, String>()

        //The following code enable observation in the consumer listener
        factory.containerProperties.isObservationEnabled = true
        factory.consumerFactory = consumerFactory
        return factory
    }

    @Bean
    @ConditionalOnProperty(prefix = "log-observation-events", name = ["enabled"], matchIfMissing = false)
    fun otp(): ObservationTextPublisher {
        return ObservationTextPublisher()
    }

}

@Component
class RequestMonitorWebFilter : WebFilter {

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        return chain.filter(exchange)
            /** !! IMPORTANT STEP !!
             * Preparing context for the Tracer Span used in TracerConfiguration
             */
            .contextWrite {
                it.also {
                    logger.info { "here ${exchange.request.method} ${exchange.request.path}" }
                }
//                ContextSnapshot.setThreadLocalsFrom(context!!, ObservationThreadLocalAccessor.KEY)
            }
            .doFinally {
                logger.info { "here we end ${exchange.response.statusCode}" }
            }
    }
}


@RestController
class FooController(
    val kafkaTemplate: KafkaTemplate<String, String>,
) {

    @GetMapping("/tests")
    fun foo() {
        logger.info { "here we come" }
        runBlocking {
            launch { first() }
            launch { second() }
            launch { sendToKafka() }
        }
    }

    @KafkaListener(topics = ["TUNNEL_RESPONSE"], groupId = "group_id")
    @Throws(IOException::class)
    fun consume(message: String) {
        logger.info { "here we consume kafka response $message" }
    }

    private suspend fun sendToKafka() {
        logger.info { "here message sends to kafka" }
        kafkaTemplate.send("TUNNEL", Random.nextInt().toString())
    }

    private suspend fun first() {
        delay(5000)
        logger.info { "here first delay" }
    }

    private suspend fun second() {
        delay(500)
        logger.info { "here second delay" }
    }
}
