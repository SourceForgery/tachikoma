import com.google.protobuf.util.JsonFormat
import com.linecorp.armeria.client.Clients
import com.sourceforgery.jersey.uribuilder.ensureGproto
import com.sourceforgery.jersey.uribuilder.withoutPassword
import com.sourceforgery.tachikoma.grpc.frontend.EmailNotification
import com.sourceforgery.tachikoma.grpc.frontend.tracking.DeliveryNotificationServiceGrpc
import com.sourceforgery.tachikoma.grpc.frontend.tracking.NotificationStreamParameters
import io.grpc.stub.StreamObserver
import java.net.URI
import java.time.Duration

fun main(args: Array<String>) {
    val frontendUri = URI.create(
        System.getenv("TACHI_FRONTEND_URI")
            ?: error("Need to specify env TACHI_FRONTEND_URI")
    )

    val apiToken = frontendUri.userInfo
    val stub = Clients.builder(frontendUri.withoutPassword().ensureGproto())
        .addHeader("x-apitoken", apiToken)
        .responseTimeout(Duration.ofDays(365))
        .writeTimeout(Duration.ofDays(365))
        .build(DeliveryNotificationServiceGrpc.DeliveryNotificationServiceStub::class.java)

    try {
        val fromServerStreamObserver = object : StreamObserver<EmailNotification> {
            override fun onError(t: Throwable) {
                t.printStackTrace()
                System.exit(1)
                TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
            }

            override fun onCompleted() {
                System.err.println("On complete called!")
            }

            override fun onNext(value: EmailNotification) {
                System.err.println("Got email: " + JsonFormat.printer().print(value))
            }
        }
        stub.notificationStream(NotificationStreamParameters.getDefaultInstance(), fromServerStreamObserver)
        Thread.sleep(100000000L)
    } catch (e: Exception) {
        e.printStackTrace()
    }
    System.err.println("Done complete")
}
