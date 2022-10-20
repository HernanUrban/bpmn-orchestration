package com.skynet4ever

import com.fasterxml.jackson.databind.ObjectMapper
import org.camunda.bpm.engine.rest.dto.VariableValueDto
import org.camunda.bpm.engine.rest.dto.message.CorrelationMessageDto
import org.camunda.bpm.engine.rest.dto.runtime.StartProcessInstanceDto
import org.camunda.bpm.engine.variable.Variables
import java.net.Authenticator
import java.net.PasswordAuthentication
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets

object MessagesManualTest {

    val objectMapper = ObjectMapper()
    val stringBody = HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)

    @JvmStatic
    fun main(args: Array<String>) {
        val businessKey = "abc"
        val client = buildClient()

        // first message
        val message1 = correlateMessage("something.happened.$businessKey", mapOf("something" to "zzz"))
        val response1 = client.send(message1, stringBody)
        println(response1)

        // to give time for the next subscription to activate
        Thread.sleep(1000)

        // second message
        val message2 = correlateMessage("something.$businessKey")
        val response2 = client.send(message2, stringBody)
        println(response2)
        //val asdf2 = StartProcessInstanceDto()
    }

    fun correlateMessage(name: String, variables: Map<String, Any>? = null): HttpRequest {
        val variablesDto = variables?.mapValues {
            val untypedValue = Variables.untypedValue(it.value, false)
            VariableValueDto.fromTypedValue(untypedValue, true)
        }
        val command = CorrelationMessageDto().apply {
            messageName = name
            processVariables = variablesDto
            isAll = true
        }

        return HttpRequest.newBuilder()
            .uri(URI("http://localhost:8080/engine-rest/message"))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .POST(
                HttpRequest.BodyPublishers.ofString(
                    objectMapper.writeValueAsString(command)
                )
            )
            .build()
    }

    fun start(key: String, businessKey: String, variables: Map<String, Any>? = null): HttpRequest {
        val variablesDto = variables?.mapValues {
            val untypedValue = Variables.untypedValue(it.value, false)
            VariableValueDto.fromTypedValue(untypedValue, true)
        }
        val command = StartProcessInstanceDto().apply {
            this.variables = variablesDto
            this.businessKey = businessKey
        }

        return HttpRequest.newBuilder()
            .uri(URI("http://localhost:8080/engine-rest/process-definition/key/${key}/start"))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .POST(
                HttpRequest.BodyPublishers.ofString(
                    objectMapper.writeValueAsString(command)
                )
            )
            .build()
    }

    fun buildClient() = HttpClient.newBuilder()
        .authenticator(object : Authenticator() {
            override fun getPasswordAuthentication() = PasswordAuthentication("admin", "admin".toCharArray())
        })
        .build()
}