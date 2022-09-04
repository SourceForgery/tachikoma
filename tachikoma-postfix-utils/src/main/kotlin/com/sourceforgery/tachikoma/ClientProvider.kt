package com.sourceforgery.tachikoma

import com.linecorp.armeria.client.ClientBuilder
import com.linecorp.armeria.client.ClientFactory
import com.linecorp.armeria.client.Clients
import com.sourceforgery.jersey.uribuilder.ensureGproto
import com.sourceforgery.jersey.uribuilder.withoutPassword
import com.sourceforgery.tachikoma.config.GrpcClientConfig
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import java.io.File
import java.time.Duration

fun provideClientBuilder(grpcClientConfig: GrpcClientConfig): ClientBuilder {
    val apiToken = grpcClientConfig.tachikomaUrl.userInfo
    return Clients.builder(grpcClientConfig.tachikomaUrl.withoutPassword().ensureGproto())
        .factory(
            ClientFactory.builder()
                .tlsCustomizer { sslCtxBuilder ->
                    if (grpcClientConfig.clientCert.isNotBlank() && grpcClientConfig.clientKey.isNotBlank()) {
                        sslCtxBuilder.keyManager(File(grpcClientConfig.clientCert), File(grpcClientConfig.clientKey))
                    }
                    if (grpcClientConfig.insecure) {
                        sslCtxBuilder.trustManager(InsecureTrustManagerFactory.INSTANCE)
                    }
                }
                .build()
        )
        .addHeader("x-apitoken", apiToken)
        .responseTimeout(Duration.ofDays(365))
        .writeTimeout(Duration.ofDays(365))
        .maxResponseLength(0)
}
