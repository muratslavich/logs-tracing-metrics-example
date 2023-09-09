package com.slavich.microservice2

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Hooks

private val logger = io.github.oshai.kotlinlogging.KotlinLogging.logger {}

@SpringBootApplication
class Microservice2Application

fun main(args: Array<String>) {
    Hooks.enableAutomaticContextPropagation()
    runApplication<Microservice2Application>(*args)
}

@RestController
class FooController {

    @GetMapping("/tests")
    fun foo() {
        logger.info { "here we come" }
    }
}
