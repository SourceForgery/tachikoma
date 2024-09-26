package com.sourceforgery.tachikoma.database.upgrades

import io.ebean.ddlrunner.DdlRunner
import org.intellij.lang.annotations.Language
import org.kodein.di.DI
import org.kodein.di.DIAware
import java.sql.Connection

class Version14(override val di: DI) : DatabaseUpgrade, DIAware {
    override val newVersion: Int = -14

    override fun run(connection: Connection): Int {
        @Language("PostgreSQL")
        val content =
            """
            create index idx_e_email_send_transaction_bcc_gin on e_email_send_transaction using gin(bcc);
            create index idx_e_email_recipient on e_email(recipient);
            """.trimIndent()

        DdlRunner(false, javaClass.simpleName)
            .runAll(content, connection)
        return newVersion
    }
}
