/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2017 Oracle and/or its affiliates. All rights reserved.
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

/**
 * Parser for string URI with template parameters which produces [URIs][java.net.URI] from Strings.
 * Example of parsed uri: `"http://user@{host}:{port}/a/{path}?query=1#fragment"`.
 * The parser is not thread safe.
 *
 * @author Paul Sandoz
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
@Suppress("unused")
class UriParser
/**
     * Creates new parser initialized with `uri`.
     *
     * @param input String with URI to be parsed. May contain template parameters.
     */
    internal constructor(private val input: String) {
        private var ci: CharacterIterator? = null
            get() {
                assertParsed()
                return field
            }
            private set
        var scheme: String? = null
            get() {
                assertParsed()
                return field
            }
            private set
        var userInfo: String? = null
            get() {
                assertParsed()
                return field
            }
            private set
        var host: String? = null
            get() {
                assertParsed()
                return field
            }
            private set
        var port: String? = null
            get() {
                assertParsed()
                return field
            }
            private set
        var query: String? = null
            get() {
                assertParsed()
                return field
            }
            private set
        var path: String? = null
            get() {
                assertParsed()
                return field
            }
            private set
        var fragment: String? = null
            get() {
                assertParsed()
                return field
            }
            private set
        var ssp: String? = null
            get() {
                assertParsed()
                return field
            }
            private set
        var authority: String? = null
            get() {
                assertParsed()
                return field
            }
            private set
        var opaque: Boolean = false
            get() {
                assertParsed()
                return field
            }
            private set
        private var parserExecuted: Boolean = false

        /**
         * Returns whether the input string URI is opaque. The [method][parse] must be called before executing this method.
         *
         * @return True if the uri is opaque.
         */
        val isOpaque: Boolean
            get() {
                assertParsed()
                return opaque
            }

        private fun assertParsed() {
            if (!parserExecuted) {
                throw IllegalStateException("Uri is not parsed yet")
            }
        }

        private fun parseComponentWithIP(
            delimiters: String,
            mayEnd: Boolean,
        ): String? {
            return parseComponent(delimiters, mayEnd, true)
        }

        /**
         * Parses the URI component. Parsing starts at position of the first character of
         * component and ends with position of one of the delimiters. The string and current
         * position is taken from the [CharacterIterator].
         *
         * @param delimiters String with delimiters which terminates the component.
         * @param mayEnd True if component might be the last part of the URI.
         * @param isIp True if the component might contain IPv6 address.
         * @return Extracted component.
         */
        private fun parseComponent(
            delimiters: String?,
            mayEnd: Boolean,
            isIp: Boolean = false,
        ): String? {
            var curlyBracketsCount = 0
            var squareBracketsCount = 0

            val sb = StringBuilder()

            var endOfInput = false
            val ci = this.ci!!
            var c = ci.current()
            while (!endOfInput) {
                if (c == '{') {
                    curlyBracketsCount++
                    sb.append(c)
                } else if (c == '}') {
                    curlyBracketsCount--
                    sb.append(c)
                } else if (isIp && c == '[') {
                    squareBracketsCount++
                    sb.append(c)
                } else if (isIp && c == ']') {
                    squareBracketsCount--
                    sb.append(c)

                    // test IPv6 or regular expressions in the template params
                } else if ((!isIp || squareBracketsCount == 0) && curlyBracketsCount == 0 &&
                    delimiters != null && delimiters.indexOf(c) >= 0
                ) {
                    return if (sb.isEmpty()) null else sb.toString()
                } else {
                    sb.append(c)
                }
                endOfInput = !ci.hasNext()
                if (!endOfInput) {
                    c = ci.next()
                }
            }
            if (mayEnd) {
                return if (sb.isEmpty()) null else sb.toString()
            }
            throw IllegalArgumentException("Component does not end by a delimiter ''$delimiters'' at index ${ci.pos()}.")
        }

        /**
         * Parses the input string URI. After calling this method The result components can be retrieved by calling appropriate
         * getter methods like [host], [port], etc.
         */
        fun parse() {
            this.parserExecuted = true
            val ci = CharacterIterator(input)
            this.ci = ci
            if (!ci.hasNext()) {
                // empty string on input -> set both SSP and path to ""
                this.path = ""
                this.ssp = ""
                return
            }
            ci.next()
            val comp = parseComponent(":/?#", true)

            if (ci.hasNext()) {
                this.ssp = ci.input.substring(ci.pos() + 1)
            }

            this.opaque = false
            if (ci.current() == ':') {
                // absolute
                if (comp == null) {
                    throw IllegalArgumentException("Expected scheme name at index " + ci.pos() + ": ''" + input + "''")
                }
                scheme = comp
                if (!ci.hasNext()) {
                    // empty SSP/path -> set both SSP and path to ""
                    this.path = ""
                    this.ssp = ""
                    return
                }
                val c = ci.next()
                if (c == '/') {
                    // hierarchical
                    parseHierarchicalUri()
                } else {
                    // opaque
                    this.opaque = true
                }
            } else {
                ci.setPosition(0)
                // relative
                if (ci.current() == '/') {
                    parseHierarchicalUri()
                } else {
                    parsePath()
                }
            }
        }

        private fun parseHierarchicalUri() {
            val ci = this.ci!!
            if (ci.hasNext() && ci.peek() == '/') {
                // authority
                ci.next()
                ci.next()
                parseAuthority()
            }
            if (!ci.hasNext()) {
                if (ci.current() == '/') {
                    path = "/"
                }
                return
            }
            parsePath()
        }

        private fun parseAuthority() {
            val ci = this.ci!!
            val start = ci.pos()
            var comp = parseComponentWithIP("@/?#", true)
            if (ci.current() == '@') {
                this.userInfo = comp
                if (!ci.hasNext()) {
                    return
                }
                ci.next()
                comp = parseComponentWithIP(":/?#", true)
            } else {
                ci.setPosition(start)
                comp = parseComponentWithIP("@:/?#", true)
            }

            this.host = comp

            if (ci.current() == ':') {
                if (!ci.hasNext()) {
                    return
                }
                ci.next()
                this.port = parseComponent("/?#", true)
            }
            this.authority = ci.input.substring(start, ci.pos())
            if (this.authority.isNullOrEmpty()) {
                this.authority = null
            }
        }

        private fun parsePath() {
            this.path = parseComponent("?#", true)
            val ci = this.ci!!

            if (ci.current() == '?') {
                if (!ci.hasNext()) {
                    return
                }
                ci.next() // skip ?

                this.query = parseComponent("#", true)
            }

            if (ci.current() == '#') {
                if (!ci.hasNext()) {
                    return
                }
                ci.next() // skip #

                this.fragment = parseComponent(null, true)
            }
        }
    }
