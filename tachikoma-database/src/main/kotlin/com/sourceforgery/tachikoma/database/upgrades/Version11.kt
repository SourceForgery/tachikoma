package com.sourceforgery.tachikoma.database.upgrades

import io.ebean.ddlrunner.DdlRunner
import org.intellij.lang.annotations.Language
import java.sql.Connection

class Version11 : DatabaseUpgrade {
    override val newVersion: Int = -11

    override fun run(connection: Connection): Int {
        @Language("PostgreSQL")
        val content =
            """
            ALTER TABLE e_email ADD COLUMN auto_mail_id VARCHAR(255);
            UPDATE e_email SET auto_mail_id = message_id WHERE TRUE;
            ALTER TABLE e_email ALTER COLUMN auto_mail_id SET NOT NULL;
            CREATE UNIQUE INDEX uq_e_email_auto_mail_id ON e_email (auto_mail_id);
            
            CREATE INDEX ix_e_email_status_email_status ON e_email_status (email_status);
            """.trimIndent()

        DdlRunner(false, javaClass.simpleName)
            .runAll(content, connection)
        return newVersion
    }
}
