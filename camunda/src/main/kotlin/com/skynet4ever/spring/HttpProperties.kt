package com.skynet4ever.spring

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "skynet.http")
data class HttpProperties(
    var connectionTimeoutSecs: Long = 10,
    var readTimeoutSecs: Long = 30,

    /**
     * Additional headers
     */
    var headers: Map<String, String> = emptyMap(),

    /**
     * Well known urls
     */
    var urls: Map<String, String> = emptyMap()
)
