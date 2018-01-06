package com.sourceforgery.tachikoma.database.objects

import com.sourceforgery.tachikoma.identifiers.AccountId
import io.ebean.common.BeanList
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.OneToMany
import javax.persistence.Table

@Table(name = "e_account")
@Entity
// This is one account (sender) with it's multiple users
// Every user = one account
class AccountDBO : GenericDBO() {
    @OneToMany
    @Column(unique = true)
    val users: List<UserDBO> = BeanList()
}

val AccountDBO.id: AccountId
    get() = AccountId(realId as Long)