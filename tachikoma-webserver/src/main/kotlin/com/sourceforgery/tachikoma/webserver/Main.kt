package com.sourceforgery.tachikoma.webserver

import com.linecorp.armeria.common.HttpMethod
import com.linecorp.armeria.common.HttpRequest
import com.linecorp.armeria.common.HttpResponse
import com.linecorp.armeria.common.RequestContext
import com.linecorp.armeria.common.SessionProtocol
import com.linecorp.armeria.common.grpc.GrpcSerializationFormats
import com.linecorp.armeria.server.DecoratingServiceFunction
import com.linecorp.armeria.server.ServerBuilder
import com.linecorp.armeria.server.cors.CorsServiceBuilder
import com.linecorp.armeria.server.grpc.GrpcServiceBuilder
import com.linecorp.armeria.server.healthcheck.HttpHealthCheckService
import com.sourceforgery.rest.RestBinder
import com.sourceforgery.rest.RestService
import com.sourceforgery.tachikoma.CommonBinder
import com.sourceforgery.tachikoma.DatabaseBinder
import com.sourceforgery.tachikoma.GrpcBinder
import com.sourceforgery.tachikoma.hk2.HK2RequestContextImpl
import com.sourceforgery.tachikoma.hk2.HK2RequestContextImpl.Instance
import com.sourceforgery.tachikoma.hk2.SettableReference
import com.sourceforgery.tachikoma.mq.JobWorker
import com.sourceforgery.tachikoma.mq.MessageQueue
import com.sourceforgery.tachikoma.mq.MqBinder
import com.sourceforgery.tachikoma.startup.StartupBinder
import com.sourceforgery.tachikoma.webserver.hk2.HTTP_REQUEST_TYPE
import com.sourceforgery.tachikoma.webserver.hk2.REQUEST_CONTEXT_TYPE
import com.sourceforgery.tachikoma.webserver.hk2.WebBinder
import io.ebean.EbeanServer
import io.grpc.BindableService
import io.netty.util.AttributeKey
import org.glassfish.hk2.utilities.ServiceLocatorUtilities
import java.time.Duration
import java.util.function.Consumer
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
    val hK2RequestContext = serviceLocator.getService(HK2RequestContextImpl::class.java)

    listOf(
            MessageQueue::class.java,
            EbeanServer::class.java
    )
            // Yes, parallal stream is broken by design, but here it should work
            .parallelStream()
            .forEach({ serviceLocator.getService(it) })

    val requestScoped = DecoratingServiceFunction<HttpRequest, HttpResponse> { delegate, ctx, req ->
        val oldHk2Ctx = hK2RequestContext.retrieveCurrent()
        ctx.attr(OLD_HK2_CONTEXT_KEY).set(oldHk2Ctx)
        val hk2Ctx = hK2RequestContext.createInstance()
        ctx.attr(HK2_CONTEXT_KEY).set(hk2Ctx)
        ctx.onEnter(Consumer {
            hK2RequestContext.setCurrent(hk2Ctx)
        })
        ctx.onExit(Consumer {
            hK2RequestContext.resumeCurrent(oldHk2Ctx)
        })
        hK2RequestContext.runInScope(hk2Ctx, {
            serviceLocator
                    .getService<SettableReference<HttpRequest>>(HTTP_REQUEST_TYPE)
                    .value = req
            serviceLocator
                    .getService<SettableReference<RequestContext>>(REQUEST_CONTEXT_TYPE)
                    .value = ctx
            delegate.serve(ctx, req)
        })
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

    val grpcServiceBuilder = GrpcServiceBuilder().supportedSerializationFormats(GrpcSerializationFormats.values())!!
    for (grpcService in serviceLocator.getAllServices(BindableService::class.java)) {
        grpcServiceBuilder.addService(grpcService)
    }
    val grpcService = grpcServiceBuilder.build()!!

    serviceLocator.getService(JobWorker::class.java).work()

    serverBuilder
            // Grpc must be last
            .decorator(Function { it.decorate(requestScoped) })
            .serviceUnder("/", grpcService)
            .port(8070, SessionProtocol.HTTP)
            .defaultRequestTimeout(Duration.ofDays(365))
            .build()
            .start()
            .join()
}

private val HK2_CONTEXT_KEY = AttributeKey.valueOf<Instance>("HK2_CONTEXT")
private val OLD_HK2_CONTEXT_KEY = AttributeKey.valueOf<Instance>("OLD_HK2_CONTEXT")