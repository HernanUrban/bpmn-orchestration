package com.skynet4ever.camunda

import org.camunda.bpm.engine.delegate.DelegateExecution
import org.camunda.bpm.engine.delegate.JavaDelegate
import org.camunda.bpm.engine.test.mock.Mocks

interface SimpleDelegate {
    companion object {
        fun delegate(simple: SimpleDelegate): JavaDelegate = JavaDelegate {
            val effect = simple.execute(it.currentActivityId, it.variables)
            effect(it)
        }

        fun register(key: String, simple: SimpleDelegate): Unit =
            Mocks.register(key, delegate(simple))

        fun setHttpVariables(statusCode: Int, body: Map<String, Any>): (DelegateExecution) -> Unit = {
            it.setVariableLocal("statusCode", statusCode)
            it.setVariableLocal("body", body)
        }

        fun httpOk(data: Any = emptyMap<String, Any>()): (DelegateExecution) -> Unit =
            setHttpVariables(200, mapOf("data" to data))
    }

    fun execute(activityId: String, variables: Map<String, Any>): (DelegateExecution) -> Unit
}