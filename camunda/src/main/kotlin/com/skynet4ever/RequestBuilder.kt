package com.skynet4ever

import com.skynet4ever.utils.Logging
import com.skynet4ever.variables.HttpVariables
import org.springframework.stereotype.Service
import java.net.URI
import java.net.http.HttpRequest
import java.net.http.HttpRequest.BodyPublisher
import java.time.Duration

@Service
class RequestBuilder {
    companion object : Logging {
        val noBody: BodyPublisher = HttpRequest.BodyPublishers.noBody()
    }

    fun build(variables: HttpVariables): HttpRequest {
        logger.debug("Building HTTP request...")
        val bodyPublisher = variables.body
            ?.let(HttpRequest.BodyPublishers::ofString)
            ?: noBody

        logger.trace("Method '{}'", variables.method)
        logger.trace("Url '{}'", variables.url)
        val builder = HttpRequest.newBuilder()
            .uri(URI(variables.url))
            .method(variables.method, bodyPublisher)

        variables.headers.forEach { (header, value) ->
            builder.setHeader(header, value)
        }

        val request = builder
            .timeout(Duration.ofSeconds(variables.readTimeoutSecs))
            .build()
        logger.debug("Finished building HTTP request")
        return request
    }
}