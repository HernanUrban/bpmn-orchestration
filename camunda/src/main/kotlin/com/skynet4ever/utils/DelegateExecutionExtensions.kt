package com.skynet4ever.utils

import org.camunda.bpm.engine.delegate.DelegateExecution

object DelegateExecutionExtensions {

    fun DelegateExecution.processDefinitionKey(): String {
        // Process definitions are cached by repository service
        return processEngineServices
            .repositoryService
            .getProcessDefinition(processDefinitionId)
            .key
        // Alternative impl without using the Repository Service:
        // val processDefinitionKey = (execution as ExecutionEntity).processDefinition.key
    }

}