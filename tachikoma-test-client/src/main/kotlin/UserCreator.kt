
import com.linecorp.armeria.client.Clients
import com.sourceforgery.jersey.uribuilder.ensureGproto
import com.sourceforgery.jersey.uribuilder.withoutPassword
import com.sourceforgery.tachikoma.grpc.frontend.EmailAddress
import com.sourceforgery.tachikoma.grpc.frontend.blockedemail.AddUserRequest
import com.sourceforgery.tachikoma.grpc.frontend.blockedemail.FrontendUserRole
import com.sourceforgery.tachikoma.grpc.frontend.blockedemail.PasswordAuth
import com.sourceforgery.tachikoma.grpc.frontend.blockedemail.UserServiceGrpc
import java.net.URI
import java.util.UUID

fun main(args: Array<String>) {
    val tachikomaUrl = URI.create(System.getenv("TACHIKOMA_URL"))

    val email = args[0]
    if (!email.contains("@")) {
        throw IllegalArgumentException("First argument is not an email")
    }

    val apiToken = tachikomaUrl.userInfo
    val stub = Clients.builder(tachikomaUrl.withoutPassword().ensureGproto())
        .addHeader("x-apitoken", apiToken)
        .build(UserServiceGrpc.UserServiceBlockingStub::class.java)

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
