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

import java.util.regex.MatchResult
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

/**
 * A pattern for matching a string against a regular expression
 * and returning capturing group values for any capturing groups present in
 * the expression.
 *
 * @author Paul Sandoz
 * @author Gerard Davison (gerard.davison at oracle.com)
 */
class PatternWithGroups {
    /**
     * The regular expression for matching and obtaining capturing group values.
     */
    val regex: String
    /**
     * The compiled regular expression of [.regex].
     */
    private val regexPattern: Pattern?
    /**
     * The array of group indexes to capturing groups.
     */
    private val groupIndexes: IntArray

    /**
     * Construct an empty pattern.
     */
    protected constructor() {
        this.regex = ""
        this.regexPattern = null
        this.groupIndexes = EMPTY_INT_ARRAY
    }

    /**
     * Construct a new pattern.
     *
     * @param regex the regular expression. If the expression is `null` or an empty string then the pattern will
     * only
     * match
     * a `null` or empty string.
     * @param groupIndexes the array of group indexes to capturing groups.
     * @throws java.util.regex.PatternSyntaxException if the regular expression could not be compiled.
     */
    @Throws(PatternSyntaxException::class)
    constructor(regex: String, groupIndexes: IntArray = EMPTY_INT_ARRAY) : this(compile(regex), groupIndexes)

    /**
     * Construct a new pattern.
     *
     * @param regexPattern the regular expression pattern.
     * @param groupIndexes the array of group indexes to capturing groups.
     * @throws IllegalArgumentException if the regexPattern is `null`.
     */
    @Throws(IllegalArgumentException::class)
    @JvmOverloads
    constructor(regexPattern: Pattern?, groupIndexes: IntArray = EMPTY_INT_ARRAY) {
        if (regexPattern == null) {
            throw IllegalArgumentException()
        }

        this.regex = regexPattern.toString()
        this.regexPattern = regexPattern
        this.groupIndexes = groupIndexes.clone()
    }

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
    fun getGroupIndexes(): IntArray {
        return groupIndexes.clone()
    }

    private class EmptyStringMatchResult : MatchResult {

        override fun start(): Int {
            return 0
        }

        override fun start(group: Int): Int {
            if (group != 0) {
                throw IndexOutOfBoundsException()
            }
            return start()
        }

        override fun end(): Int {
            return 0
        }

        override fun end(group: Int): Int {
            if (group != 0) {
                throw IndexOutOfBoundsException()
            }
            return end()
        }

        override fun group(): String {
            return ""
        }

        override fun group(group: Int): String {
            if (group != 0) {
                throw IndexOutOfBoundsException()
            }
            return group()
        }

        override fun groupCount(): Int {
            return 0
        }
    }

    private inner class GroupIndexMatchResult internal constructor(private val result: MatchResult) : MatchResult {

        override fun start(): Int {
            return result.start()
        }

        override fun start(group: Int): Int {
            if (group > groupCount()) {
                throw IndexOutOfBoundsException()
            }

            return if (group > 0) result.start(groupIndexes[group - 1]) else result.start()
        }

        override fun end(): Int {
            return result.end()
        }

        override fun end(group: Int): Int {
            if (group > groupCount()) {
                throw IndexOutOfBoundsException()
            }

            return if (group > 0) result.end(groupIndexes[group - 1]) else result.end()
        }

        override fun group(): String {
            return result.group()
        }

        override fun group(group: Int): String {
            if (group > groupCount()) {
                throw IndexOutOfBoundsException()
            }

            return if (group > 0) result.group(groupIndexes[group - 1]) else result.group()
        }

        override fun groupCount(): Int {
            return groupIndexes.size
        }
    }

    /**
     * Match against the pattern.
     *
     * @param cs the char sequence to match against the template.
     * @return the match result, otherwise null if no match occurs.
     */
    fun match(cs: CharSequence?): MatchResult? {
        // Check for match against the empty pattern
        if (cs == null) {
            return if (regexPattern == null) EMPTY_STRING_MATCH_RESULT else null
        } else if (regexPattern == null) {
            return null
        }

        // Match regular expression
        val m = regexPattern.matcher(cs)
        if (!m.matches()) {
            return null
        }

        if (cs.length == 0) {
            return EMPTY_STRING_MATCH_RESULT
        }

        return if (groupIndexes.size > 0) GroupIndexMatchResult(m) else m
    }

