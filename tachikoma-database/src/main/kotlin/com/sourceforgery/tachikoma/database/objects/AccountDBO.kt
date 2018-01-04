package com.sourceforgery.tachikoma.database.objects

import com.sourceforgery.tachikoma.identifiers.AccountId
import io.ebean.common.BeanList
import javax.persistence.Entity

@Entity
class AccountDBO(
        val users: List<UserDBO> = BeanList()
) : GenericDBO()

val AccountDBO.id: AccountId
    get() = AccountId(realId as Long)