package com.skynet4ever.spring

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.net.http.HttpClient
import java.time.Duration

@Configuration
open class Config {

    /**
     * Case insensitive map of well known urls
     */
    @Bean("urls")
    open fun urls(httpProperties: HttpProperties): Map<String, String> =
        httpProperties.urls.toSortedMap(String.CASE_INSENSITIVE_ORDER)

    @Bean
    open fun httpClient(httpProperties: HttpProperties): HttpClient =
        HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(httpProperties.connectionTimeoutSecs))
            .build()

    @Bean
    open fun objectMapper(): ObjectMapper = ObjectMapper().registerModule(KotlinModule())
}
