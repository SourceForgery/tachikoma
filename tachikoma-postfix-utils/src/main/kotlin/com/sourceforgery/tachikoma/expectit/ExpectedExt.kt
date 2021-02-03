package com.sourceforgery.tachikoma.expectit

import net.sf.expectit.Expect
import net.sf.expectit.ExpectIOException
import net.sf.expectit.Result
import net.sf.expectit.matcher.Matcher
import net.sf.expectit.matcher.Matchers.regexp
import java.util.regex.Pattern

fun Expect.expectNoSmtpError(pattern: String): Result =
    interact().`when`(regexpLine("^([45][0-9][0-9] .*)")).then { r -> throw ExpectIOException("Error", r.input) }
        .until(regexpLine(pattern))

fun Expect.expectNoQuit(pattern: String): Result =
    interact().`when`(regexp(Pattern.compile("^QUIT$", Pattern.CASE_INSENSITIVE)))
        .then { r -> throw ExpectIOException("Error", r.input) }
        .until(regexpLine(pattern))

fun Expect.emptyBuffer(): Result =
    expect(regexp(Pattern.compile(".*", Pattern.DOTALL)))

fun regexpLine(regex: String): Matcher<Result> = regexp(Pattern.compile(regex, Pattern.MULTILINE))