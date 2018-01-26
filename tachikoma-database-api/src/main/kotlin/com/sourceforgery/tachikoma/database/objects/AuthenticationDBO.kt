package com.sourceforgery.tachikoma.database.objects

import com.sourceforgery.tachikoma.common.AuthenticationRole
import com.sourceforgery.tachikoma.common.Email
import com.sourceforgery.tachikoma.identifiers.AuthenticationId
import io.ebean.annotation.Encrypted
import io.ebean.common.BeanList
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.ManyToOne
import javax.persistence.Table

@Table(name = "e_user")
@Entity
// One user with credentials. Never delete, but do deactivate
class AuthenticationDBO(
        @Column
        var encryptedPassword: String? = null,

        @Column(unique = true)
        @Encrypted
        var apiToken: String? = null,

        @Column
        val role: AuthenticationRole,

        @ManyToOne
        val account: AccountDBO,

        @Column
        var recipientOverride: Email? = null
) : GenericDBO() {
    val incomingEmailAddresses: List<IncomingEmailAddressDBO> = BeanList()

    @Column
    var active = true
}

val AuthenticationDBO.id: AuthenticationId
    get() = AuthenticationId(dbId!!)