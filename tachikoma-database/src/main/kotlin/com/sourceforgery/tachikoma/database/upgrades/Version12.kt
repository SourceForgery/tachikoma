package com.sourceforgery.tachikoma.database.upgrades

import com.sourceforgery.tachikoma.common.ExtractEmailMetadata
import com.sourceforgery.tachikoma.database.hooks.EbeanHook
import com.sourceforgery.tachikoma.database.objects.query.QIncomingEmailDBO
import io.ebean.Database
import io.ebean.migration.ddl.DdlRunner
import java.sql.Connection
import kotlin.math.absoluteValue
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
            RENAME COLUMN from_email TO mail_from_email;
            ALTER TABLE e_incoming_email
                RENAME COLUMN from_name TO mail_from_name;
            ALTER TABLE e_incoming_email
                RENAME COLUMN receiver_email TO recipient_email;
            ALTER TABLE e_incoming_email
                RENAME COLUMN receiver_name TO recipient_name;
                
            ALTER TABLE e_incoming_email
                ADD COLUMN from_emails jsonb not null default '[]',
                ADD COLUMN reply_to_emails jsonb not null default '[]',
                ADD COLUMN to_emails jsonb not null default '[]';
        """.trimIndent()

        DdlRunner(false, javaClass.simpleName)
            .runAll(content, connection)
        upgraded = true
        return newVersion.absoluteValue
    }

    override fun postStart(database: Database) {
        if (upgraded) {
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
}