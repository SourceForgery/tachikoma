package com.sourceforgery.tachikoma.database.objects

import com.sourceforgery.tachikoma.account.Account
import com.sourceforgery.tachikoma.identifiers.AccountId
import com.sourceforgery.tachikoma.identifiers.MailDomain
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.net.URI

/**
 * This is one account (sender) with it's multiple users
 * Could be called "company" or "organization" responsible for one email domain
 */
@Table(name = "e_account")
@Entity
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
