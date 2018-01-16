package com.sourceforgery.tachikoma.database.objects

import com.sourceforgery.tachikoma.identifiers.AccountId
import io.ebean.common.BeanList
import javax.persistence.Entity
import javax.persistence.OneToMany
import javax.persistence.Table

@Table(name = "e_account")
@Entity
// This is one account (sender) with it's multiple users
// Every user = one account
class AccountDBO(
        val domain: String
) : GenericDBO() {
    @OneToMany
    val allowedSendingEmails: List<AllowedSendingEmailDBO> = BeanList()
    @OneToMany
    val authentications: List<AuthenticationDBO> = BeanList()
    @OneToMany
    val incomingEmailAddresses: List<IncomingEmailAddressDBO> = BeanList()
    @OneToMany
    val incomingEmails: List<IncomingEmailDBO> = BeanList()
}

val AccountDBO.id: AccountId
    get() = AccountId(dbId!!)