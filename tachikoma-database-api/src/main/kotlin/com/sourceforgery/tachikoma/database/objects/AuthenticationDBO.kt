package com.sourceforgery.tachikoma.database.objects

import com.sourceforgery.tachikoma.identifiers.AuthenticationId
import io.ebean.annotation.Encrypted
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.ManyToOne
import javax.persistence.Table

@Table(name = "e_user")
@Entity
// One user with
class AuthenticationDBO(
        @Column
        var encryptedPassword: String? = null,

        @Column(unique = true)
        @Encrypted
        var apiToken: String? = null,

        @Column
        val backend: Boolean = false,

        @ManyToOne
        val account: AccountDBO? = null
) : GenericDBO() {

    // 'Fake' constructor for Ebean
    private constructor() : this(backend = true)

    init {
        if (backend && account != null) {
            throw RuntimeException("Should not be both backend and have accountId")
        }
        if (!backend && account == null) {
            throw RuntimeException("Must have at least one authenticator")
        }
    }
}

val AuthenticationDBO.id: AuthenticationId
    get() = AuthenticationId(dbId!!)