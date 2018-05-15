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
    // This is the domain allowed in the from field
    @Column(unique = true)
    val mailDomain: MailDomain
) : GenericDBO() {
    @OneToMany(mappedBy = "account")
    val authentications: List<AuthenticationDBO> = BeanList()
    @OneToMany(mappedBy = "account")
    val incomingEmailAddresses: List<IncomingEmailAddressDBO> = BeanList()
    @OneToMany(mappedBy = "account")
    val incomingEmails: List<IncomingEmailDBO> = BeanList()
}

val AccountDBO.id: AccountId
    get() = AccountId(dbId!!)