package com.sourceforgery.tachikoma.database.objects

import io.ebean.annotation.CreatedTimestamp
import io.ebean.annotation.UpdatedTimestamp
import io.ebean.bean.EntityBean
import java.time.Instant
import javax.persistence.Column
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.MappedSuperclass
import javax.persistence.SequenceGenerator
import javax.persistence.Transient
import javax.persistence.Version

@MappedSuperclass
abstract class GenericDBO {

    @Id
    @Column(columnDefinition = "DECIMAL(20)")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "unique_id_seq")
    @SequenceGenerator(name = "unique_id_seq", sequenceName = "unique_id_seq", allocationSize = 100)
    lateinit var id: Number

    @field:Version
    var version: Long = 0L
    @field:CreatedTimestamp
    var dateCreated: Instant? = null
    @field:UpdatedTimestamp
    var lastUpdated: Instant? = null

    val new: Boolean
        @Transient
        get() = (this as EntityBean)._ebean_getIntercept().isNew
}