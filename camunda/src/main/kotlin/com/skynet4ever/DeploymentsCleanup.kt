package com.skynet4ever

import com.skynet4ever.utils.Logging
import org.camunda.bpm.engine.delegate.DelegateExecution
import org.camunda.bpm.engine.delegate.JavaDelegate
import org.camunda.bpm.engine.repository.DecisionDefinition
import org.camunda.bpm.engine.repository.Deployment
import org.camunda.bpm.engine.repository.ProcessDefinition
import org.springframework.stereotype.Service

/**
 * Remove process definitions and DMNs that are no longer used.
 * A deployment can only be removed if all process definitions and DMN definitions contained in it
 * are not in use (no instances running or in the history and if it is not the latest version)
 *
 * Based on: https://camunda.com/best-practices/cleaning-up-historical-data/
 */
@Service("deploymentsCleanup")
class DeploymentsCleanup : JavaDelegate, Logging {

    override fun execute(delegateExecution: DelegateExecution) {
        logger.info("Starting deployments cleanup...")
        val context = Context(delegateExecution)
        val toDelete = context.deployments()
            .filter {
                logger.debug("Analyzing deployment '$it' ...")
                // can only delete a deployment if ALL process definitions and dmn definitions can be deleted
                context.processDefinitions(it).all(context::canDeleteProcessDefinition) &&
                        context.decisionDefinitions(it).all(context::canDeleteDecisionDefinition)
            }

        if (toDelete.isEmpty()) {
            logger.info("No deployments to cleanup")
        } else {
            logger.info("Deleting deployments: $toDelete")
        }
        delegateExecution.setVariable("toDeleteDeploymentIds", toDelete.map { it.id })
        toDelete.forEach {
            delegateExecution.processEngineServices.repositoryService
                .deleteDeployment(it.id)
        }
        logger.info("Deployments deleted: $toDelete")
    }

    class Context(delegateExecution: DelegateExecution) : Logging {
        private val repositoryService = delegateExecution.processEngineServices.repositoryService
        private val runtimeService = delegateExecution.processEngineServices.runtimeService
        private val historyService = delegateExecution.processEngineServices.historyService

        fun processDefinitions(deployment: Deployment): List<ProcessDefinition> =
            repositoryService
                .createProcessDefinitionQuery()
                .deploymentId(deployment.id).unlimitedList()

        fun decisionDefinitions(deployment: Deployment): List<DecisionDefinition> =
            repositoryService
                .createDecisionDefinitionQuery()
                .deploymentId(deployment.id).unlimitedList()

        fun deployments(): List<Deployment> =
            repositoryService.createDeploymentQuery().unlimitedList()

        fun canDeleteProcessDefinition(processDefinition: ProcessDefinition): Boolean {
            val latestProcessDefinition = repositoryService
                .createProcessDefinitionQuery()
                .processDefinitionKey(processDefinition.key)
                .latestVersion().singleResult()

            val isLatest = latestProcessDefinition.id.equals(processDefinition.id)

            val hasRunningInstances = runtimeService
                .createProcessInstanceQuery()
                .processDefinitionId(processDefinition.id).count() > 0

            val hasHistoricInstances = historyService
                .createHistoricProcessInstanceQuery()
                .processDefinitionId(processDefinition.id).count() > 0

            val canDelete = !(isLatest || hasRunningInstances || hasHistoricInstances)
            logger.debug("Can Delete: $canDelete, Process definition: $processDefinition")
            return canDelete
        }

        fun canDeleteDecisionDefinition(decisionDefinition: DecisionDefinition): Boolean {
            val latestProcessDefinition = repositoryService
                .createDecisionDefinitionQuery()
                .decisionDefinitionKey(decisionDefinition.key)
                .latestVersion().singleResult()

            val isLatest = latestProcessDefinition.id.equals(decisionDefinition.id)

            val hasHistoricInstances = historyService
                .createHistoricDecisionInstanceQuery()
                .processDefinitionId(decisionDefinition.id).count() > 0

            val canDelete = !(isLatest || hasHistoricInstances)
            logger.debug("Can Delete: $canDelete, Decision definition: $decisionDefinition")
            return canDelete
        }
    }

}