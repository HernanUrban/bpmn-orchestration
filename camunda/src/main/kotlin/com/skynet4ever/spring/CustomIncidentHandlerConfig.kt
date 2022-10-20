package com.skynet4ever.spring

import com.skynet4ever.ExternalTaskIncidentHandler
import com.skynet4ever.FailedJobIncidentHandler
import org.camunda.bpm.engine.ProcessEngine
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl
import org.camunda.bpm.engine.impl.cfg.ProcessEnginePlugin
import org.springframework.stereotype.Component

@Component
class CustomIncidentHandlerConfig : ProcessEnginePlugin {
    override fun preInit(processEngineConfiguration: ProcessEngineConfigurationImpl?) {
        val failedJobHandler = FailedJobIncidentHandler(processEngineConfiguration)
        val externalTaskHandler = ExternalTaskIncidentHandler(processEngineConfiguration)
        processEngineConfiguration?.customIncidentHandlers = listOf(failedJobHandler, externalTaskHandler)
    }

    override fun postInit(processEngineConfiguration: ProcessEngineConfigurationImpl?) {

    }

    override fun postProcessEngineBuild(processEngine: ProcessEngine?) {

    }
}
