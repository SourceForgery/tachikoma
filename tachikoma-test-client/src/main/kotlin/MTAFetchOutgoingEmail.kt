
import com.sourceforgery.tachikoma.config.GrpcClientConfig
import com.sourceforgery.tachikoma.mta.MTAEmailQueueGrpcKt
import com.sourceforgery.tachikoma.mta.MTAQueuedNotification
import com.sourceforgery.tachikoma.provideClientBuilder
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.runBlocking
import java.net.URI

fun main() {
    val backendUri = URI.create(
        System.getenv("TACHI_BACKEND_URI")
            ?: error("Need to specify env TACHI_BACKEND_URI")
    )

    val channel = Channel<MTAQueuedNotification>()

    val apiToken = backendUri.userInfo
    val configuration = object : GrpcClientConfig {
        override val tachikomaUrl = URI(
            System.getenv("TACHI_BACKEND_URI")
                ?: error("Need to specify env TACHI_BACKEND_URI")
        )
        override val insecure: Boolean
            get() = true
        override val clientCert = System.getenv("TACHI_CLIENT_CERT") ?: ""
        override val clientKey = System.getenv("TACHI_CLIENT_KEY") ?: ""
    }
    val stub = provideClientBuilder(configuration)
        .build(MTAEmailQueueGrpcKt.MTAEmailQueueCoroutineStub::class.java)

    try {
        runBlocking {
            stub.getEmailsWithKeepAlive(channel.receiveAsFlow())
                .mapNotNull {
                    if (it.hasEmailMessage()) {
                        it.emailMessage
                    } else {
                        System.err.println(it)
                        null
                    }
                }
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
