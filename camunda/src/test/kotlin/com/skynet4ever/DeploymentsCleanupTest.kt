package com.skynet4ever

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import net.jqwik.api.*
import org.camunda.bpm.engine.delegate.DelegateExecution
import org.camunda.bpm.engine.repository.DecisionDefinition
import org.camunda.bpm.engine.repository.Deployment
import org.camunda.bpm.engine.repository.ProcessDefinition
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class DeploymentsCleanupTest {

    data class MockExecution(
        val delegateExecution: DelegateExecution = mockk(relaxed = true)
    ) {
        val repositoryService = delegateExecution.processEngineServices.repositoryService
        val runtimeService = delegateExecution.processEngineServices.runtimeService
        val historyService = delegateExecution.processEngineServices.historyService
    }

    val delegate = DeploymentsCleanup()

    @Test
    fun ifNoDeploymentsNothingHappens() {
        val m = MockExecution()
        every {
            m.repositoryService.createDeploymentQuery().unlimitedList()
        } returns emptyList()

        delegate.execute(m.delegateExecution)

        verify(exactly = 0) {
            m.repositoryService.deleteDeployment(any())
        }
    }

    @Property(tries = 100)
    @DisplayName("deployments where all its elements are not the latest deployed version & don't have running instances, should be deleted")
    fun `not the latest deployed version & don't have running instances, should be deleted`(
        @ForAll("deployments") deployments: List<MockDeployment>
    ) {
        val m = MockExecution()
        deployments.forEach { it.mockQueries(m) }
        every {
            m.repositoryService.createDeploymentQuery().unlimitedList()
        } returns deployments.map { it.mock }

        delegate.execute(m.delegateExecution)

        deployments.filter { it.shouldDelete }.forEach {
            verify {
                m.repositoryService.deleteDeployment(it.id)
            }
        }
    }

    data class MockDeployment(
        val processes: List<MockProcessDefinition>,
        val decisions: List<MockDecisionDefinition>,
    ) {
        val mock: Deployment = mockk()
        val id = mock.toString()

        fun mockQueries(m: MockExecution) {
            every { mock.id } returns id

            every {
                m.repositoryService
                    .createProcessDefinitionQuery()
                    .deploymentId(id).unlimitedList()
            } returns processes.map { it.mock }

            every {
                m.repositoryService
                    .createDecisionDefinitionQuery()
                    .deploymentId(mock.id).unlimitedList()
            } returns decisions.map { it.mock }

            processes.forEach { it.mockQueries(m) }
            decisions.forEach { it.mockQueries(m) }
        }

        val shouldDelete = processes.all { it.shouldDelete } &&
                decisions.all { it.shouldDelete }
    }

    data class MockProcessDefinition(
        val latest: Boolean,
        val running: Boolean,
        val history: Boolean,
    ) {
        val mock: ProcessDefinition = mockk()
        fun mockQueries(m: MockExecution) {
            val id = mock.toString()
            every { mock.id } returns id

            val key = id + "key"
            every { mock.key } returns key

            every {
                m.repositoryService
                    .createProcessDefinitionQuery()
                    .processDefinitionKey(key)
                    .latestVersion().singleResult()
            } returns if (latest) {
                mock
            } else {
                val other: ProcessDefinition = mockk()
                every { other.id } returns "otherId"
                other
            }

            every {
                m.runtimeService
                    .createProcessInstanceQuery()
                    .processDefinitionId(id).count()
            } returns if (running) 1 else 0

            every {
                m.historyService
                    .createHistoricProcessInstanceQuery()
                    .processDefinitionId(id).count()
            } returns if (history) 1 else 0
        }

        val shouldDelete = !latest && !running && !history
    }

    data class MockDecisionDefinition(
        val latest: Boolean,
        val history: Boolean,
    ) {
        val mock: DecisionDefinition = mockk()

        fun mockQueries(m: MockExecution) {
            val id = mock.toString()
            every { mock.id } returns id
            val key = id + "key"
            every { mock.key } returns key

            every {
                m.repositoryService
                    .createDecisionDefinitionQuery()
                    .decisionDefinitionKey(key)
                    .latestVersion().singleResult()
            } returns if (latest) {
                mock
            } else {
                val other: DecisionDefinition = mockk()
                every { other.id } returns "otherId"
                other
            }

            every {
                m.historyService
                    .createHistoricDecisionInstanceQuery()
                    .processDefinitionId(id).count()
            } returns if (history) 1 else 0
        }

        val shouldDelete = !latest && !history
    }

    @Provide
    fun deployments(): Arbitrary<List<MockDeployment>> =
        Combinators.combine(
            processes().list().ofMaxSize(10),
            decisions().list().ofMaxSize(10),
        ).`as`(::MockDeployment).list().ofMaxSize(10)

    fun processes(): Arbitrary<MockProcessDefinition> =
        Combinators.combine(
            Arbitraries.defaultFor(Boolean::class.java),
            Arbitraries.defaultFor(Boolean::class.java),
            Arbitraries.defaultFor(Boolean::class.java),
        ).`as`(::MockProcessDefinition)

    fun decisions(): Arbitrary<MockDecisionDefinition> =
        Combinators.combine(
            Arbitraries.defaultFor(Boolean::class.java),
            Arbitraries.defaultFor(Boolean::class.java),
        ).`as`(::MockDecisionDefinition)

}
