package com.sourceforgery.tachikoma.database.upgrades

import io.ebean.ddlrunner.DdlRunner
import org.intellij.lang.annotations.Language
import java.sql.Connection

class Version10 : DatabaseUpgrade {
    override val newVersion: Int = -10

    override fun run(connection: Connection): Int {
        @Language("PostgreSQL")
        val content =
            """
            ALTER TABLE e_email_status DROP CONSTRAINT ck_e_email_status_email_status;
            ALTER TABLE e_email_status ALTER COLUMN email_status TYPE integer USING email_status::NUMERIC;
            ALTER TABLE e_email_status ADD CONSTRAINT ck_e_email_status_email_status CHECK ( email_status in (0, 1, 2, 3, 4, 5, 6, 7));
            """.trimIndent()

        DdlRunner(false, javaClass.simpleName)
            .runAll(content, connection)
        return -10
    }
}
