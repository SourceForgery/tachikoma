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

import com.google.common.collect.Multimap
import com.google.common.collect.MultimapBuilder
import java.io.UnsupportedEncodingException
import java.net.URI
import java.net.URLDecoder
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.Charset
import java.util.Arrays
import java.util.LinkedList

/**
 * Utility class for validating, encoding and decoding components
 * of a URI.
 *
 * @author Paul Sandoz
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
object UriComponent {

    private val HEX_DIGITS = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F')

    private val SCHEME = listOf("0-9", "A-Z", "a-z", "+", "-", ".")
    private val UNRESERVED = listOf("0-9", "A-Z", "a-z", "-", ".", "_", "~")
    private val SUB_DELIMS = listOf("!", "$", "&", "'", "(", ")", "*", "+", ",", ";", "=")
    private val ENCODING_TABLES = initEncodingTables()

    private val UTF_8_CHARSET = Charset.forName("UTF-8")

    private val HEX_TABLE = initHexTable()

    // TODO rewrite to use masks and not lookup tables

    /**
     * The URI component type.
     */
    enum class Type {

        /**
         * ALPHA / DIGIT / "-" / "." / "_" / "~" characters.
         */
        UNRESERVED,
        /**
         * The URI scheme component type.
         */
        SCHEME,
        /**
         * The URI authority component type.
         */
        AUTHORITY,
        /**
         * The URI user info component type.
         */
        USER_INFO,
        /**
         * The URI host component type.
         */
        HOST,
        /**
         * The URI port component type.
         */
        PORT,
        /**
         * The URI path component type.
         */
        PATH,
        /**
         * The URI path component type that is a path segment.
         */
        PATH_SEGMENT,
        /**
         * The URI path component type that is a matrix parameter.
         */
        MATRIX_PARAM,
        /**
         * The URI query component type, encoded using application/x-www-form-urlencoded rules.
         */
        QUERY,
        /**
         * The URI query component type that is a query parameter, encoded using
         * application/x-www-form-urlencoded rules (space character is encoded
         * as `+`).
         */
        QUERY_PARAM,
        /**
         * The URI query component type that is a query parameter, encoded using
         * application/x-www-form-urlencoded (space character is encoded as
         * `%20`).
         */
        QUERY_PARAM_SPACE_ENCODED,
        /**
         * The URI fragment component type.
         */
        FRAGMENT
    }

    /**
     * Validates the legal characters of a percent-encoded string that
     * represents a URI component type.
     *
     * @param s the encoded string.
     * @param t the URI component type identifying the legal characters.
     * @param template true if the encoded string contains URI template variables
     * @throws IllegalArgumentException if the encoded string contains illegal
     * characters.
     */
    @JvmOverloads
    fun validate(s: String, t: Type, template: Boolean = false) {
        val i = _valid(s, t, template)
        if (i > -1) {
            throw IllegalArgumentException("The string ''" + s + "'' for the URI component " + t + " contains an invalid character, ''" + s[i] + "'', at index " + i + ".")
        }
    }

    /**
     * Validates the legal characters of a percent-encoded string that
     * represents a URI component type.
     *
     * @param s the encoded string.
     * @param t the URI component type identifying the legal characters.
     * @param template true if the encoded string contains URI template variables
     * @return true if the encoded string is valid, otherwise false.
     */
    @JvmOverloads
    fun valid(s: String, t: Type, template: Boolean = false): Boolean {
        return _valid(s, t, template) == -1
    }

    private fun _valid(s: String, t: Type, template: Boolean): Int {
        val table = ENCODING_TABLES[t.ordinal]

        for (i in 0 until s.length) {
            val c = s[i]
            if (c.code < 0x80 && c != '%' && !table[c.code] || c.code >= 0x80) {
                if (!template || c != '{' && c != '}') {
                    return i
                }
            }
        }
        return -1
    }

    /**
     * Contextually encodes the characters of string that are either non-ASCII
     * characters or are ASCII characters that must be percent-encoded using the
     * UTF-8 encoding. Percent-encoded characters will be recognized and not
     * double encoded.
     *
     * @param s the string to be encoded.
     * @param t the URI component type identifying the ASCII characters that
     * must be percent-encoded.
     * @return the encoded string.
     */
    fun contextualEncode(s: String, t: Type): String {
        return _encode(s, t, false, true)
    }

    /**
     * Contextually encodes the characters of string that are either non-ASCII
     * characters or are ASCII characters that must be percent-encoded using the
     * UTF-8 encoding. Percent-encoded characters will be recognized and not
     * double encoded.
     *
     * @param s the string to be encoded.
     * @param t the URI component type identifying the ASCII characters that
     * must be percent-encoded.
     * @param template true if the encoded string contains URI template variables
     * @return the encoded string.
     */
    fun contextualEncode(s: String, t: Type, template: Boolean): String {
        return _encode(s, t, template, true)
    }

    /**
     * Encodes the characters of string that are either non-ASCII characters
     * or are ASCII characters that must be percent-encoded using the
     * UTF-8 encoding.
     *
     * @param s the string to be encoded.
     * @param t the URI component type identifying the ASCII characters that
     * must be percent-encoded.
     * @return the encoded string.
     */
    fun encode(s: String, t: Type): String {
        return _encode(s, t, false, false)
    }

    /**
     * Encodes the characters of string that are either non-ASCII characters
     * or are ASCII characters that must be percent-encoded using the
     * UTF-8 encoding.
     *
     * @param s the string to be encoded.
     * @param t the URI component type identifying the ASCII characters that
     * must be percent-encoded.
     * @param template true if the encoded string contains URI template variables
     * @return the encoded string.
     */
    fun encode(s: String, t: Type, template: Boolean): String {
        return _encode(s, t, template, false)
    }

    /**
     * Encodes a string with template parameters names present, specifically the
     * characters '{' and '}' will be percent-encoded.
     *
     * @param s the string with zero or more template parameters names
     * @return the string with encoded template parameters names.
     */
    fun encodeTemplateNames(s: String): String {
        var result = s
        var i = result.indexOf('{')
        if (i != -1) {
            result = result.replace("{", "%7B")
        }
        i = result.indexOf('}')
        if (i != -1) {
            result = result.replace("}", "%7D")
        }

        return result
    }

    private fun _encode(s: String, t: Type, template: Boolean, contextualEncode: Boolean): String {
        val table = ENCODING_TABLES[t.ordinal]
        var insideTemplateParam = false

        var sb: StringBuilder? = null
        var offset = 0
        var codePoint: Int
        while (offset < s.length) {
            codePoint = s.codePointAt(offset)

            if (codePoint < 0x80 && table[codePoint]) {
                sb?.append(codePoint.toChar())
            } else {
                if (template) {
                    var leavingTemplateParam = false
                    if (codePoint == '{'.code) {
                        insideTemplateParam = true
                    } else if (codePoint == '}'.code) {
                        insideTemplateParam = false
                        leavingTemplateParam = true
                    }
                    if (insideTemplateParam || leavingTemplateParam) {
                        sb?.append(Character.toChars(codePoint))
                        offset += Character.charCount(codePoint)
                        continue
                    }
                }

                if (contextualEncode &&
                    codePoint == '%'.code &&
                    offset + 2 < s.length &&
                    isHexCharacter(s[offset + 1]) &&
                    isHexCharacter(s[offset + 2])
                ) {
                    sb?.append('%')?.append(s[offset + 1])?.append(s[offset + 2])
                    offset += 2
                    offset += Character.charCount(codePoint)
                    continue
                }

                if (sb == null) {
                    sb = StringBuilder()
                    sb.append(s.substring(0, offset))
                }

                if (codePoint < 0x80) {
                    if (codePoint == ' '.code && t == Type.QUERY_PARAM) {
                        sb.append('+')
                    } else {
                        appendPercentEncodedOctet(sb, codePoint.toChar().code)
                    }
                } else {
                    appendUTF8EncodedCharacter(sb, codePoint)
                }
            }
            offset += Character.charCount(codePoint)
        }

        return sb?.toString() ?: s
    }

    private fun appendPercentEncodedOctet(sb: StringBuilder, b: Int) {
        sb.append('%')
        sb.append(HEX_DIGITS[b shr 4])
        sb.append(HEX_DIGITS[b and 0x0F])
    }

    private fun appendUTF8EncodedCharacter(sb: StringBuilder, codePoint: Int) {
        val chars = CharBuffer.wrap(Character.toChars(codePoint))
        val bytes = UTF_8_CHARSET.encode(chars)

        while (bytes.hasRemaining()) {
            appendPercentEncodedOctet(sb, bytes.get().toInt() and 0xFF)
        }
    }

    private fun initEncodingTables(): Array<BooleanArray> {
        val tables = arrayOfNulls<BooleanArray>(Type.values().size)
        tables[Type.SCHEME.ordinal] = initEncodingTable(SCHEME)
        tables[Type.UNRESERVED.ordinal] = initEncodingTable(UNRESERVED)
        tables[Type.HOST.ordinal] = initEncodingTable(UNRESERVED + SUB_DELIMS)
        tables[Type.PORT.ordinal] = initEncodingTable(listOf("0-9"))

        tables[Type.USER_INFO.ordinal] = initEncodingTable(UNRESERVED + SUB_DELIMS + listOf(":"))

        tables[Type.AUTHORITY.ordinal] = initEncodingTable(UNRESERVED + SUB_DELIMS + listOf(":", "@"))

        val pathSegment = initEncodingTable(UNRESERVED + SUB_DELIMS + listOf(":", "@"))
        pathSegment[';'.code] = false
        tables[Type.PATH_SEGMENT.ordinal] = pathSegment

        val matrixParam = initEncodingTable(UNRESERVED + SUB_DELIMS + listOf(":", "@"))
        matrixParam['='.code] = false
        pathSegment[';'.code] = false
        tables[Type.MATRIX_PARAM.ordinal] = matrixParam

        tables[Type.PATH.ordinal] = initEncodingTable(UNRESERVED + SUB_DELIMS + listOf(":", "@", "/"))

        val query = initEncodingTable(UNRESERVED + SUB_DELIMS + listOf(":", "@", "/"))
        query['!'.code] = false
        query['*'.code] = false
        query['\''.code] = false
        query['('.code] = false
        query[')'.code] = false
        query[';'.code] = false
        query[':'.code] = false
        query['@'.code] = false
        query['$'.code] = false
        query[','.code] = false
        query['/'.code] = false
        query['?'.code] = false
        tables[Type.QUERY.ordinal] = query

        val queryParam = query.copyOf()
        tables[Type.QUERY_PARAM.ordinal] = queryParam
        queryParam['='.code] = false
        queryParam['+'.code] = false
        queryParam['&'.code] = false

        tables[Type.QUERY_PARAM_SPACE_ENCODED.ordinal] = tables[Type.QUERY_PARAM.ordinal]

        tables[Type.FRAGMENT.ordinal] = tables[Type.QUERY.ordinal]
        return tables.requireNoNulls()
    }

    private fun initEncodingTable(allowed: List<String>): BooleanArray {
        val table = BooleanArray(0x80)
        for (range in allowed) {
            if (range.length == 1) {
                table[range[0].code] = true
            } else if (range.length == 3 && range[1] == '-') {
                var i = range[0].code
                while (i <= range[2].code) {
                    table[i] = true
                    i++
                }
            }
        }

        return table
    }

    /**
     * Decodes characters of a string that are percent-encoded octets using
     * UTF-8 decoding (if needed).
     *
     *
     * It is assumed that the string is valid according to an (unspecified) URI
     * component type. If a sequence of contiguous percent-encoded octets is
     * not a valid UTF-8 character then the octets are replaced with '\uFFFD'.
     *
     *
     * If the URI component is of type HOST then any "%" found between "[]" is
     * left alone. It is an IPv6 literal with a scope_id.
     *
     *
     * If the URI component is of type QUERY_PARAM then any "+" is decoded as
     * as ' '.
     *
     *
     *
     * @param s the string to be decoded.
     * @param t the URI component type, may be null.
     * @return the decoded string.
     * @throws IllegalArgumentException if a malformed percent-encoded octet is
     * detected
     */
    fun decode(s: String?, t: Type?): String {
        if (s == null) {
            throw IllegalArgumentException()
        }

        val n = s.length
        if (n == 0) {
            return s
        }

        // If there are no percent-escaped octets
        if (s.indexOf('%') < 0) {
            // If there are no '+' characters for query param
            if (t == Type.QUERY_PARAM) {
                if (s.indexOf('+') < 0) {
                    return s
                }
            } else {
                return s
            }
        } else {
            // Malformed percent-escaped octet at the end
            if (n < 2) {
                throw IllegalArgumentException("Malformed percent-encoded octet at index 1.")
            }

            // Malformed percent-escaped octet at the end
            if (s[n - 2] == '%') {
                throw IllegalArgumentException("Malformed percent-encoded octet at index " + (n - 2) + ".")
            }
        }

        if (t == null) {
            return decode(s, n)
        }

        when (t) {
            Type.HOST -> return decodeHost(s, n)
            Type.QUERY_PARAM -> return decodeQueryParam(s, n)
            else -> return decode(s, n)
        }
    }

    /**
     * Decode the query component of a URI.
     *
     *
     * Query parameter names in the returned map are always decoded. Decoding of query parameter
     * values can be controlled using the `decode` parameter flag.
     *
     *
     * @param u the URI.
     * @param decode `true` if the returned query parameter values of the query component
     * should be in decoded form.
     * @return the multivalued map of query parameters.
     */
    fun decodeQuery(u: URI, decode: Boolean): Multimap<String, String> {
        return decodeQuery(u.rawQuery, decode)
    }

    /**
     * Decode the query component of a URI.
     *
     *
     * Query parameter names in the returned map are always decoded. Decoding of query parameter
     * values can be controlled using the `decode` parameter flag.
     *
     *
     * @param q the query component in encoded form.
     * @param decode `true` if the returned query parameter values of the query component
     * should be in decoded form.
     * @return the multivalued map of query parameters.
     */
    fun decodeQuery(q: String, decode: Boolean): Multimap<String, String> {
        return decodeQuery(q, true, decode)
    }

    /**
     * Decode the query component of a URI.
     *
     *
     * Decoding of query parameter names and values can be controlled using the `decodeNames`
     * and `decodeValues` parameter flags.
     *
     *
     * @param q the query component in encoded form.
     * @param decodeNames `true` if the returned query parameter names of the query component
     * should be in decoded form.
     * @param decodeValues `true` if the returned query parameter values of the query component
     * should be in decoded form.
     * @return the multivalued map of query parameters.
     */
    fun decodeQuery(
        q: String?,
        decodeNames: Boolean,
        decodeValues: Boolean
    ): Multimap<String, String> {
        val queryParameters = MultimapBuilder.linkedHashKeys().arrayListValues().build<String, String>()

        if (q == null || q.length == 0) {
            return queryParameters
        }

        var s = 0
        do {
            val e = q.indexOf('&', s)

            if (e == -1) {
                decodeQueryParam(queryParameters, q.substring(s), decodeNames, decodeValues)
            } else if (e > s) {
                decodeQueryParam(queryParameters, q.substring(s, e), decodeNames, decodeValues)
            }
            s = e + 1
        } while (s > 0 && s < q.length)

        return queryParameters
    }

    private fun decodeQueryParam(
        params: Multimap<String, String>,
        param: String,
        decodeNames: Boolean,
        decodeValues: Boolean
    ) {
        try {
            val equals = param.indexOf('=')
            if (equals > 0) {
                params.put(
                    if (decodeNames) URLDecoder.decode(param.substring(0, equals), "UTF-8") else param.substring(0, equals),
                    if (decodeValues) URLDecoder.decode(param.substring(equals + 1), "UTF-8") else param.substring(equals + 1)
                )
            } else if (equals == 0) {
                // no key declared, ignore
            } else if (param.length > 0) {
                params.put(if (decodeNames) URLDecoder.decode(param, "UTF-8") else param, "")
            }
        } catch (ex: UnsupportedEncodingException) {
            // This should never occur
            throw IllegalArgumentException(ex)
        }
    }

    /**
     * Decode the path component of a URI as path segments.
     *
     * @param u the URI. If the path component is an absolute path component
     * then the leading '/' is ignored and is not considered a delimiator
     * of a path segment.
     * @param decode true if the path segments of the path component
     * should be in decoded form.
     * @return the list of path segments.
     */
    fun decodePath(u: URI, decode: Boolean): List<PathSegment> {
        var rawPath: String? = u.rawPath
        if (rawPath != null && rawPath.length > 0 && rawPath[0] == '/') {
            rawPath = rawPath.substring(1)
        }
        return decodePath(rawPath, decode)
    }

    /**
     * Decode the path component of a URI as path segments.
     *
     *
     * Any '/' character in the path is considered to be a deliminator
     * between two path segments. Thus if the path is '/' then the path segment
     * list will contain two empty path segments. If the path is "//" then
     * the path segment list will contain three empty path segments. If the path
     * is "/a/" the path segment list will consist of the following path
     * segments in order: "", "a" and "".
     *
     *
     * @param path the path component in encoded form.
     * @param decode true if the path segments of the path component
     * should be in decoded form.
     * @return the list of path segments.
     */
    fun decodePath(path: String?, decode: Boolean): List<PathSegment> {
        val segments = LinkedList<PathSegment>()

        if (path == null) {
            return segments
        }

        var s: Int
        var e = -1
        do {
            s = e + 1
            e = path.indexOf('/', s)

            if (e > s) {
                decodePathSegment(segments, path.substring(s, e), decode)
            } else if (e == s) {
                segments.add(PathSegment.EMPTY_PATH_SEGMENT)
            }
        } while (e != -1)
        if (s < path.length) {
            decodePathSegment(segments, path.substring(s), decode)
        } else {
            segments.add(PathSegment.EMPTY_PATH_SEGMENT)
        }
        return segments
    }

    /**
     * Decode the path segment and add it to the list of path segments.
     *
     * @param segments mutable list of path segments.
     * @param segment path segment to be decoded.
     * @param decode `true` if the path segment should be in a decoded form.
     */
    fun decodePathSegment(segments: MutableList<PathSegment>, segment: String, decode: Boolean) {
        val colon = segment.indexOf(';')
        if (colon != -1) {
            segments.add(
                PathSegment(
                    if (colon == 0) "" else segment.substring(0, colon),
                    decode,
                    decodeMatrix(
                        segment,
                        decode
                    )
                )
            )
        } else {
            segments.add(PathSegment(segment, decode))
        }
    }

    /**
     * Decode the matrix component of a URI path segment.
     *
     * @param pathSegment the path segment component in encoded form.
     * @param decode true if the matrix parameters of the path segment component
     * should be in decoded form.
     * @return the multivalued map of matrix parameters.
     */
    fun decodeMatrix(pathSegment: String, decode: Boolean): Multimap<String, String> {
        val matrixMap = MultimapBuilder.linkedHashKeys().arrayListValues().build<String, String>()

        // Skip over path segment
        var s = pathSegment.indexOf(';') + 1
        if (s == 0 || s == pathSegment.length) {
            return matrixMap
        }

        do {
            val e = pathSegment.indexOf(';', s)

            if (e == -1) {
                decodeMatrixParam(matrixMap, pathSegment.substring(s), decode)
            } else if (e > s) {
                decodeMatrixParam(matrixMap, pathSegment.substring(s, e), decode)
            }
            s = e + 1
        } while (s > 0 && s < pathSegment.length)

        return matrixMap
    }

    private fun decodeMatrixParam(params: Multimap<String, String>, param: String, decode: Boolean) {
        val equals = param.indexOf('=')
        if (equals > 0) {
            params.put(
                decode(param.substring(0, equals), Type.MATRIX_PARAM),
                if (decode)
                    decode(param.substring(equals + 1), Type.MATRIX_PARAM)
                else
                    param
                        .substring(equals + 1)
            )
        } else if (equals == 0) {
            // no key declared, ignore
        } else if (param.length > 0) {
            params.put(decode(param, Type.MATRIX_PARAM), "")
        }
    }

    private fun decode(s: String, n: Int): String {
        val sb = StringBuilder(n)
        var bb: ByteBuffer? = null

        var i = 0
        while (i < n) {
            val c = s[i++]
            if (c != '%') {
                sb.append(c)
            } else {
                bb = decodePercentEncodedOctets(s, i, bb)
                i = decodeOctets(i, bb, sb)
            }
        }

        return sb.toString()
    }

    private fun decodeQueryParam(s: String, n: Int): String {
        val sb = StringBuilder(n)
        var bb: ByteBuffer? = null

        var i = 0
        while (i < n) {
            val c = s[i++]
            if (c != '%') {
                if (c != '+') {
                    sb.append(c)
                } else {
                    sb.append(' ')
                }
            } else {
                bb = decodePercentEncodedOctets(s, i, bb)
                i = decodeOctets(i, bb, sb)
            }
        }

        return sb.toString()
    }

    private fun decodeHost(s: String, n: Int): String {
        val sb = StringBuilder(n)
        var bb: ByteBuffer? = null

        var betweenBrackets = false
        var i = 0
        while (i < n) {
            val c = s[i++]
            if (c == '[') {
                betweenBrackets = true
            } else if (betweenBrackets && c == ']') {
                betweenBrackets = false
            }

            if (c != '%' || betweenBrackets) {
                sb.append(c)
            } else {
                bb = decodePercentEncodedOctets(s, i, bb)
                i = decodeOctets(i, bb, sb)
            }
        }

        return sb.toString()
    }

    /**
     * Decode a continuous sequence of percent encoded octets.
     *
     *
     * Assumes the index, i, starts that the first hex digit of the first
     * percent-encoded octet.
     */
    private fun decodePercentEncodedOctets(s: String, i: Int, bb: ByteBuffer?): ByteBuffer {
        var position = i
        var buffer = if (bb == null) {
            ByteBuffer.allocate(1)
        } else {
            bb.clear()
            bb
        }

        while (true) {
            // Decode the hex digits
            buffer.put((decodeHex(s, position++) shl 4 or decodeHex(s, position++)).toByte())

            // Finish if at the end of the string
            if (position == s.length) {
                break
            }

            // Finish if no more percent-encoded octets follow
            if (s[position++] != '%') {
                break
            }

            // Check if the byte buffer needs to be increased in size
            if (buffer.position() == buffer.capacity()) {
                buffer.flip()
                // Create a new byte buffer with the maximum number of possible
                // octets, hence resize should only occur once
                val bb_new = ByteBuffer.allocate(s.length / 3)
                bb_new.put(buffer)
                buffer = bb_new
            }
        }

        buffer.flip()
        return buffer
    }

    /**
     * Decodes octets to characters using the UTF-8 decoding and appends
     * the characters to a StringBuffer.
     *
     * @return the index to the next unchecked character in the string to decode
     */
    private fun decodeOctets(i: Int, bb: ByteBuffer, sb: StringBuilder): Int {
        // If there is only one octet and is an ASCII character
        if (bb.limit() == 1 && bb.get(0).toInt() and 0xFF < 0x80) {
            // Octet can be appended directly
            sb.append(bb.get(0).toInt().toChar())
            return i + 2
        } else {
            //
            val cb = UTF_8_CHARSET.decode(bb)
            sb.append(cb.toString())
            return i + bb.limit() * 3 - 1
        }
    }

    private fun decodeHex(s: String, i: Int): Int {
        val v = decodeHex(s[i])
        if (v == -1) {
            throw IllegalArgumentException("Malformed percent-encoded octet at index " + i + ", invalid hexadecimal digit ''" + s[i] + "''.")
        }
        return v
    }

    private fun initHexTable(): IntArray {
        val table = IntArray(0x80)
        Arrays.fill(table, -1)

        run {
            var c = '0'
            while (c <= '9') {
                table[c.code] = c - '0'
                c++
            }
        }
        run {
            var c = 'A'
            while (c <= 'F') {
                table[c.code] = c - 'A' + 10
                c++
            }
        }
        var c = 'a'
        while (c <= 'f') {
            table[c.code] = c - 'a' + 10
            c++
        }
        return table
    }

    private fun decodeHex(c: Char): Int {
        return if (c.code < 128) HEX_TABLE[c.code] else -1
    }

    /**
     * Checks whether the character `c` is hexadecimal character.
     *
     * @param c Any character
     * @return The is `c` is a hexadecimal character (e.g. 0, 5, a, A, f, ...)
     */
    fun isHexCharacter(c: Char): Boolean {
        return c.code < 128 && HEX_TABLE[c.code] != -1
    }

    /**
     * Return the `Request-Uri` representation as defined by HTTP spec. For example:
     * <pre>&lt;Method> &lt;Request-URI> HTTP/&lt;Version> (e.g. GET /auth;foo=bar/hello?foo=bar HTTP/1.1)</pre>
     *
     * @param uri uri to obtain `Request-Uri` from.
     * @return `Request-Uri` representation or `null` if `uri` is not provided.
     */
    fun fullRelativeUri(uri: URI?): String? {
        uri ?: return null

        val query = uri.rawQuery
            ?: ""

        return uri.rawPath + if (query.length > 0) "?$query" else ""
    }
}
class PathSegment
internal constructor(
    path: String,
    decode: Boolean,
    matrixParameters: Multimap<String, String> = emptyMultimap()
) {
    val path: String
    val matrixParameters = MultimapBuilder.linkedHashKeys().arrayListValues().build(matrixParameters)

    init {
        this.path = if (decode) UriComponent.decode(path, UriComponent.Type.PATH_SEGMENT) else path
    }

    override fun toString(): String {
        return path
    }

    companion object {

        internal val EMPTY_PATH_SEGMENT = PathSegment("", false)
    }
}
