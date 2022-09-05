package com.sourceforgery.tachikoma

import com.linecorp.armeria.client.ClientBuilder
import com.linecorp.armeria.client.ClientFactory
import com.linecorp.armeria.client.Clients
import com.sourceforgery.jersey.uribuilder.ensureGproto
import com.sourceforgery.jersey.uribuilder.withoutPassword
import com.sourceforgery.tachikoma.ClientProvider.LOGGER
import com.sourceforgery.tachikoma.config.GrpcClientConfig
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import org.apache.logging.log4j.kotlin.logger
import java.io.File
import java.time.Duration

fun provideClientBuilder(grpcClientConfig: GrpcClientConfig): ClientBuilder {
    val apiToken: String? = grpcClientConfig.tachikomaUrl.userInfo
    return Clients.builder(grpcClientConfig.tachikomaUrl.withoutPassword().ensureGproto())
        .factory(
            ClientFactory.builder()
                .tlsCustomizer { sslCtxBuilder ->
                    if (grpcClientConfig.clientCert.isNotBlank() && grpcClientConfig.clientKey.isNotBlank()) {
                        LOGGER.info { "Using client certs in '${grpcClientConfig.clientCert}' and '${grpcClientConfig.clientKey}'" }
                        sslCtxBuilder.keyManager(File(grpcClientConfig.clientCert), File(grpcClientConfig.clientKey))
                    }
                    if (grpcClientConfig.insecure) {
                        LOGGER.info { "Using insecure connection to tachikoma webserver" }
                        sslCtxBuilder.trustManager(InsecureTrustManagerFactory.INSTANCE)
                    }
                }
                .build()
        )
        .onlyIf(apiToken != null) {
            addHeader("x-apitoken", apiToken!!)
        }
        .responseTimeout(Duration.ofDays(365))
        .writeTimeout(Duration.ofDays(365))
        .maxResponseLength(0)
}

private object ClientProvider {
    val LOGGER = logger()
}
