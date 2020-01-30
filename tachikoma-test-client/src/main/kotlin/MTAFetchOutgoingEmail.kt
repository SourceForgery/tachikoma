import com.google.protobuf.util.JsonFormat
import com.sourceforgery.tachikoma.mta.EmailMessage
import com.sourceforgery.tachikoma.mta.MTAEmailQueueGrpc
import com.sourceforgery.tachikoma.mta.MTAQueuedNotification
import io.grpc.ManagedChannelBuilder
import io.grpc.Metadata
import io.grpc.stub.MetadataUtils
import io.grpc.stub.StreamObserver
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

private val APITOKEN_HEADER = Metadata.Key.of("x-apitoken", Metadata.ASCII_STRING_MARSHALLER)

fun main(args: Array<String>) {
    val metadataAuth = Metadata()
    metadataAuth.put(APITOKEN_HEADER, System.getenv("BACKEND_API_TOKEN")!!)

    @Suppress("DEPRECATION")
    val channel = ManagedChannelBuilder.forAddress("localhost", 8070)
        .usePlaintext()
        .idleTimeout(365, TimeUnit.DAYS)
        .intercept(MetadataUtils.newAttachHeadersInterceptor(metadataAuth))
        .build()

    val stub = MTAEmailQueueGrpc.newStub(channel)

    try {
        val responseHolder = AtomicReference<StreamObserver<MTAQueuedNotification>>()
        val fromServerStreamObserver = object : StreamObserver<EmailMessage> {
            override fun onError(t: Throwable) {
                t.printStackTrace()
                System.exit(1)
                TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
            }

            override fun onCompleted() {
                responseHolder.get().onCompleted()
            }

            override fun onNext(value: EmailMessage) {
                System.err.println("Got email: " + JsonFormat.printer().print(value))
                for (i in 1..4) {
                    responseHolder.get().onNext(
                        MTAQueuedNotification.newBuilder()
                            .setQueueId("12345A" + i)
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
