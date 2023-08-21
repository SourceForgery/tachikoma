package com.sourceforgery.tachikoma.database.objects

import com.sourceforgery.tachikoma.account.Account
import com.sourceforgery.tachikoma.identifiers.AccountId
import com.sourceforgery.tachikoma.identifiers.MailDomain
import java.net.URI
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.OneToMany
import javax.persistence.Table

@Table(name = "e_account")
@Entity
/**
 * This is one account (sender) with it's multiple users
 * Could be called "company" or "organization" responsible for one email domain
 */
class AccountDBO(
    // This is the domain allowed in the from field
    @Column(unique = true)
    override val mailDomain: MailDomain,
    override var baseUrl: URI? = null,
) : GenericDBO(), Account {

    override val id: AccountId
        get() = AccountId(this::dbId.get()!!)

    @OneToMany(orphanRemoval = true)
    val authentications: List<AuthenticationDBO> = ArrayList()

    @OneToMany(orphanRemoval = true)
    val incomingEmailAddresses: List<IncomingEmailAddressDBO> = ArrayList()

    @OneToMany(orphanRemoval = true)
    val incomingEmails: List<IncomingEmailDBO> = ArrayList()

    @OneToMany(orphanRemoval = true)
    val blockedEmails: List<BlockedEmailDBO> = ArrayList()
}
