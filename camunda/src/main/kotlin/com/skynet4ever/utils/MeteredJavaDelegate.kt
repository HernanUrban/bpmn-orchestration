package com.skynet4ever.utils

import com.skynet4ever.utils.DelegateExecutionExtensions.processDefinitionKey
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.camunda.bpm.engine.delegate.BpmnError
import org.camunda.bpm.engine.delegate.DelegateExecution
import org.camunda.bpm.engine.delegate.JavaDelegate

abstract class MeteredJavaDelegate(
    private val registry: MeterRegistry
) : JavaDelegate {

    protected fun metricTags(execution: DelegateExecution): Array<String> {
        val definitionKey = execution.processDefinitionKey()
        val activityId = execution.currentActivityId
        val delegateType = this.javaClass.simpleName

        return arrayOf(
            "process_definition_key", definitionKey,
            "activity_id", activityId,
            "delegate_type", delegateType,
        )
    }

    override fun execute(execution: DelegateExecution) {
        val genericTags = metricTags(execution)

        // Counter to analyze if we have delegates starting but never ending
        registry.counter("camunda.delegate.start", *genericTags).increment()

        val sample = Timer.start(registry)
        fun stop(result: String, exception: Throwable? = null, errorCode: String = "") {
            sample.stop(
                registry.timer(
                    "camunda.delegate",
                    "result", result,
                    "exception", exception?.javaClass?.simpleName ?: "",
                    "error_code", errorCode,
                    *genericTags
                )
            )
        }

        return try {
            meteredExecute(execution)
            stop("success")
        } catch (e: BpmnError) {
            // don't send this exception to Sentry because is expected
            stop("error", e, e.errorCode)
            throw e
        } catch (e: Throwable) {
            stop("failure", e)
            throw e
        } finally {
            // Counter to analyze if we have delegates starting but never ending
            registry.counter("camunda.delegate.end", *genericTags).increment()
        }
    }

    abstract fun meteredExecute(execution: DelegateExecution)
}