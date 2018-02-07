package com.sourceforgery.tachikoma.database.objects

import com.sourceforgery.tachikoma.identifiers.AccountId
import com.sourceforgery.tachikoma.identifiers.MailDomain
import io.ebean.common.BeanList
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.OneToMany
import javax.persistence.Table

@Table(name = "e_account")
@Entity
// This is one account (sender) with it's multiple users
// Every user = one account
class AccountDBO(
        @Column(unique = true)
        val mailDomain: MailDomain
) : GenericDBO() {
    @OneToMany
    val authentications: List<AuthenticationDBO> = BeanList()
    @OneToMany
    val incomingEmailAddresses: List<IncomingEmailAddressDBO> = BeanList()
    @OneToMany
    val incomingEmails: List<IncomingEmailDBO> = BeanList()
}

val AccountDBO.id: AccountId
    get() = AccountId(dbId!!)