    /**
     * Match against the pattern.
     *
     *
     * If a matched then the capturing group values (if any) will be added to a list passed in as parameter.
     *
     * @param cs the char sequence to match against the template.
     * @param groupValues the list to add the values of a pattern's capturing groups if matching is successful. The values are
     * added in the same order as the pattern's capturing groups. The list is cleared before values are added.
     * @return `true` if the char sequence matches the pattern, otherwise `false`.
     *
     * @throws IllegalArgumentException if the group values is `null`.
     */
    @Throws(IllegalArgumentException::class)
    fun match(cs: CharSequence?, groupValues: MutableList<String>?): Boolean {
        if (groupValues == null) {
            throw IllegalArgumentException()
        }

        // Check for match against the empty pattern
        if (cs == null || cs.length == 0) {
            return regexPattern == null
        } else if (regexPattern == null) {
            return false
        }

        // Match the regular expression
        val m = regexPattern.matcher(cs)
        if (!m.matches()) {
            return false
        }

        groupValues.clear()
        if (groupIndexes.size > 0) {
            for (i in groupIndexes.indices) {
                groupValues.add(m.group(groupIndexes[i]))
            }
        } else {
            for (i in 1..m.groupCount()) {
                groupValues.add(m.group(i))
            }
        }

        // TODO check for consistency of different capturing groups
        // that must have the same value

        return true
    }

    /**
     * Match against the pattern.
     *
     *
     * If a matched then the capturing group values (if any) will be added to a list passed in as parameter.
     *
     * @param cs the char sequence to match against the template.
     * @param groupNames the list names associated with a pattern's capturing groups. The names MUST be in the same order as the
     * pattern's capturing groups and the size MUST be equal to or less than the number of capturing groups.
     * @param groupValues the map to add the values of a pattern's capturing groups if matching is successful. A values is put
     * into the map using the group name associated with the capturing group. The map is cleared before values
     * are added.
     * @return `true` if the matches the pattern, otherwise `false`.
     *
     * @throws IllegalArgumentException if group values is `null`.
     */
    @Throws(IllegalArgumentException::class)
    fun match(cs: CharSequence?, groupNames: List<String>, groupValues: MutableMap<String, String>?): Boolean {
        if (groupValues == null) {
            throw IllegalArgumentException()
        }

        // Check for match against the empty pattern
        if (cs == null || cs.length == 0) {
            return regexPattern == null
        } else if (regexPattern == null) {
            return false
        }

        // Match the regular expression
        val m = regexPattern.matcher(cs)
        if (!m.matches()) {
            return false
        }

        // Assign the matched group values to group names
        groupValues.clear()

        for (i in groupNames.indices) {
            val name = groupNames[i]
            val currentValue = m.group(if (groupIndexes.size > 0) groupIndexes[i] else i + 1)

            // Group names can have the same name occurring more than once,
            // check that groups values are same.
            val previousValue = groupValues[name]
            if (previousValue != null && previousValue != currentValue) {
                return false
            }

            groupValues[name] = currentValue
        }

        return true
    }

    override fun hashCode(): Int {
        return regex.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) {
            return false
        }
        if (javaClass != other.javaClass) {
            return false
        }
        val that = other as PatternWithGroups?
        return !(this.regex !== that!!.regex && (this.regex != that!!.regex))
    }

    override fun toString(): String {
        return regex
    }

    companion object {

        private val EMPTY_INT_ARRAY = IntArray(0)
        /**
         * The empty pattern that matches the null or empty string.
         */
        val EMPTY = PatternWithGroups()

        @Throws(PatternSyntaxException::class)
        private fun compile(regex: String?): Pattern? {
            return if (regex == null || regex.isEmpty()) null else Pattern.compile(regex)
        }

        private val EMPTY_STRING_MATCH_RESULT = EmptyStringMatchResult()
    }
}
