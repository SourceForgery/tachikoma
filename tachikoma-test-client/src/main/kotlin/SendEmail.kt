import com.google.protobuf.util.JsonFormat
import com.sourceforgery.tachikoma.grpc.frontend.NamedEmailAddress
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.EmailRecipient
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.MailDeliveryServiceGrpc
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.OutgoingEmail
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.StaticBody
import io.grpc.ManagedChannelBuilder
import io.grpc.Metadata
import io.grpc.Metadata.ASCII_STRING_MARSHALLER
import io.grpc.stub.MetadataUtils
import java.net.URI
import java.time.Instant
import javax.mail.internet.InternetAddress

private val APITOKEN_HEADER = Metadata.Key.of("x-apitoken", ASCII_STRING_MARSHALLER)

fun main(args: Array<String>) {
    val metadataAuth = Metadata()
    val frontendUri = URI.create(
        System.getenv("TACHI_FRONTEND_URI")
            ?: error("Need to specifify env TACHI_FRONTEND_URI")
    )
    val from = InternetAddress(
        System.getenv("TACHI_FROM")
            ?: error("Need to specifify env TACHI_FROM")
    )
    val to = InternetAddress.parse(
        System.getenv("TACHI_TO")
            ?: error("Need to specifify env TACHI_TO (multiple addresses possible via RFC822 format)")
    )

    metadataAuth.put(APITOKEN_HEADER, frontendUri.authority)

    val https = when (frontendUri.scheme) {
        "https" -> true
        "http" -> false
        else -> error("Only supports http or https")
    }

    val port = if (frontendUri.port != -1) {
        frontendUri.port
    } else {
        if (https) {
            443
        } else {
            80
        }
    }
    @Suppress("DEPRECATION")
    val channel = ManagedChannelBuilder.forAddress(frontendUri.host, port)
        .apply {
            if (!https) {
                usePlaintext()
            }
        }
        .intercept(MetadataUtils.newAttachHeadersInterceptor(metadataAuth))
        .build()

    val stub = MailDeliveryServiceGrpc.newBlockingStub(channel)

    val mailBody = """
    |<body>
    |    <center>
    |        <!-- HEADER LOGO BLOCK -->
    |        <div>
    |            <a href="http://www.google.com" target="_blank" style="text-decoration: none;color: #50cad0;">
    |                <img src="http://lorempixel.com/400/200/sports/1/" style="width: 150px;margin-top: 15px;margin-bottom: 5px;">
    |            </a>
    |        </div>
    |        <!-- HEADER LOGO BLOCK -->
    |
    |        <div>
    |            <h1>The mail header</h1>
    |        </div>
    |
    |        <div>
    |            <br>
    |            <span>Are you the one?</span>
    |            <h1>
    |                Acme Inc
    |            </h1>
    |            <p>
    |                Lorem ipsum dolor sit amet, erat sit eu aenean atque leo, nunc mauris justo eros vel, sed ac in. Wisi nulla elit eget nam hymenaeos aliquam, in ut iaculis, ac arcu fringilla varius et. Mattis in. Tincidunt suscipit, lorem massa nunc nullam. Hendrerit ac dui volutpat, dapibus hac arcu donec ipsum luctus mollis. Ligula accumsan pellentesque, sit facilisis libero mi. Sit vel etiam, eget velit.
    |            </p>
    |            <p class="center" style="font-size: 16px;color: #626262;padding: 20px;margin: 0;text-align: center;">
    |                Acme Inc AB Ghmb.
    |            </p>
    |            <a class="button" href="https://localhost:3001/api/login/asdf" target="_blank" style="text-decoration: none;color: #fff;display: inline-block;background-color: #fd844a;padding: 10px 16px;border-radius: 2px;font-size: 16px;font-weight: 500;border: none;margin-top: 20px;margin-bottom: 20px;">
    |                Go to questions
    |            </a>
    |            <br><br><br>
    |            <h6>Some footer text</h6>
    |        </div>
    |    </center>
    |</body>
    """.trimMargin()

    val outgoingEmail = OutgoingEmail.newBuilder()
        .setStatic(
            StaticBody.newBuilder()
                .setHtmlBody(mailBody)
                .setSubject("Test email öåäöäåöäåöåäöäå 日本." + Instant.now())
        )
        .addAllRecipients(to.map {
            EmailRecipient.newBuilder()
                .setNamedEmail(
                    NamedEmailAddress.newBuilder()
                        .setEmail(it.address)
                        .setName(it.personal)
                )
                .build()
        })
        .setFrom(NamedEmailAddress.newBuilder()
            .setEmail(from.address)
            .setName(from.personal)
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
