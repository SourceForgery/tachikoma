import com.google.protobuf.util.JsonFormat
import com.linecorp.armeria.client.Clients
import com.sourceforgery.jersey.uribuilder.ensureGproto
import com.sourceforgery.jersey.uribuilder.withoutPassword
import com.sourceforgery.tachikoma.mta.MTAEmailQueueGrpcKt
import com.sourceforgery.tachikoma.mta.MTAQueuedNotification
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.runBlocking
import java.net.URI
import java.time.Duration

fun main() {
    val backendUri = URI.create(
        System.getenv("TACHI_BACKEND_URI")
            ?: error("Need to specify env TACHI_BACKEND_URI")
    )

    val apiToken = backendUri.userInfo
    val stub = Clients.builder(backendUri.withoutPassword().ensureGproto())
        .addHeader("x-apitoken", apiToken)
        .responseTimeout(Duration.ofDays(365))
        .writeTimeout(Duration.ofDays(365))
        .build(MTAEmailQueueGrpcKt.MTAEmailQueueCoroutineStub::class.java)

    try {
        val requests = flow {
            for (i in 1..4) {
                emit(
                    MTAQueuedNotification.newBuilder()
                        .setQueueId("12345A$i")
                        .setSuccess(true)
                        .build()
                )
            }
        }
        runBlocking {
            stub.getEmails(requests)
                .take(1)
                .collect { System.err.println("Got email: " + JsonFormat.printer().print(it)) }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    System.err.println("Send complete")
}
