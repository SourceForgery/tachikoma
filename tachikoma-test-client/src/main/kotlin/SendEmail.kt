import com.google.protobuf.util.JsonFormat
import com.sourceforgery.tachikoma.grpc.frontend.NamedEmail
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.EmailRecipient
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.MailDeliveryServiceGrpc
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.OutgoingEmail
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.StaticBody
import io.grpc.ManagedChannelBuilder
import java.time.Instant

fun main(args: Array<String>) {
    val channel = ManagedChannelBuilder.forAddress("localhost", 8070)
            .usePlaintext(true)
            .build()

    val stub = MailDeliveryServiceGrpc.newBlockingStub(channel)

    val outgoingEmail = OutgoingEmail.newBuilder()
            .setStatic(
                    StaticBody.newBuilder()
                            .setHtmlBody("<body>teteteete</body>")
                            .setSubject("Test email " + Instant.now())
            )
            .addRecipients(EmailRecipient.newBuilder()
                    .setNamedEmail(
                            NamedEmail.newBuilder()
                                    .setEmail("test@example.com")
                                    .setName("This won't work")
                    )
            )
            .setFrom(NamedEmail.newBuilder()
                    .setEmail("test@example.com")
                    .setName("This won't work")
            )
            .build()

    try {
        stub.sendEmail(outgoingEmail).forEach {
            System.err.println(JsonFormat.printer().print(it))
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    System.err.println("Send complete")
}
