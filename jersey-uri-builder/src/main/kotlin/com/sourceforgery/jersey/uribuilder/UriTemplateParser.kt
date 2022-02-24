/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2017 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

/**
 * Sourceforgery elects to include this software in this distribution under the CDDL license.
 *
 * Modifications:
 * Repackaged original source
 * Removed dependency on javax.ws.rs interfaces
 * Converted to Kotlin
 */

package com.sourceforgery.jersey.uribuilder

import java.util.ArrayList
import java.util.HashMap
import java.util.HashSet
import java.util.NoSuchElementException
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

/**
 * A URI template parser that parses JAX-RS specific URI templates.
 *
 * @author Paul Sandoz
 * @author Gerard Davison (gerard.davison at oracle.com)
 */
@Suppress("unused")
class UriTemplateParser
/**
 * Parse a template.
 *
 * @param template the template.
 * @throws IllegalArgumentException if the template is null, an empty string
 * or does not conform to a JAX-RS URI template.
 */
@Throws(IllegalArgumentException::class)
constructor(
    /**
     * Get the template.
     *
     * @return the template.
     */
    val template: String?
) {
    private val regex = StringBuffer()
    private val normalizedTemplate = StringBuffer()
    private val literalCharactersBuffer = StringBuffer()
    /**
     * Get the pattern.
     *
     * @return the pattern.
     */
    val pattern: Pattern
    private val names = ArrayList<String>()
    private val groupCounts = ArrayList<Int>()
    private val nameToPattern = HashMap<String, Pattern>()
    /**
     * Get the number of explicit regular expressions.
     *
     * @return the number of explicit regular expressions.
     */
    var numberOfExplicitRegexes: Int = 0
        private set
    private var skipGroup: Int = 0

    /**
     * Get the number of literal characters.
     *
     * @return the number of literal characters.
     */
    var numberOfLiteralCharacters: Int = 0
        private set

    /**
     * Get the group indexes to capturing groups.
     *
     *
     * Any nested capturing groups will be ignored and the
     * the group index will refer to the top-level capturing
     * groups associated with the templates variables.
     *
     * @return the group indexes to capturing groups.
     */
    val groupIndexes: IntArray
        get() {
            if (names.isEmpty()) {
                return EMPTY_INT_ARRAY
            }

            val indexes = IntArray(names.size)
            indexes[0] = 0 + groupCounts[0]
            for (i in 1 until indexes.size) {
                indexes[i] = indexes[i - 1] + groupCounts[i]
            }

            return indexes
        }

    /**
     * Get the number of regular expression groups
     *
     * @return the number of regular expressions groups
     * @since 2.9
     */
    val numberOfRegexGroups: Int
        get() {
            if (groupCounts.isEmpty()) {
                return 0
            } else {
                val groupIndex = groupIndexes
                return groupIndex[groupIndex.size - 1] + skipGroup
            }
        }

    init {
        if (template == null || template.isEmpty()) {
            throw IllegalArgumentException("Template is null or has zero length")
        }
        parse(CharacterIterator(template))
        try {
            pattern = Pattern.compile(regex.toString())
        } catch (ex: PatternSyntaxException) {
            throw IllegalArgumentException(
                "Invalid syntax for the template expression '" +
                    regex + "'",
                ex
            )
        }
    }

    /**
     * Get the normalized template.
     *
     *
     * A normalized template is a template without any explicit regular
     * expressions.
     *
     * @return the normalized template.
     */
    fun getNormalizedTemplate(): String {
        return normalizedTemplate.toString()
    }

    /**
     * Get the map of template names to patterns.
     *
     * @return the map of template names to patterns.
     */
    fun getNameToPattern(): Map<String, Pattern> {
        return nameToPattern
    }

    /**
     * Get the list of template names.
     *
     * @return the list of template names.
     */
    fun getNames(): List<String> {
        return names
    }

    /**
     * Get the capturing group counts for each template variable.
     *
     * @return the capturing group counts.
     */
    fun getGroupCounts(): List<Int> {
        return groupCounts
    }

    /**
     * Encode literal characters of a template.
     *
     * @param characters the literal characters
     * @return the encoded literal characters.
     */
    protected fun encodeLiteralCharacters(characters: String): String {
        return characters
    }

    private fun parse(ci: CharacterIterator) {
        try {
            while (ci.hasNext()) {
                val c = ci.next()
                if (c == '{') {
                    processLiteralCharacters()
                    skipGroup = parseName(ci, skipGroup)
                } else {
                    literalCharactersBuffer.append(c)
                }
            }
            processLiteralCharacters()
        } catch (ex: NoSuchElementException) {
            throw IllegalArgumentException("Invalid syntax in the template \"$template\". Check if a path parameter is terminated with a \"}\".", ex)
        }
    }

    private fun processLiteralCharacters() {
        if (literalCharactersBuffer.length > 0) {
            numberOfLiteralCharacters += literalCharactersBuffer.length

            val s = encodeLiteralCharacters(literalCharactersBuffer.toString())

            normalizedTemplate.append(s)

            // Escape if reserved regex character
            var i = 0
            while (i < s.length) {
                val c = s[i]
                if (RESERVED_REGEX_CHARACTERS.contains(c)) {
                    regex.append("\\")
                    regex.append(c)
                } else if (c == '%') {
                    val c1 = s[i + 1]
                    val c2 = s[i + 2]
                    if (UriComponent.isHexCharacter(c1) && UriComponent.isHexCharacter(c2)) {
                        regex.append("%").append(HEX_TO_UPPERCASE_REGEX[c1.code]).append(HEX_TO_UPPERCASE_REGEX[c2.code])
                        i += 2
                    }
                } else {
                    regex.append(c)
                }
                i++
            }
            literalCharactersBuffer.setLength(0)
        }
    }

    private fun parseName(ci: CharacterIterator, skipGroup: Int): Int {
        var c = consumeWhiteSpace(ci)

        var paramType = 'p' // Normal path param unless otherwise stated
        val nameBuffer = StringBuilder()

        // Look for query or matrix types
        if (c == '?' || c == ';') {
            paramType = c
            c = ci.next()
        }

        if (Character.isLetterOrDigit(c) || c == '_') {
            // Template name character
            nameBuffer.append(c)
        } else {
            throw IllegalArgumentException("Illegal character \"" + c + "\" at position " + ci.pos() + " is not allowed as a start of a name in a path template \"" + template + "\".")
        }

        var nameRegexString = ""
        while (true) {
            c = ci.next()
            // "\\{(\\w[-\\w\\.]*)
            if (Character.isLetterOrDigit(c) || c == '_' || c == '-' || c == '.') {
                // Template name character
                nameBuffer.append(c)
            } else if (c == ',' && paramType != 'p') {
                // separator allowed for non-path parameter names
                nameBuffer.append(c)
            } else if (c == ':' && paramType == 'p') {
                nameRegexString = parseRegex(ci)
                break
            } else if (c == '}') {
                break
            } else if (c == ' ') {
                c = consumeWhiteSpace(ci)

                if (c == ':') {
                    nameRegexString = parseRegex(ci)
                    break
                } else if (c == '}') {
                    break
                } else {
                    // Error
                    throw IllegalArgumentException("Illegal character \"" + c + "\" at position " + ci.pos() + " is not allowed after a name in a path template \"" + template + "\".")
                }
            } else {
                throw IllegalArgumentException("Illegal character \"" + c + "\" at position " + ci.pos() + " is not allowed as a part of a name in a path template \"" + template + "\".")
            }
        }

        var name = nameBuffer.toString()
        val namePattern: Pattern
        val nextSkipGroup: Int
        try {
            if (paramType == '?' || paramType == ';') {
                val subNames = name.split(",\\s?".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

                // Build up the regex for each of these properties
                val regexBuilder = StringBuilder(if (paramType == '?') "\\?" else ";")
                val separator = if (paramType == '?') "\\&" else ";/\\?"

                // Start a group because each parameter could repeat
                //                names.add("__" + (paramType == '?' ? "query" : "matrix"));

                var first = true

                regexBuilder.append("(")
                for (subName in subNames) {
                    regexBuilder.append("(&?")
                    regexBuilder.append(subName)
                    regexBuilder.append("(=([^")
                    regexBuilder.append(separator)
                    regexBuilder.append("]*))?")
                    regexBuilder.append(")")
                    if (!first) {
                        regexBuilder.append("|")
                    }

                    names.add(subName)
                    groupCounts.add(
                        if (first) 5 else 3
                    )
                    first = false
                }

                //                groupCounts.add(1);
                nextSkipGroup = 1

                // Knock of last bar
                regexBuilder.append(")*")

                namePattern = Pattern.compile(regexBuilder.toString())

                // Make sure we display something useful
                name = paramType + name
            } else {
                names.add(name)
                //               groupCounts.add(1 + skipGroup);

                if (!nameRegexString.isEmpty()) {
                    numberOfExplicitRegexes++
                }
                namePattern = if (nameRegexString.isEmpty())
                    TEMPLATE_VALUE_PATTERN
                else
                    Pattern.compile(nameRegexString)
                if (nameToPattern.containsKey(name)) {
                    if (nameToPattern[name] != namePattern) {
                        throw IllegalArgumentException("The name \"$name\" is declared more than once with different regular expressions in a path template \"$template\".")
                    }
                } else {
                    nameToPattern[name] = namePattern
                }

                // Determine group count of pattern
                val m = namePattern.matcher("")
                val g = m.groupCount()
                groupCounts.add(1 + skipGroup)
                nextSkipGroup = g
            }

            regex.append('(')
                .append(namePattern)
                .append(')')

            normalizedTemplate.append('{')
                .append(name)
                .append('}')
        } catch (ex: PatternSyntaxException) {
            throw IllegalArgumentException("Invalid syntax for the expression \"$nameRegexString\" associated with the name \"$name\" in a path template \"$template\".")
        }

        // Tell the next time through the loop how many to skip
        return nextSkipGroup
    }

    private fun parseRegex(ci: CharacterIterator): String {
        val regexBuffer = StringBuilder()

        var braceCount = 1
        while (true) {
            val c = ci.next()
            if (c == '{') {
                braceCount++
            } else if (c == '}') {
                braceCount--
                if (braceCount == 0) {
                    break
                }
            }
            regexBuffer.append(c)
        }

        return regexBuffer.toString().trim { it <= ' ' }
    }

    private fun consumeWhiteSpace(ci: CharacterIterator): Char {
        var c: Char
        do {
            c = ci.next()
        } while (Character.isWhitespace(c))

        return c
    }

    companion object {

        internal val EMPTY_INT_ARRAY = IntArray(0)
        private val RESERVED_REGEX_CHARACTERS by lazy {
            val reserved = charArrayOf('.', '^', '&', '!', '?', '-', ':', '<', '(', '[', '$', '=', ')', ']', ',', '>', '*', '+', '|')

            val s = HashSet<Char>(reserved.size)
            for (c in reserved) {
                s.add(c)
            }
            s
        }

        private val HEX_TO_UPPERCASE_REGEX by lazy {
            val table = arrayOfNulls<String>(0x80)
            for (i in table.indices) {
                table[i] = i.toChar().toString()
            }

            run {
                var c = 'a'
                while (c <= 'f') {
                    // initialize table values: table[a] = ([aA]) ...
                    table[c.code] = "[" + c + (c - 'a' + 'A'.code).toChar() + "]"
                    c++
                }
            }

            var c = 'A'
            while (c <= 'F') {
                // initialize table values: table[A] = ([aA]) ...
                table[c.code] = "[" + (c - 'A' + 'a'.code).toChar() + c + "]"
                c++
            }
            table
        }

        /**
         * Default URI template value regexp pattern.
         */
        val TEMPLATE_VALUE_PATTERN = Pattern.compile("[^/]+")
    }
}
