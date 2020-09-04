package com.sourceforgery.tachikoma.incomingemailaddress

import com.sourceforgery.tachikoma.database.dao.AuthenticationDAO
import com.sourceforgery.tachikoma.database.dao.IncomingEmailAddressDAO
import com.sourceforgery.tachikoma.database.objects.IncomingEmailAddressDBO
import com.sourceforgery.tachikoma.grpc.frontend.incomingemailaddress.IncomingEmailAddress
import com.sourceforgery.tachikoma.identifiers.AuthenticationId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance

internal class IncomingEmailAddressService(override val di: DI) : DIAware {
    private val authenticationDAO: AuthenticationDAO by instance()
    private val incomingEmailAddressDAO: IncomingEmailAddressDAO by instance()

    fun addIncomingEmailAddress(request: IncomingEmailAddress, authenticationId: AuthenticationId) {
        val authenticationDBO = authenticationDAO.getActiveById(authenticationId)!!

        val incomingEmailAddressDBO = IncomingEmailAddressDBO(
            localPart = request.localPart,
            account = authenticationDBO.account
        )

        incomingEmailAddressDAO.save(incomingEmailAddressDBO)
    }

    fun getIncomingEmailAddresses(authenticationId: AuthenticationId): Flow<IncomingEmailAddress> {
        val authenticationDBO = authenticationDAO.getActiveById(authenticationId)!!

        return incomingEmailAddressDAO.getAll(accountDBO = authenticationDBO.account)
            .asFlow()
            .map {
                IncomingEmailAddress
                    .newBuilder()
                    .setLocalPart(it.localPart)
                    .build()
            }
    }

    fun deleteIncomingEmailAddress(request: IncomingEmailAddress, authenticationId: AuthenticationId) {
        val authenticationDBO = authenticationDAO.getActiveById(authenticationId)!!

        incomingEmailAddressDAO.delete(
            accountDBO = authenticationDBO.account,
            localPart = request.localPart
        )
    }
}
