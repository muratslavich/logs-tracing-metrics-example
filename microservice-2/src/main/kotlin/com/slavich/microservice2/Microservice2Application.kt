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
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Hooks
import reactor.core.publisher.Mono


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
class FooController {

    @GetMapping("/tests")
    fun foo() {
        logger.info { "here we come" }
        runBlocking {
            launch { first() }
            launch { second() }
            launch { sendToKafka() }
        }
    }

    private suspend fun sendToKafka() {
        logger.info { "here message sends to kafka" }
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
