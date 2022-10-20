package com.skynet4ever

import com.skynet4ever.utils.DelegateExecutionExtensions.processDefinitionKey
import com.skynet4ever.utils.Logging
import com.skynet4ever.utils.MeteredJavaDelegate
import io.micrometer.core.instrument.MeterRegistry
import org.camunda.bpm.engine.delegate.BpmnError
import org.camunda.bpm.engine.delegate.DelegateExecution
import org.springframework.stereotype.Service

@Service("checkDuplicatedBusinessKey")
class CheckDuplicatedBusinessKey(
    registry: MeterRegistry
) : MeteredJavaDelegate(registry), Logging {

    override fun meteredExecute(execution: DelegateExecution) {
        val businessKey = execution.processBusinessKey
        logger.trace("Business key: $businessKey")
        val processDefinitionKey = execution.processDefinitionKey()
        logger.trace("Process definition key: $processDefinitionKey")
        val processInstanceId = execution.processInstanceId
        logger.trace("Process instance id: $processInstanceId")

        val instanceList = execution.processEngineServices
            .historyService
            .createHistoricProcessInstanceQuery()
            .processDefinitionKey(processDefinitionKey)
            .processInstanceBusinessKey(businessKey)
            .orderByProcessInstanceStartTime()
            .asc()
            .listPage(0, 2)
        logger.trace("Instance list: $instanceList")
        logger.trace("Instance list size: ${instanceList.size}")

        if (instanceList.size > 1) {
            // only the "first" instance is the original, all the others should throw an error
            // WARNING: assumes that the "start time" is unique between instances with the same business key & definition key
            if (processInstanceId != instanceList[0].id) {
                logger.warn("Duplicated business key '$businessKey' (first instance id: '${instanceList[0].id}', current instance id: '$processInstanceId')")
                throw BpmnError("business-key.duplicated", "Duplicated business key")
            } else {
                // There are duplicates but this is the "first" instance
                logger.debug("Duplicated business key '$businessKey' (the current instance ('$processInstanceId´) is the first, next instance id is: '${instanceList[1].id}'")
            }
        } else {
            // No duplicates
            logger.debug("This instance ('$processInstanceId') is the only one with the business key '$businessKey'.")
        }
    }

}