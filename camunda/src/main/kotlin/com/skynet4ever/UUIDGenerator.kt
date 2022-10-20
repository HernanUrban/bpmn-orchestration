package com.skynet4ever

import com.skynet4ever.utils.Logging
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Service
import java.util.*

@Service("UUID")
class UUIDGenerator(private val registry: MeterRegistry) : Logging {
    fun randomUUID(): String {
        logger.info("Generating UUID")
        registry.counter("camunda.uuid").increment()
        return UUID.randomUUID().toString()
    }
}