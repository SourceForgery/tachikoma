package com.sourceforgery.tachikoma.database.objects

import io.ebean.annotation.WhenCreated
import io.ebean.annotation.WhenModified
import io.ebean.bean.EntityBean
import java.time.Instant
import javax.persistence.Column
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.MappedSuperclass
import javax.persistence.SequenceGenerator
import javax.persistence.Version

@MappedSuperclass
abstract class GenericDBO {

    @Id
    @Column(columnDefinition = "DECIMAL(20)", name = "id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "unique_id_seq")
    @SequenceGenerator(name = "unique_id_seq", sequenceName = "unique_id_seq", allocationSize = 1)
    internal var dbId: Long? = null

    @field:Version
    var version: Long = 0L
    @WhenCreated
    var dateCreated: Instant? = null
    @WhenModified
    var lastUpdated: Instant? = null

    val new: Boolean
        get() = (this as EntityBean)._ebean_getIntercept().isNew
}
