import com.google.protobuf.util.JsonFormat
import com.linecorp.armeria.client.Clients
import com.sourceforgery.jersey.uribuilder.ensureGproto
import com.sourceforgery.jersey.uribuilder.withoutPassword
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.IncomingEmailParameters
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.MailDeliveryServiceGrpcKt
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.runBlocking
import java.net.URI
import java.time.Duration

fun main() {
    val frontendUri = URI.create(
        System.getenv("TACHI_FRONTEND_URI")
            ?: error("Need to specify env TACHI_FRONTEND_URI")
    )

    val apiToken = frontendUri.userInfo
    val stub = Clients.builder(frontendUri.withoutPassword().ensureGproto())
        .addHeader("x-apitoken", apiToken)
        .responseTimeout(Duration.ofDays(365))
        .writeTimeout(Duration.ofDays(365))
        .build(MailDeliveryServiceGrpcKt.MailDeliveryServiceCoroutineStub::class.java)

    try {
        runBlocking {
            stub.streamIncomingEmailsWithKeepAlive(
                IncomingEmailParameters
                    .newBuilder()
                    .setIncludeMessageAttachments(true)
                    .setIncludeMessageHeader(true)
                    .setIncludeMessageParsedBodies(true)
                    .setIncludeMessageWholeEnvelope(true)
                    .build()
            )
                .mapNotNull {
                    if (it.hasIncomingEmail()) {
                        it.incomingEmail
                    } else {
                        System.err.println(it)
                        null
                    }
                }
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
