import com.google.protobuf.util.JsonFormat
import com.sourceforgery.tachikoma.config.GrpcClientConfig
import com.sourceforgery.tachikoma.grpc.frontend.tracking.DeliveryNotificationServiceGrpcKt
import com.sourceforgery.tachikoma.grpc.frontend.tracking.NotificationStreamParameters
import com.sourceforgery.tachikoma.provideClientBuilder
import kotlinx.coroutines.runBlocking
import java.net.URI

fun main() {
    val configuration = object : GrpcClientConfig {
        override val tachikomaUrl = URI(
            System.getenv("TACHI_FRONTEND_URI")
                ?: error("Need to specify env TACHI_FRONTEND_URI")
        )
        override val insecure: Boolean
            get() = true
        override val clientCert = System.getenv("TACHI_CLIENT_CERT") ?: ""
        override val clientKey = System.getenv("TACHI_CLIENT_KEY") ?: ""
    }
    val stub = provideClientBuilder(configuration)
        .build(DeliveryNotificationServiceGrpcKt.DeliveryNotificationServiceCoroutineStub::class.java)

    try {
        runBlocking {
            stub.notificationStreamWithKeepAlive(NotificationStreamParameters.getDefaultInstance())
                .collect {
                    System.err.println("Got email: " + JsonFormat.printer().print(it))
                }
            System.err.println("On complete called!")
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    System.err.println("Done complete")
}
