package com.sourceforgery.tachikoma.database.upgrades

import io.ebean.ddlrunner.DdlRunner
import org.intellij.lang.annotations.Language
import org.kodein.di.DI
import org.kodein.di.DIAware
import java.sql.Connection

class Version15(override val di: DI) : DatabaseUpgrade, DIAware {
    override val newVersion: Int = -15

    override fun run(connection: Connection): Int {
        @Language("PostgreSQL")
        val content =
            """
                create index idx_e_email_mta_queue_id on e_email(mta_queue_id);
            """.trimIndent()

        DdlRunner(false, javaClass.simpleName)
            .runAll(content, connection)
        return newVersion
    }
}
