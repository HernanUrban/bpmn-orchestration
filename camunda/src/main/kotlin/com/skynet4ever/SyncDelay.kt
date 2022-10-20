package com.skynet4ever

import com.skynet4ever.utils.Logging
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Deprecated("Uses too many threads", replaceWith = ReplaceWith("timer"))
@Service("syncDelay")
class SyncDelay(private val registry: MeterRegistry) : Logging {
    fun millis(millis: Long) {
        logger.info("Sync thread sleep: $millis")
        registry
            .timer("camunda.delegate", "delegate_type", "SyncDelay")
            .record(millis, TimeUnit.MILLISECONDS)
        Thread.sleep(millis)
    }
}