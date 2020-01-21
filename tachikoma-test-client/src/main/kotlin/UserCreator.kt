
import com.sourceforgery.tachikoma.grpc.frontend.EmailAddress
import com.sourceforgery.tachikoma.grpc.frontend.blockedemail.AddUserRequest
import com.sourceforgery.tachikoma.grpc.frontend.blockedemail.FrontendUserRole
import com.sourceforgery.tachikoma.grpc.frontend.blockedemail.PasswordAuth
import com.sourceforgery.tachikoma.grpc.frontend.blockedemail.UserServiceGrpc
import io.grpc.ManagedChannel
import io.grpc.Metadata
import io.grpc.netty.GrpcSslContexts
import io.grpc.netty.NettyChannelBuilder
import io.grpc.stub.MetadataUtils
import java.net.URI
import java.util.UUID

private val APITOKEN_HEADER = Metadata.Key.of("x-apitoken", Metadata.ASCII_STRING_MARSHALLER)

private fun addPort(uri: URI): URI {
    val query = uri.rawQuery?.let { "?$it" } ?: ""
    val port: Int =
            if (uri.port == -1) {
                when (uri.scheme) {
                    "http" -> 80
                    "https" -> 443
                    else -> throw IllegalArgumentException("Unknown proto's default port is unknown")
                }
            } else {
                uri.port
            }
    return URI.create("${uri.scheme}://${uri.userInfo}@${uri.host}:$port${uri.path ?: "/"}$query")
}

fun createChannel(tachikomaUrl: URI): ManagedChannel {
    val tachikomaUrlWithPort = addPort(tachikomaUrl)
    val plaintext = tachikomaUrlWithPort.scheme == "http"
    val metadataAuth = Metadata()
    metadataAuth.put(APITOKEN_HEADER, tachikomaUrlWithPort.userInfo!!)

    return NettyChannelBuilder.forAddress(tachikomaUrlWithPort.host, tachikomaUrlWithPort.port)
            .intercept(MetadataUtils.newAttachHeadersInterceptor(metadataAuth))
            .apply {
                if (plaintext) {
                    usePlaintext()
                } else {
                    useTransportSecurity()
                    sslContext(
                            GrpcSslContexts.forClient().build()
                    )
                }
            }
            .build()
}

fun main(args: Array<String>) {
    val tachikomaUrl = URI.create(System.getenv("TACHIKOMA_URL"))

    val email = args[0]
    if (!email.contains("@")) {
        throw IllegalArgumentException("First argument is not an email")
    }

    val channel = createChannel(tachikomaUrl)

    val stub = UserServiceGrpc.newBlockingStub(channel)

    try {
        val response = stub.addFrontendUser(AddUserRequest.newBuilder()
                .apply {
                    passwordAuth = PasswordAuth.newBuilder().apply {
                        login = email.substringBefore('@')
                        password = UUID.randomUUID().toString()
                    }.build()
                    recipientOverride = EmailAddress.newBuilder().setEmail(args[0]).build()
                    active = true
                    mailDomain = tachikomaUrl.authority.substringBefore(":")
                    authenticationRole = FrontendUserRole.FRONTEND
                    addApiToken = true
                }.build()
        )
        System.err.println(response)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}