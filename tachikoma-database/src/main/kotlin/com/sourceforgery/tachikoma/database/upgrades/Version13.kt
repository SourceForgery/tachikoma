package com.sourceforgery.tachikoma.database.upgrades

import com.sourceforgery.tachikoma.config.TrackingConfig
import io.ebean.ddlrunner.DdlRunner
import org.intellij.lang.annotations.Language
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance
import java.sql.Connection

class Version13(override val di: DI) : DatabaseUpgrade, DIAware {
    override val newVersion: Int = -13

    private val trackingConfig: TrackingConfig by instance()

    override fun run(connection: Connection): Int {
        @Language("PostgreSQL")
        val content =
            """
            alter table e_account add column base_url varchar(255);
            """.trimIndent()

        DdlRunner(false, javaClass.simpleName)
            .runAll(content, connection)
        return newVersion
    }
}
