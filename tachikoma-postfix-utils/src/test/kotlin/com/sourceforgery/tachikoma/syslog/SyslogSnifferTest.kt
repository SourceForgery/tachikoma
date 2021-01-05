package com.sourceforgery.tachikoma.syslog

import kotlin.test.assertEquals
import kotlin.test.fail
import org.junit.Test

class SyslogSnifferTest {
    @Test
    fun `parse good line #1`() {
        val notification = parseLine("Jan 18 22:55:46 1c7326acd8e5 postfix/smtp[249]: 2D61E2A03: to=<test@example.com>, relay=none, delay=30, delays=0.01/0/30/0, dsn=4.4.1, status=deferred (connect to example.com[93.184.216.34]:25: Connection timed out)")
            ?: fail("No notification could be parse")
        assertEquals("test@example.com", notification.originalRecipient)
        assertEquals("2D61E2A03", notification.queueId)
        assertEquals("4.4.1", notification.status)
        assertEquals("deferred", notification.reason)
        assertEquals("deferred (connect to example.com[93.184.216.34]:25: Connection timed out)", notification.diagnoseText)
    }
}
