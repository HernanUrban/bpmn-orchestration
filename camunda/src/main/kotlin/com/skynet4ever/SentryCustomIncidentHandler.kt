package com.skynet4ever

import com.skynet4ever.utils.Logging
import io.sentry.Sentry
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl
import org.camunda.bpm.engine.impl.incident.IncidentContext
import org.camunda.bpm.engine.impl.incident.IncidentHandler
import org.camunda.bpm.engine.runtime.Incident

abstract class SentryCustomIncidentHandler(open val engineConfig: ProcessEngineConfigurationImpl?) : IncidentHandler {
    companion object : Logging

    override fun handleIncident(context: IncidentContext?, message: String?): Incident? {
        var sentryMessage: String? = message
        context?.let { ctx ->
            engineConfig?.let { engine ->
                val processDefinition = engine.repositoryService.getProcessDefinition(ctx.processDefinitionId)
                val processInstanceId = engine.runtimeService.createExecutionQuery().executionId(ctx.executionId)
                    .singleResult().processInstanceId
                val businessKey = engine.runtimeService.createProcessInstanceQuery()
                    .processInstanceId(processInstanceId).singleResult().businessKey

                val debugMessage = """Sending incident info to sentry: activity_id=${ctx.activityId},
                        |process_definition_id=${ctx.processDefinitionId},
                        |process_definition_key=${processDefinition.key},
                        |processInstanceId=${processInstanceId},
                        |business_key=$businessKey,
                        |process_definition_version=${processDefinition.version},
                        |message=$message""".trimMargin().replace("\n", " ")
                logger.debug(debugMessage)
                Sentry.configureScope { scope ->
                    scope.setTag("activity_id", ctx.activityId)
                    scope.setTag("process_definition_id", ctx.processDefinitionId)
                    scope.setTag(
                        "process_definition_key",
                        processDefinition.key
                    )
                    scope.setTag(
                        "process_definition_version",
                        processDefinition.version.toString()
                    )
                    scope.setTag(
                        "business_key",
                        businessKey
                    )
                }
                sentryMessage =
                    "Error on camunda process=${processDefinition.key} version=${processDefinition.version} " +
                            "businessKey=$businessKey processDefinitionId=${ctx.processDefinitionId}. Detail: $message"
            }
        }
        Sentry.captureMessage(sentryMessage ?: "Null message exception!")
        return null
    }

    override fun resolveIncident(context: IncidentContext?) {
    }

    override fun deleteIncident(context: IncidentContext?) {
    }
}

class FailedJobIncidentHandler(override val engineConfig: ProcessEngineConfigurationImpl?) :
    SentryCustomIncidentHandler(engineConfig) {
    override fun getIncidentHandlerType(): String = Incident.FAILED_JOB_HANDLER_TYPE
}

class ExternalTaskIncidentHandler(override val engineConfig: ProcessEngineConfigurationImpl?) :
    SentryCustomIncidentHandler(engineConfig) {
    override fun getIncidentHandlerType(): String = Incident.EXTERNAL_TASK_HANDLER_TYPE
}
