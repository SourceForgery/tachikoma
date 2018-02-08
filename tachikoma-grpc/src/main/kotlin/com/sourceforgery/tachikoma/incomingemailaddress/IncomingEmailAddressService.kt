package com.sourceforgery.tachikoma.incomingemailaddress

import com.sourceforgery.tachikoma.auth.Authentication
import com.sourceforgery.tachikoma.database.dao.AuthenticationDAO
import com.sourceforgery.tachikoma.database.dao.IncomingEmailAddressDAO
import com.sourceforgery.tachikoma.database.objects.IncomingEmailAddressDBO
import com.sourceforgery.tachikoma.grpc.frontend.incomingemailaddress.IncomingEmailAddress
import io.grpc.stub.StreamObserver
import javax.inject.Inject

internal class IncomingEmailAddressService
@Inject
private constructor(
        private val authentication: Authentication,
        private val authenticationDAO: AuthenticationDAO,
        private val incomingEmailAddressDAO: IncomingEmailAddressDAO
) {

    fun addIncomingEmailAddress(request: IncomingEmailAddress) {
        authentication.requireFrontendAdmin()
        val authenticationDBO = authenticationDAO.getActiveById(authentication.authenticationId)!!

        val incomingEmailAddressDBO = IncomingEmailAddressDBO(
                localPart = request.localPart,
                account = authenticationDBO.account
        )

        incomingEmailAddressDAO.save(incomingEmailAddressDBO)
    }

    fun getIncomingEmailAddresses(responseObserver: StreamObserver<IncomingEmailAddress>) {
        authentication.requireFrontendAdmin()
        val authenticationDBO = authenticationDAO.getActiveById(authentication.authenticationId)!!

        incomingEmailAddressDAO.getAll(accountDBO = authenticationDBO.account)
                .forEach {
                    val incomingEmailAddress = IncomingEmailAddress
                            .newBuilder()
                            .setLocalPart(it.localPart)
                            .build()
                    responseObserver.onNext(incomingEmailAddress)
                }
    }

    fun deleteIncomingEmailAddress(request: IncomingEmailAddress) {
        authentication.requireFrontendAdmin()
        val authenticationDBO = authenticationDAO.getActiveById(authentication.authenticationId)!!

        incomingEmailAddressDAO.delete(
                accountDBO = authenticationDBO.account,
                localPart = request.localPart
        )
    }
}