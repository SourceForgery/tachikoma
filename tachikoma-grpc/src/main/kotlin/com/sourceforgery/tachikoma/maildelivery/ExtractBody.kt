package com.sourceforgery.tachikoma.maildelivery

import com.sourceforgery.tachikoma.common.Email
import org.jsoup.Jsoup
import java.util.Locale
import java.util.Scanner
import java.util.regex.Pattern

val ALL_NEWLINES = Pattern.compile("\r*\n\r*")

val EMAIL_DELIMITER = "------"
private val EMAIL_FROM = "From:"
private val MULTIPLE_EMPTY_LINES = listOf("", "")

/**
 * @param originalText The text
 * @param toEmail the email that was sent to (and which will most likely be in the line looking like `foo@example.com wrote:`)
 */
fun extractBodyFromPlaintextEmail(originalText: String, toEmail: Email): String {
    val scanner = Scanner(originalText)
    scanner.useDelimiter(ALL_NEWLINES)
    scanner.useLocale(Locale.ENGLISH)

    @OptIn(ExperimentalStdlibApi::class)
    return buildList<String> {
        for (temp in scanner) {
            if (temp.startsWith(">") || EMAIL_FROM in temp) {
                break
            }
            if (EMAIL_DELIMITER in temp || temp.contains(toEmail.address, true)) {
                removeLastOrNull()
                break
            }
            if (temp == "" && takeLast(2) == MULTIPLE_EMPTY_LINES) {
                continue
            }
            add(temp)
        }
    }
        .joinToString("\n")
        .trim()
}

/**
 * @param originalHtml The text
 * @param toEmail the email that was sent to (and which will most likely be in the line looking like `foo@example.com wrote:`)
 */
fun extractBodyFromHtmlEmail(originalHtml: String, email: Email): String =
    extractBodyFromPlaintextEmail(
        getPlainText(Jsoup.parse(originalHtml)),
        email
    )