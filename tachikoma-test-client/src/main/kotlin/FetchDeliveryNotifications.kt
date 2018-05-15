import com.google.protobuf.util.JsonFormat
import com.sourceforgery.tachikoma.grpc.frontend.EmailNotification
import com.sourceforgery.tachikoma.grpc.frontend.tracking.DeliveryNotificationServiceGrpc
import com.sourceforgery.tachikoma.grpc.frontend.tracking.NotificationStreamParameters
import io.grpc.ManagedChannelBuilder
import io.grpc.Metadata
import io.grpc.stub.MetadataUtils
import io.grpc.stub.StreamObserver
import java.util.concurrent.TimeUnit

private val APITOKEN_HEADER = Metadata.Key.of("x-apitoken", Metadata.ASCII_STRING_MARSHALLER)

fun main(args: Array<String>) {
    val metadataAuth = Metadata()
    metadataAuth.put(APITOKEN_HEADER, System.getenv("FRONTEND_API_TOKEN")!!)

    val channel = ManagedChannelBuilder.forAddress("localhost", 8070)
        .usePlaintext(true)
        .idleTimeout(365, TimeUnit.DAYS)
        .intercept(MetadataUtils.newAttachHeadersInterceptor(metadataAuth))
        .build()

    val stub = DeliveryNotificationServiceGrpc.newStub(channel)

    try {
        val fromServerStreamObserver = object : StreamObserver<EmailNotification> {
            override fun onError(t: Throwable) {
                t.printStackTrace()
                System.exit(1)
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
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