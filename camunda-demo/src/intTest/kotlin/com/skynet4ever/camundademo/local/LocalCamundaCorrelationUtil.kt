package com.skynet4ever.camundademo.local

import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.skynet4ever.camundademo.util.Logging
import org.camunda.bpm.engine.ProcessEngine
import org.camunda.bpm.engine.ProcessEngineConfiguration

object LocalCamundaCorrelationUtil : Logging {
    // Default configuration
    private val processEngine: ProcessEngine by lazy {
        ProcessEngineConfiguration
            .createStandaloneInMemProcessEngineConfiguration()
            .setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_FALSE)
            .setJdbcUrl("jdbc:postgresql://localhost:5432/camunda")
            .setJdbcUsername("skynet")
            .setJdbcPassword("skynet")
            .setJdbcDriver("org.postgresql.Driver")
            .setJobExecutorActivate(false)
            .setHistory(ProcessEngineConfiguration.HISTORY_FULL)
            .buildProcessEngine()
    }

    @JvmStatic
    fun main(args: Array<String>) {
        // Update with the movementId from current camunda flow
        //val userId = "77b56248-3eb7-437e-931f-921df2fd81b7"
        //val userId = "123error"
        val onboardRevisionEvent = OnboardingRevisionEvent(
            allowedCreation = true,
            newStatus = Status.ACCEPTED
        )
        //startProcess(userId)
        //correlate("UserRevisionChanged_${userId}", RevisionWrapper(onboardRevisionEvent))

    }


    private fun correlate(messageName: String, variables: Any) {
        logger.info("Correlation message id: $messageName")

        processEngine.runtimeService
            .createMessageCorrelation(messageName)
            .setVariables(toMap(variables))
            .correlateAll()
    }

    private fun startProcess(userId: String) {
        logger.info("Starting process skyner-demo for user: $userId")
        processEngine.runtimeService.startProcessInstanceByKey("skynet-demo","998878", mapOf(Pair("userId", userId)))
    }

    private fun toMap(event: Any): Map<String, Any> = jacksonObjectMapper().convertValue(event)

    private data class RevisionWrapper(val revision: Any)
}


data class OnboardingRevisionEvent(
    val allowedCreation: Boolean,
    val newStatus: Status
)

enum class Status {
    ACCEPTED,
    REJECTED
}