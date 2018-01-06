package com.sourceforgery.tachikoma.database.objects

import com.sourceforgery.tachikoma.identifiers.EmailStatusId
import io.ebean.annotation.CreatedTimestamp
import io.ebean.bean.EntityBean
import java.time.Instant
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.SequenceGenerator
import javax.persistence.Table
import javax.persistence.Transient

@Table(name = "e_email_status")
@Entity
class EmailStatusDBO(
        @Column
        val emailStatus: EmailStatusDBO,
        @Column
        val email: EmailDBO,
        @Column
        val mtaStatusCode: String
) {
    @Id
    @Column(columnDefinition = "DECIMAL(20)", name = "id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "unique_id_seq")
    @SequenceGenerator(name = "unique_id_seq", sequenceName = "unique_id_seq", allocationSize = 100)
    internal lateinit var realId: Number

    @field:CreatedTimestamp
    var dateCreated: Instant? = null

    @Suppress("CAST_NEVER_SUCCEEDS")
    val new: Boolean
        @Transient
        get() = (this as EntityBean)._ebean_getIntercept().isNew
}

val EmailStatusDBO.id: EmailStatusId
    get() = EmailStatusId(realId as Long)