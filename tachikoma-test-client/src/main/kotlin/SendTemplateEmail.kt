import com.google.protobuf.Struct
import com.google.protobuf.Value
import com.google.protobuf.util.JsonFormat
import com.sourceforgery.tachikoma.grpc.frontend.NamedEmail
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.EmailRecipient
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.MailDeliveryServiceGrpc
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.OutgoingEmail
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.TemplateBody
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.TemplateEngine
import io.grpc.ManagedChannelBuilder
import io.grpc.Metadata
import io.grpc.stub.MetadataUtils
import java.time.Instant

private val APITOKEN_HEADER = Metadata.Key.of("x-apitoken", Metadata.ASCII_STRING_MARSHALLER)
private val FRONTEND_API_TOKEN = "example.net:Oufeing2ieth2aequie2ia2ahc3yoonaiw5iey5xifuxoo4tai"

fun main(args: Array<String>) {
    val metadataAuth = Metadata()
    metadataAuth.put(APITOKEN_HEADER, FRONTEND_API_TOKEN)

    val channel = ManagedChannelBuilder.forAddress("localhost", 8070)
            .usePlaintext(true)
            .intercept(MetadataUtils.newAttachHeadersInterceptor(metadataAuth))
            .build()

    val stub = MailDeliveryServiceGrpc.newBlockingStub(channel)

    val template = """
    <div class="entry">
        <h1>{{title}}</h1>
        <div class="body">
            <div>
            {{body}}
            </div>
            <ul>
            {{#items}}
                <li><strong>{{.}}</strong></li>
            {{/items}}
            </ul>
        </div>
    </div>
"""

    val mailTitle = Value.newBuilder().setStringValue("This is a magic title!").build()
    val mailBody = Value.newBuilder().setStringValue("This is a magic mail body!").build()
    val listItems = listOf<Value>(
            Value.newBuilder().setStringValue("Babba").build(),
            Value.newBuilder().setStringValue("Diddi").build()
    )

    val listItemValue = Value.newBuilder().setListValue(Value.newBuilder().listValueBuilder.addAllValues(listItems).build()).build()

    val templateVariables: HashMap<String, Value> = hashMapOf("title" to mailTitle, "body" to mailBody, "items" to listItemValue)

    val globalVars = Struct.newBuilder().putAllFields(templateVariables).build()

    val outgoingEmail = OutgoingEmail.newBuilder()
            .setTemplate(
                    TemplateBody.newBuilder()
                            .setTemplatingEngine(TemplateEngine.HANDLEBARS)
                            .setHtmlTemplate(template)
                            .setGlobalVars(globalVars)
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
