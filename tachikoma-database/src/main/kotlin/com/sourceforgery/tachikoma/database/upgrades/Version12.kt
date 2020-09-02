package com.sourceforgery.tachikoma.database.upgrades

import com.sourceforgery.tachikoma.common.ExtractEmailMetadata
import com.sourceforgery.tachikoma.database.hooks.EbeanHook
import com.sourceforgery.tachikoma.database.objects.query.QIncomingEmailDBO
import io.ebean.Database
import io.ebean.migration.ddl.DdlRunner
import java.sql.Connection
import org.apache.logging.log4j.kotlin.logger
import org.intellij.lang.annotations.Language
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance

class Version12(override val di: DI) : DatabaseUpgrade, EbeanHook, DIAware {
    override val newVersion: Int = -12
    private var upgraded = false

    private val extractEmailMetadata: ExtractEmailMetadata by instance()

    override fun run(connection: Connection): Int {
        @Language("PostgreSQL")
        val content = """
            ALTER TABLE e_incoming_email
            RENAME COLUMN from_email TO mail_from;
            ALTER TABLE e_incoming_email
                DROP COLUMN from_name;
            ALTER TABLE e_incoming_email
                RENAME COLUMN receiver_email TO recipient;
            ALTER TABLE e_incoming_email
                DROP COLUMN receiver_name;
                
            ALTER TABLE e_incoming_email
                ADD COLUMN from_emails jsonb not null default '[]',
                ADD COLUMN reply_to_emails jsonb not null default '[]',
                ADD COLUMN to_emails jsonb not null default '[]';
        """.trimIndent()

        DdlRunner(false, javaClass.simpleName)
            .runAll(content, connection)
        upgraded = true
        return newVersion
    }

    override fun postStart(database: Database) {
        if (upgraded) {
            LOGGER.warn { "Running upgrade of incoming emails" }
            QIncomingEmailDBO(database)
                .findEach {
                    val emails = extractEmailMetadata.extract(it.body)
                    it.replyToEmails = emails.replyTo
                    it.fromEmails = emails.from
                    it.toEmails = emails.to
                    database.save(it)
                }
        }
    }

    companion object {
        private val LOGGER = logger()
    }
}
