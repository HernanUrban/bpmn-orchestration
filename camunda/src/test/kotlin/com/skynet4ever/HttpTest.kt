package com.skynet4ever

import com.skynet4ever.camunda.SimpleDelegate
import com.skynet4ever.camunda.SimpleDelegate.Companion.httpOk
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions
import org.camunda.bpm.engine.ProcessEngine
import org.camunda.bpm.engine.delegate.BpmnError
import org.camunda.bpm.engine.runtime.ProcessInstance
import org.camunda.bpm.engine.test.Deployment
import org.camunda.bpm.engine.test.assertions.ProcessEngineTests.assertThat
import org.camunda.bpm.engine.test.assertions.ProcessEngineTests.execute
import org.camunda.bpm.engine.test.mock.Mocks
import org.camunda.bpm.extension.junit5.test.ProcessEngineExtension
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Duration
import java.time.Instant
import java.util.*

@ExtendWith(ProcessEngineExtension::class)
@Deployment(resources = ["test.bpmn", "call-activity.bpmn"])
class HttpTest {

    private val processDefinitionKey = "testHttp"
    lateinit var processEngine: ProcessEngine
    lateinit var http: SimpleDelegate

    @BeforeEach
    fun setUp() {
        http = mockk(relaxed = true)
        every { http.execute(any(), any()) } returns httpOk()
        every { http.execute("not_found", any()) } throws BpmnError("some.error.code", "some message")
        SimpleDelegate.register("http", http)
        Mocks.register("urls", mapOf("MOCK" to "something"))
        Mocks.register("UUID", UUIDGenerator(SimpleMeterRegistry()))
        Mocks.register("checkDuplicatedBusinessKey", CheckDuplicatedBusinessKey(SimpleMeterRegistry()))
    }

    @Test
    fun ignoreSecondInstanceWithSameKey() {
        every { http.execute("post_1", any()) } returns
                httpOk(mapOf("email" to "ok@ank.app", "status" to "DONE"))

        // first
        val processInstance = start("someBusinessKey", mapOf("userId" to "123"))
        executeAllJobs()
        processInstance.correlate("something.happened.someBusinessKey", mapOf("something" to "zzz"))
        processInstance.correlate("something.someBusinessKey", emptyMap())

        assertThat(processInstance).hasPassed("end_ok").isEnded

        // second
        val processInstance2 = start("someBusinessKey", mapOf("userId" to "123"))
        executeAllJobs()
        assertThat(processInstance2).hasPassed("end_ignored").isEnded
    }

    @Test
    fun canRetry3TimesPOST1() {
        every { http.execute("post_1", any()) } returnsMany listOf(
            httpOk(mapOf("status" to "IN_PROGRESS")),
            httpOk(mapOf("status" to "IN_PROGRESS")),
            httpOk(mapOf("status" to "DONE")),
        )

        val processInstance = start("someBusinessKey", mapOf("userId" to "123"))
        executeAllJobs()
        repeat(2) {
            processInstance.executeTimer("backoff_timer")
        }
        executeAllJobs()

        processInstance.correlate("something.happened.someBusinessKey", mapOf("something" to "zzz"))
        processInstance.correlate("something.someBusinessKey", emptyMap())

        assertThat(processInstance).hasPassed("end_ok").isEnded
    }

    @Test
    fun failsOnTooManyAttempts() {
        every { http.execute("post_1", any()) } returns httpOk(mapOf("status" to "IN_PROGRESS"))

        val processInstance = start("someBusinessKey", mapOf("userId" to "123"))
        executeAllJobs()
        repeat(2) {
            processInstance.executeTimer("backoff_timer")
        }
        assertThat(processInstance).hasPassed("end_error").isEnded
    }

    @Test
    fun failsOnTimeout() {
        every { http.execute("post_1", any()) } returns httpOk(mapOf("status" to "IN_PROGRESS"))

        val processInstance = start("someBusinessKey", mapOf("userId" to "123"))
        executeAllJobs()
        val timer = processEngine.managementService.createJobQuery()
            .timers()
            .activityId("timeout_timer")
            .singleResult()

        val oneMin = Duration.ofMinutes(1).toMillis()
        val oneHour = Date.from(Instant.now().plus(Duration.ofHours(1)))
        Assertions.assertThat(timer.duedate).isCloseTo(oneHour, oneMin)

        execute(timer)
        executeAllJobs()
        assertThat(processInstance).hasPassed("end_timeout").isEnded
    }

    private fun start(businessKey: String, variables: Map<String, Any>): ProcessInstance =
        processEngine
            .runtimeService
            .startProcessInstanceByKey(processDefinitionKey, businessKey, variables)

    private fun ProcessInstance.correlate(name: String, variables: Map<String, Any>) {
        assertThat(this).isWaitingFor(name)

        processEngine.runtimeService
            .createMessageCorrelation(name)
            .setVariables(variables)
            .correlateAll()

        executeAllJobs()
    }

    private fun executeAllJobs() {
        val managementService = processEngine.managementService
        var jobs = managementService.createJobQuery().executable().list()
        while (jobs.isNotEmpty()) {
            jobs.forEach { execute(it) }
            jobs = managementService.createJobQuery().executable().list()
        }
    }

    private fun ProcessInstance.executeTimer(activityId: String) {
        assertThat(this).isWaitingAt(activityId)
        val job = processEngine.managementService.createJobQuery()
            .timers()
            .activityId(activityId)
            .singleResult() ?: throw Exception("Not waiting on timer $activityId")
        execute(job)
    }

}
