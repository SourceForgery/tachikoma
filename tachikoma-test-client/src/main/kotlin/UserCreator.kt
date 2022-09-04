
import com.sourceforgery.tachikoma.config.GrpcClientConfig
import com.sourceforgery.tachikoma.grpc.frontend.EmailAddress
import com.sourceforgery.tachikoma.grpc.frontend.blockedemail.AddUserRequest
import com.sourceforgery.tachikoma.grpc.frontend.blockedemail.FrontendUserRole
import com.sourceforgery.tachikoma.grpc.frontend.blockedemail.PasswordAuth
import com.sourceforgery.tachikoma.grpc.frontend.blockedemail.UserServiceGrpc
import com.sourceforgery.tachikoma.provideClientBuilder
import java.net.URI
import java.util.UUID

fun main(args: Array<String>) {
    val email = args[0]
    if (!email.contains("@")) {
        throw IllegalArgumentException("First argument is not an email")
    }
    val configuration = object : GrpcClientConfig {
        override val tachikomaUrl = URI(
            System.getenv("TACHI_FRONTEND_URL")
                ?: error("Need to specify env TACHI_FRONTEND")
        )
        override val insecure: Boolean
            get() = true
        override val clientCert = System.getenv("TACHI_CLIENT_CERT") ?: ""
        override val clientKey = System.getenv("TACHI_CLIENT_KEY") ?: ""
    }

    val stub = provideClientBuilder(configuration)
        .build(UserServiceGrpc.UserServiceBlockingStub::class.java)

    try {
        val response = stub.addFrontendUser(
            AddUserRequest.newBuilder()
                .apply {
                    passwordAuth = PasswordAuth.newBuilder().apply {
                        login = email.substringBefore('@')
                        password = UUID.randomUUID().toString()
                    }.build()
                    recipientOverride = EmailAddress.newBuilder().setEmail(args[0]).build()
                    active = true
                    mailDomain = configuration.tachikomaUrl.authority.substringBefore(":")
                    authenticationRole = FrontendUserRole.FRONTEND
                    addApiToken = true
                }.build()
        )
        System.err.println(response)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
