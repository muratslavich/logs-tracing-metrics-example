package com.slavich.microservice3

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import org.springframework.stereotype.Component
import java.io.IOException


private val logger = io.github.oshai.kotlinlogging.KotlinLogging.logger {}

@SpringBootApplication
class Microservice3Application

fun main(args: Array<String>) {
    runApplication<Microservice3Application>(*args)
}

@Configuration
class Service3Config {

    @Bean
    fun kafkaListenerContainerFactory(consumerFactory: ConsumerFactory<String, String>): ConcurrentKafkaListenerContainerFactory<String, String> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, String>()

        //The following code enable observation in the consumer listener
        factory.containerProperties.isObservationEnabled = true
        factory.consumerFactory = consumerFactory
        return factory
    }

    @Bean
    fun kafkaTemplate(producerFactory: ProducerFactory<String, String>): KafkaTemplate<String, String> {
        val stringStringKafkaTemplate = KafkaTemplate(producerFactory)
        stringStringKafkaTemplate.setObservationEnabled(true)
        return stringStringKafkaTemplate
    }
}

@Component
class ListenerLogService(
    val kafkaTemplate: KafkaTemplate<String, String>
) {
    @KafkaListener(topics = ["TUNNEL"])
    @Throws(IOException::class)
    fun consume(message: String) {
        logger.info { "here we consume kafka $message" }
        runBlocking {
            delay((0..1500).random().toLong())
        }
        kafkaTemplate.send("TUNNEL_RESPONSE", message)
    }
}
