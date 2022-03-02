
import com.linecorp.armeria.client.Clients
import com.sourceforgery.jersey.uribuilder.ensureGproto
import com.sourceforgery.jersey.uribuilder.withoutPassword
import com.sourceforgery.tachikoma.mta.MTAEmailQueueGrpcKt
import com.sourceforgery.tachikoma.mta.MTAQueuedNotification
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.runBlocking
import java.net.URI
import java.time.Duration

fun main() {
    val backendUri = URI.create(
        System.getenv("TACHI_BACKEND_URI")
            ?: error("Need to specify env TACHI_BACKEND_URI")
    )

    val channel = Channel<MTAQueuedNotification>()

    val apiToken = backendUri.userInfo
    val stub = Clients.builder(backendUri.withoutPassword().ensureGproto())
        .addHeader("x-apitoken", apiToken)
        .responseTimeout(Duration.ofDays(365))
        .writeTimeout(Duration.ofDays(365))
        .build(MTAEmailQueueGrpcKt.MTAEmailQueueCoroutineStub::class.java)

    try {
        runBlocking {
            stub.getEmails(channel.receiveAsFlow())
                .collect {
                    // System.err.println("Got email: " + JsonFormat.printer().print(it))
                    for (i in 1..4) {
                        channel.send(
                            MTAQueuedNotification.newBuilder()
                                .setQueueId("12345A$i")
                                .setSuccess(true)
                                .build()
                        )
                    }
                }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    System.err.println("Send complete")
}
