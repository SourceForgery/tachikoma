import com.google.protobuf.util.JsonFormat
import com.linecorp.armeria.client.Clients
import com.sourceforgery.jersey.uribuilder.ensureGproto
import com.sourceforgery.jersey.uribuilder.withoutPassword
import com.sourceforgery.tachikoma.mta.EmailMessage
import com.sourceforgery.tachikoma.mta.MTAEmailQueueGrpc
import com.sourceforgery.tachikoma.mta.MTAQueuedNotification
import io.grpc.stub.StreamObserver
import java.net.URI
import java.time.Duration
import java.util.concurrent.atomic.AtomicReference
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val backendUri = URI.create(
        System.getenv("TACHI_BACKEND_URI")
            ?: error("Need to specify env TACHI_BACKEND_URI")
    )

    val apiToken = backendUri.userInfo
    val stub = Clients.builder(backendUri.withoutPassword().ensureGproto())
        .addHeader("x-apitoken", apiToken)
        .responseTimeout(Duration.ofDays(365))
        .writeTimeout(Duration.ofDays(365))
        .build(MTAEmailQueueGrpc.MTAEmailQueueStub::class.java)

    try {
        val responseHolder = AtomicReference<StreamObserver<MTAQueuedNotification>>()
        val fromServerStreamObserver = object : StreamObserver<EmailMessage> {
            override fun onError(t: Throwable) {
                t.printStackTrace()
                exitProcess(1)
            }

            override fun onCompleted() {
                responseHolder.get().onCompleted()
            }

            override fun onNext(value: EmailMessage) {
                System.err.println("Got email: " + JsonFormat.printer().print(value))
                for (i in 1..4) {
                    responseHolder.get().onNext(
                        MTAQueuedNotification.newBuilder()
                            .setQueueId("12345A$i")
                            .setSuccess(true)
                            .build()
                    )
                }
            }
        }
        val response: StreamObserver<MTAQueuedNotification> = stub.getEmails(fromServerStreamObserver)
        responseHolder.set(response)
        Thread.sleep(100000000L)
    } catch (e: Exception) {
        e.printStackTrace()
    }
    System.err.println("Send complete")
}
