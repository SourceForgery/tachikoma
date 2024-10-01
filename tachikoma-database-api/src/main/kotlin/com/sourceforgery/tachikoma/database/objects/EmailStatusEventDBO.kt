package com.sourceforgery.tachikoma.database.objects

import com.sourceforgery.tachikoma.common.EmailStatus
import com.sourceforgery.tachikoma.identifiers.EmailStatusId
import io.ebean.annotation.DbJson
import io.ebean.annotation.Index
import io.ebean.annotation.WhenCreated
import io.ebean.bean.EntityBean
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import java.time.Instant

@Table(name = "e_email_status")
@Entity
class EmailStatusEventDBO(
    @Column
    @Index
    val emailStatus: EmailStatus,
    @ManyToOne(cascade = [CascadeType.ALL])
    val email: EmailDBO,
    @field:DbJson
    val metaData: StatusEventMetaData,
) {
    @Id
    @Column(columnDefinition = "DECIMAL(20)", name = "id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "unique_status_event_id_seq")
    @SequenceGenerator(name = "unique_status_event_id_seq", sequenceName = "unique_status_event_id_seq", allocationSize = 1)
    internal var dbId: Long? = null

    @WhenCreated
    var dateCreated: Instant? = null

    @Suppress("CAST_NEVER_SUCCEEDS")
    val new: Boolean
        get() = (this as EntityBean)._ebean_getIntercept().isNew
}

val EmailStatusEventDBO.id: EmailStatusId
    get() = EmailStatusId(dbId!!)
