package com.sourceforgery.tachikoma.webserver

import com.linecorp.armeria.common.HttpMethod
import com.linecorp.armeria.common.HttpRequest
import com.linecorp.armeria.common.HttpResponse
import com.linecorp.armeria.common.RequestContext
import com.linecorp.armeria.common.SessionProtocol
import com.linecorp.armeria.common.grpc.GrpcSerializationFormats
import com.linecorp.armeria.server.DecoratingServiceFunction
import com.linecorp.armeria.server.ServerBuilder
import com.linecorp.armeria.server.ServiceRequestContext
import com.linecorp.armeria.server.cors.CorsServiceBuilder
import com.linecorp.armeria.server.grpc.GrpcServiceBuilder
import com.linecorp.armeria.server.healthcheck.HttpHealthCheckService
import com.sourceforgery.rest.RestBinder
import com.sourceforgery.rest.RestService
import com.sourceforgery.tachikoma.CommonBinder
import com.sourceforgery.tachikoma.DatabaseBinder
import com.sourceforgery.tachikoma.GrpcBinder
import com.sourceforgery.tachikoma.hk2.HK2RequestContext
import com.sourceforgery.tachikoma.hk2.SettableReference
import com.sourceforgery.tachikoma.mq.MqBinder
import com.sourceforgery.tachikoma.startup.StartupBinder
import com.sourceforgery.tachikoma.webserver.hk2.HTTP_REQUEST_TYPE
import com.sourceforgery.tachikoma.webserver.hk2.REQUEST_CONTEXT_TYPE
import com.sourceforgery.tachikoma.webserver.hk2.WebBinder
import io.grpc.BindableService
import io.grpc.ForwardingServerCallListener
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import io.grpc.ServerInterceptors
import org.glassfish.hk2.utilities.ServiceLocatorUtilities
import java.util.function.Function


@Suppress("unused")
fun main(vararg args: String) {

    val serviceLocator = ServiceLocatorUtilities.bind(
            CommonBinder(),
            StartupBinder(),
            RestBinder(),
            MqBinder(),
            GrpcBinder(),
            DatabaseBinder(),
            WebBinder()
    )!!
    val hK2RequestContext = serviceLocator.getService(HK2RequestContext::class.java)

    val scopedHttpRequest = serviceLocator.getService<SettableReference<HttpRequest>>(HTTP_REQUEST_TYPE)
    val scopedRequestContext = serviceLocator.getService<SettableReference<RequestContext>>(REQUEST_CONTEXT_TYPE)

    val requestScoped = DecoratingServiceFunction<HttpRequest, HttpResponse> { delegate, ctx, req ->
        hK2RequestContext.runInScope {
            scopedHttpRequest.value = req
            scopedRequestContext.value = ctx
            delegate.serve(ctx, req)
        }
    }

    val grpcServiceBuilder = GrpcServiceBuilder()
            .supportedSerializationFormats(GrpcSerializationFormats.values())!!
    val headerServerInterceptor = HK2RequestServerInterceptor(
            hK2RequestContext = hK2RequestContext,
            scopedRequestContext = scopedRequestContext,
            scopedHttpRequest = scopedHttpRequest
    )
    for (grpcService in serviceLocator.getAllServices(BindableService::class.java)) {
        grpcServiceBuilder.addService(ServerInterceptors.intercept(grpcService, headerServerInterceptor))
    }

    val healthService = CorsServiceBuilder
            .forAnyOrigin()
            .allowNullOrigin()
            .allowCredentials()
            .allowRequestMethods(HttpMethod.GET)
            .build(object : HttpHealthCheckService() {})!!

    // Order matters!
    val serverBuilder = ServerBuilder()
            .serviceUnder("/health", healthService)
    for (restService in serviceLocator.getAllServices(RestService::class.java)) {
        serverBuilder.annotatedService("/", restService)
    }

    val grpcService = grpcServiceBuilder.build()!!

    serverBuilder
            // Grpc must be last
            .decorator(Function { it.decorate(requestScoped) })
            .serviceUnder("/", grpcService)
            .port(8070, SessionProtocol.HTTP)
            .build()
            .start()
            .join()
}

internal class HK2RequestServerInterceptor(
        private val hK2RequestContext: HK2RequestContext,
        private val scopedHttpRequest: SettableReference<HttpRequest>,
        private val scopedRequestContext: SettableReference<RequestContext>
) : ServerInterceptor {
    override fun <ReqT, RespT> interceptCall(
            call: ServerCall<ReqT, RespT>,
            requestHeaders: Metadata,
            next: ServerCallHandler<ReqT, RespT>
    ): ServerCall.Listener<ReqT> {
        val startCall = next.startCall(call, requestHeaders)
        return object : ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(startCall) {
            override fun onHalfClose() {
                hK2RequestContext.runInScope {
                    scopedRequestContext.value = RequestContext.current()
                    scopedRequestContext.value = RequestContext.current<RequestContext>().request()
                    super.onHalfClose()
                }
            }
        }
    }
}