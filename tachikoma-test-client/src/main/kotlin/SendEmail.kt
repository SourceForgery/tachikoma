import com.google.protobuf.Timestamp
import com.google.protobuf.util.JsonFormat
import com.linecorp.armeria.client.Clients
import com.sourceforgery.jersey.uribuilder.ensureGproto
import com.sourceforgery.jersey.uribuilder.withoutPassword
import com.sourceforgery.tachikoma.grpc.frontend.NamedEmailAddress
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.EmailRecipient
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.MailDeliveryServiceGrpc
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.OutgoingEmail
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.StaticBody
import jakarta.mail.internet.InternetAddress
import java.net.URI
import java.time.Duration
import java.time.Instant

fun main(args: Array<String>) {
    val frontendUri = URI.create(
        System.getenv("TACHI_FRONTEND_URI")
            ?: error("Need to specify env TACHI_FRONTEND_URI")
    )
    val from = InternetAddress(
        System.getenv("TACHI_FROM")
            ?: error("Need to specify env TACHI_FROM")
    )
    val to = InternetAddress.parse(
        System.getenv("TACHI_TO")
            ?: error("Need to specify env TACHI_TO (multiple addresses possible via RFC822 format)")
    )

    val apiToken = frontendUri.userInfo
    val stub = Clients.builder(frontendUri.withoutPassword().ensureGproto())
        .addHeader("x-apitoken", apiToken)
        .responseTimeout(Duration.ofDays(365))
        .writeTimeout(Duration.ofDays(365))
        .build(MailDeliveryServiceGrpc.MailDeliveryServiceBlockingStub::class.java)

    val mailBody =
        """
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
    |            <a class="button" href="https://google.com/" target="_blank" style="text-decoration: none;color: #fff;display: inline-block;background-color: #fd844a;padding: 10px 16px;border-radius: 2px;font-size: 16px;font-weight: 500;border: none;margin-top: 20px;margin-bottom: 20px;">
    |                Go to questions
    |            </a>
    |            <br><br><br>
    |            <h6>Some footer text</h6>
    |        </div>
    |        <a href="*|UNSUB|*">Unsubscribe from list</a>
    |    </center>
    |</body>
    """.trimMargin()

    val outgoingEmail = OutgoingEmail.newBuilder()
        .setUnsubscribeRedirectUri("https://google.com/?q=unsubscribed")
        .setStatic(
            StaticBody.newBuilder()
                .setHtmlBody(mailBody)
                .setSubject("Application for öåäöäåöäåöåäöäå 日本." + Instant.now())
        )
        .setSendAt(Instant.now().plusSeconds(60).toTimestamp())
        .addAllRecipients(
            to.map {
                EmailRecipient.newBuilder()
                    .setNamedEmail(
                        NamedEmailAddress.newBuilder()
                            .setEmail(it.address)
                            .setName(it.personal ?: "")
                    )
                    .build()
            }
        )
        .setFrom(
            NamedEmailAddress.newBuilder()
                .setEmail(from.address)
                .setName(from.personal ?: "")
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

fun Instant.toTimestamp() =
    Timestamp.newBuilder()
        .setSeconds(this.epochSecond)
        .setNanos(this.nano)
        .build()
