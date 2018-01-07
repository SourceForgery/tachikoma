import com.google.protobuf.Empty
import com.google.protobuf.util.JsonFormat
import com.sourceforgery.tachikoma.grpc.frontend.NamedEmail
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.EmailRecipient
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.MailDeliveryServiceGrpc
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.OutgoingEmail
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.StaticBody
import com.sourceforgery.tachikoma.mta.MTAEmailQueueGrpc
import io.grpc.ManagedChannelBuilder
import java.time.Instant

fun main(args: Array<String>) {
    val channel = ManagedChannelBuilder.forAddress("localhost", 8070)
            .usePlaintext(true)
            .build()

    val stub = MTAEmailQueueGrpc.newBlockingStub(channel)

    try {
        stub.getEmails(Empty.getDefaultInstance()).forEach {
            System.err.println("Got email: " + JsonFormat.printer().print(it))
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    System.err.println("Send complete")
}