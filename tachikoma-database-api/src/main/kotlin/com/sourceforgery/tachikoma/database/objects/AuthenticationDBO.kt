package com.sourceforgery.tachikoma.database.objects

import com.sourceforgery.tachikoma.common.AuthenticationRole
import com.sourceforgery.tachikoma.common.Email
import com.sourceforgery.tachikoma.identifiers.AuthenticationId
import io.ebean.annotation.Encrypted
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

// One user with credentials. Never delete, but do deactivate
@Table(name = "e_user")
@Entity
class AuthenticationDBO(
    @Column
    var encryptedPassword: String? = null,
    @Column(unique = true, name = "username")
    val login: String? = null,
    @Column(unique = true)
    @Encrypted
    var apiToken: String? = null,
    @Column
    var role: AuthenticationRole,
    @ManyToOne(cascade = [CascadeType.ALL])
    val account: AccountDBO,
    @Column
    var recipientOverride: Email? = null,
) : GenericDBO() {
    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true)
    val emailSendTransactionDBO: List<EmailSendTransactionDBO> = ArrayList()

    @Column
    var active = true

    override fun toString(): String {
        return "AuthenticationDBO(id=$id, role=$role, login=$login)"
    }
}

val AuthenticationDBO.id: AuthenticationId
    get() = AuthenticationId(dbId!!)
