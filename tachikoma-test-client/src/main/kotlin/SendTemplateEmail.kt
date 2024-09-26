import com.google.protobuf.Struct
import com.google.protobuf.Value
import com.google.protobuf.util.JsonFormat
import com.sourceforgery.tachikoma.config.GrpcClientConfig
import com.sourceforgery.tachikoma.grpc.frontend.NamedEmailAddress
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.EmailRecipient
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.MailDeliveryServiceGrpc
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.OutgoingEmail
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.TemplateBody
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.TemplateEngine
import com.sourceforgery.tachikoma.provideClientBuilder
import io.grpc.StatusRuntimeException
import java.net.URI
import java.time.Instant

fun main() {
    val configuration =
        object : GrpcClientConfig {
            override val tachikomaUrl =
                URI(
                    System.getenv("TACHI_FRONTEND_URI")
                        ?: error("Need to specify env TACHI_FRONTEND_URI"),
                )
            override val insecure: Boolean
                get() = true
            override val clientCert = System.getenv("TACHI_CLIENT_CERT") ?: ""
            override val clientKey = System.getenv("TACHI_CLIENT_KEY") ?: ""
        }
    val stub =
        provideClientBuilder(configuration)
            .build(MailDeliveryServiceGrpc.MailDeliveryServiceBlockingStub::class.java)

    val template =
        """
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
    val listItems =
        listOf<Value>(
            Value.newBuilder().setStringValue("Babba").build(),
            Value.newBuilder().setStringValue("Diddi").build(),
        )

    val listItemValue = Value.newBuilder().setListValue(Value.newBuilder().listValueBuilder.addAllValues(listItems).build()).build()

    val templateVariables: HashMap<String, Value> = hashMapOf("title" to mailTitle, "body" to mailBody, "items" to listItemValue)

    val globalVars = Struct.newBuilder().putAllFields(templateVariables).build()

    val outgoingEmail =
        OutgoingEmail.newBuilder()
            .setTemplate(
                TemplateBody.newBuilder()
                    .setTemplatingEngine(TemplateEngine.HANDLEBARS)
                    .setHtmlTemplate(template)
                    .setGlobalVars(globalVars)
                    .setSubject("Test email " + Instant.now()),
            )
            .addRecipients(
                EmailRecipient.newBuilder()
                    .setNamedEmail(
                        NamedEmailAddress.newBuilder()
                            .setEmail("test@example.com")
                            .setName("This won't work"),
                    ),
            )
            .setFrom(
                NamedEmailAddress.newBuilder()
                    .setEmail("test@example.com")
                    .setName("This won't work"),
            )
            .build()

    try {
        stub.sendEmail(outgoingEmail).forEach {
            System.err.println(JsonFormat.printer().print(it))
        }
    } catch (e: Exception) {
        e.printStackTrace()
        (e as? StatusRuntimeException)
            ?.let { System.err.println(e.message) }
    }
    System.err.println("Send complete")
}
