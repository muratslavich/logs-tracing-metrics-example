package com.slavich.microservice1

import io.micrometer.core.instrument.kotlin.asContextElement
import io.micrometer.observation.Observation
import io.micrometer.observation.ObservationRegistry
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor
import kotlinx.coroutines.*
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitExchange
import reactor.core.publisher.Hooks

private val logger = io.github.oshai.kotlinlogging.KotlinLogging.logger {}

@SpringBootApplication
class Microservice1Application

fun main(args: Array<String>) {
    // https://micrometer.io/docs/observation#instrumentation_of_reactive_libraries_after_reactor_3_5_3
    Hooks.enableAutomaticContextPropagation()
    runApplication<Microservice1Application>(*args)
}

@Configuration
class MyContext(
    val registry: ObservationRegistry,
) {

    @OptIn(DelicateCoroutinesApi::class)
    @Bean
    fun startMyRequest(): Unit {
        ObservationThreadLocalAccessor.getInstance().setObservationRegistry(registry);
        GlobalScope.launch {
            while (true) {
                delay(3000)
                // create trace
                Observation.createNotStarted("m-observation", registry)
                    .observe {
                        runBlocking {
                            extracted()
                        }
                    }
            }
        }
    }

    private suspend fun extracted() {
        // context inside tread
        val context = registry.asContextElement() + Dispatchers.Unconfined
        try {
            logger.info { "here we go ${registry.currentObservation!!.context.name}" }
            WebClient.builder()
                .observationRegistry(registry)
                // enable headers logging
                .exchangeStrategies(ExchangeStrategies.builder().codecs {
                    it.defaultCodecs().enableLoggingRequestDetails(true) }
                    .build()
                )
                .build()
                .get()
                .uri("http://127.0.0.1:8082/tests")
                .awaitExchange {
                    // propagate observation context
                    withContext(context) {
                        logger.info { "response ${it.statusCode()}" }
                    }
                }
        } catch (e: Exception) {
            logger.warn(e) { "here we wrap error" }
        } finally {
            logger.debug { "here we final request" }
        }
    }
}


