package com.slavich.microservice1

import io.micrometer.observation.Observation
import io.micrometer.observation.ObservationRegistry
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

private val logger = io.github.oshai.kotlinlogging.KotlinLogging.logger {}

@SpringBootApplication
class Microservice1Application

fun main(args: Array<String>) {
    runApplication<Microservice1Application>(*args)
}

@Configuration
class MyContext(
    val registry: ObservationRegistry,
) {

    @OptIn(DelicateCoroutinesApi::class)
    @Bean
    fun startMyRequest() = GlobalScope.launch {
        while (true) {
            delay(3000)
            Observation.createNotStarted("m-observation", registry)
                .contextualName("command-line-runner")
                .observe {
                    logger.info { "here we go ${Thread.currentThread().name}" }
                }
        }
    }
}


