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

@file:Suppress("UnstableApiUsage")

package com.sourceforgery.jersey.uribuilder

import com.google.common.collect.LinkedHashMultimap
import com.google.common.collect.Multimap
import com.google.common.net.InetAddresses
import java.net.URI
import java.net.URISyntaxException
import java.util.HashMap

/**
 *
 * @author Paul Sandoz
 * @author Martin Matula
 * @author Miroslav Fuksa
 * @author Vetle Leinonen-Roeim (vetle at roeim.net)
 */
class JerseyUriBuilder
private constructor(
    private val path: StringBuilder = StringBuilder(),
    private val query: StringBuilder = StringBuilder()
) {

    // All fields should be in the percent-encoded form
    private var scheme: String? = null

    private var ssp: String? = null

    private var authority: String? = null

    private var userInfo: String? = null

    private var host: String? = null

    private var port: String? = null

    private var matrixParams: Multimap<String, String>? = null

    private var queryParams: Multimap<String, String>? = null

    private var fragment: String? = null

    /**
     * Check whether or not the URI represented by this `UriBuilder` is absolute.
     *
     *
     * A URI is absolute if, and only if, it has a scheme component.
     *
     * @return `true` if, and only if, the URI represented by this `UriBuilder` is absolute.
     * @since 2.7
     */
    val isAbsolute: Boolean
        get() = scheme != null

    /**
     * Create new implementation of `UriBuilder`.
     */
    private constructor(that: JerseyUriBuilder) : this(
        path = StringBuilder(that.path),
        query = StringBuilder(that.query)
    ) {
        this.scheme = that.scheme
        this.ssp = that.ssp
        this.authority = that.authority
        this.userInfo = that.userInfo
        this.host = that.host
        this.port = that.port
        this.matrixParams = if (that.matrixParams == null) null else LinkedHashMultimap.create(that.matrixParams!!)
        this.queryParams = if (that.queryParams == null) null else LinkedHashMultimap.create(that.queryParams!!)
        this.fragment = that.fragment
    }

    fun uri(uri: URI): JerseyUriBuilder {
        if (uri.rawFragment != null) {
            fragment = uri.rawFragment
        }

        if (uri.isOpaque) {
            scheme = uri.scheme
            ssp = uri.rawSchemeSpecificPart
            return this
        }

        if (uri.scheme == null) {
            if (ssp != null) {
                if (uri.rawSchemeSpecificPart != null) {
                    ssp = uri.rawSchemeSpecificPart
                    return this
                }
            }
        } else {
            scheme = uri.scheme
        }

        ssp = null
        if (uri.rawAuthority != null) {
            if (uri.rawUserInfo == null && uri.host == null && uri.port == -1) {
                authority = uri.rawAuthority
                userInfo = null
                host = null
                port = null
            } else {
                authority = null
                if (uri.rawUserInfo != null) {
                    userInfo = uri.rawUserInfo
                }
                if (uri.host != null) {
                    host = uri.host
                }
                if (uri.port != -1) {
                    port = uri.port.toString()
                }
            }
        }

        if (uri.rawPath != null && !uri.rawPath.isEmpty()) {
            path.setLength(0)
            path.append(uri.rawPath)
        }
        if (uri.rawQuery != null && !uri.rawQuery.isEmpty()) {
            query.setLength(0)
            query.append(uri.rawQuery)
        }

        return this
    }

    constructor(uriTemplate: String) : this() {
        uri(uriTemplate)
    }

    constructor(uri: URI) : this() {
        uri(uri)
    }

    fun uri(uriTemplate: String): JerseyUriBuilder {
        var parser = UriParser(uriTemplate)
        parser.parse()

        val parsedScheme = parser.scheme
        if (parsedScheme != null) {
            scheme(parsedScheme)
            if (ssp != null) {
                // The previously set scheme was opaque and uriTemplate does not contain a scheme part.
                // However, the scheme might have already changed, as demonstrated in
                // JerseyUriBuilderTest.testChangeUriStringAfterChangingOpaqueSchemeToHttp().
                // So to be safe, we need to erase the existing internal SSP value and
                // re-parse the new uriTemplate using the current scheme and try to set the SSP
                // again using the re-parsed data.
                // See also JERSEY-457 and related test.
                ssp = null
                parser = UriParser("$scheme:$uriTemplate")
                parser.parse()
            }
        }

        schemeSpecificPart(parser)

        val parserFragment = parser.fragment
        if (parserFragment != null) {
            fragment(parserFragment)
        }

        return this
    }

    /**
     * Set scheme specific part from the URI parser.
     *
     * @param parser initialized URI parser.
     */
    private fun schemeSpecificPart(parser: UriParser) {
        if (parser.isOpaque) {
            if (parser.ssp != null) {
                this.port = null
                this.host = this.port
                this.authority = this.host
                this.path.setLength(0)
                this.query.setLength(0)

                // TODO encode or validate scheme specific part
                this.ssp = parser.ssp
            }
            return
        }

        this.ssp = null
        val authority = parser.authority
        if (authority != null) {
            if (parser.userInfo == null && parser.host == null && parser.port == null) {
                this.authority = encode(authority, UriComponent.Type.AUTHORITY)
                this.userInfo = null
                this.host = null
                this.port = null
            } else {
                this.authority = null
                if (parser.userInfo != null) {
                    userInfo(parser.userInfo)
                }
                if (parser.host != null) {
                    host(parser.host)
                }
                if (parser.port != null) {
                    this.port = parser.port
                }
            }
        }

        val path = parser.path
        if (path != null) {
            this.path.setLength(0)
            path(path)
        }
        if (parser.query != null) {
            this.query.setLength(0)
            this.query.append(parser.query)
        }
    }

    fun scheme(scheme: String?): JerseyUriBuilder {
        if (scheme != null) {
            this.scheme = scheme
            UriComponent.validate(scheme, UriComponent.Type.SCHEME, true)
        } else {
            this.scheme = null
        }
        return this
    }

    fun schemeSpecificPart(ssp: String): JerseyUriBuilder {
        val parser = UriParser(if (scheme != null) "$scheme:$ssp" else ssp)
        parser.parse()

        if (parser.scheme != null && parser.scheme != scheme) {
            throw IllegalStateException("Supplied scheme-specific URI part '" + ssp + "' contains unexpected URI Scheme component: '" + parser.scheme + "'.")
        }
        if (parser.fragment != null) {
            throw IllegalStateException("Supplied scheme-specific URI part '" + ssp + "' contains URI Fragment component: '" + parser.fragment + "'.")
        }

        schemeSpecificPart(parser)

        return this
    }

    fun userInfo(ui: String?): JerseyUriBuilder {
        checkSsp()
        this.userInfo = if (ui != null)
            encode(ui, UriComponent.Type.USER_INFO)
        else
            null
        return this
    }

    fun host(host: String?): JerseyUriBuilder {
        checkSsp()
        if (host != null) {
            if (host.isEmpty()) {
                throw IllegalArgumentException("Invalid host name.")
            }
            if (InetAddresses.isMappedIPv4Address(host) || InetAddresses.isUriInetAddress(host)) {
                this.host = host
            } else {
                this.host = encode(host, UriComponent.Type.HOST)
            }
        } else {
            // null is used to reset host setting
            this.host = null
        }
        return this
    }

    fun port(port: Int): JerseyUriBuilder {
        checkSsp()
        if (port < -1) {
            // -1 is used to reset port setting and since URI allows
            // as port any positive integer, so do we.

            throw IllegalArgumentException("Invalid port value.")
        }
        this.port = if (port == -1) null else port.toString()
        return this
    }

    fun replacePath(path: String?): JerseyUriBuilder {
        checkSsp()
        this.path.setLength(0)
        if (path != null) {
            appendPath(path)
        }
        return this
    }

    fun path(path: String): JerseyUriBuilder {
        checkSsp()
        appendPath(path)
        return this
    }

    fun paths(vararg paths: String): JerseyUriBuilder {
        checkSsp()
        for (path in paths) {
            appendPath(path)
        }
        return this
    }

    @Throws(IllegalArgumentException::class)
    fun segment(vararg segments: String): JerseyUriBuilder {
        checkSsp()
        for (segment in segments) {
            appendPath(segment, true)
        }
        return this
    }

    fun replaceMatrix(matrix: String?): JerseyUriBuilder {
        checkSsp()
        val trailingSlash = path[path.length - 1] == '/'
        val slashIndex = if (trailingSlash) path.lastIndexOf("/", path.length - 2) else path.lastIndexOf("/")

        val i = path.indexOf(";", slashIndex)

        if (i != -1) {
            path.setLength(i + 1)
        } else if (matrix != null) {
            path.append(';')
        }

        if (matrix != null) {
            path.append(encode(matrix, UriComponent.Type.PATH))
        } else if (i != -1) {
            path.setLength(i)

            if (trailingSlash) {
                path.append("/")
            }
        }

        return this
    }

    fun matrixParam(name: String, vararg values: Any): JerseyUriBuilder {
        checkSsp()
        if (values.isEmpty()) {
            return this
        }

        val encodedName = encode(name, UriComponent.Type.MATRIX_PARAM)
        if (matrixParams == null) {
            for (value in values) {
                path.append(';').append(encodedName)

                val stringValue = value.toString()
                if (!stringValue.isEmpty()) {
                    path.append('=').append(encode(stringValue, UriComponent.Type.MATRIX_PARAM))
                }
            }
        } else {
            for (value in values) {
                matrixParams!!.put(encodedName, encode(value.toString(), UriComponent.Type.MATRIX_PARAM))
            }
        }
        return this
    }

    fun replaceMatrixParam(name: String, vararg values: Any): JerseyUriBuilder {
        checkSsp()

        if (matrixParams == null) {
            var i = path.lastIndexOf("/")
            if (i == -1) {
                i = 0
            }
            matrixParams = UriComponent.decodeMatrix(path.substring(i), false)
            i = path.indexOf(";", i)
            if (i != -1) {
                path.setLength(i)
            }
        }

        val encodedName = encode(name, UriComponent.Type.MATRIX_PARAM)
        matrixParams!!.removeAll(encodedName)
        for (value in values) {
            matrixParams!!.put(encodedName, encode(value.toString(), UriComponent.Type.MATRIX_PARAM))
        }
        return this
    }

    fun replaceQuery(query: String?): JerseyUriBuilder {
        checkSsp()
        this.query.setLength(0)
        if (query != null) {
            this.query.append(encode(query, UriComponent.Type.QUERY))
        }
        return this
    }

    fun queryParam(name: String, vararg values: Any): JerseyUriBuilder {
        checkSsp()
        if (values.isEmpty()) {
            return this
        }

        val encodedName = encode(name, UriComponent.Type.QUERY_PARAM)
        if (queryParams == null) {
            for (value in values) {
                if (query.length > 0) {
                    query.append('&')
                }
                query.append(encodedName)
                query.append('=').append(encode(value.toString(), UriComponent.Type.QUERY_PARAM))
            }
        } else {
            for (value in values) {
                queryParams!!.put(encodedName, encode(value.toString(), UriComponent.Type.QUERY_PARAM))
            }
        }
        return this
    }

    fun replaceQueryParam(name: String, vararg values: Any): JerseyUriBuilder {
        checkSsp()

        if (queryParams == null) {
            queryParams = UriComponent.decodeQuery(query.toString(), false, false)
            query.setLength(0)
        }

        val encodedName = encode(name, UriComponent.Type.QUERY_PARAM)
        queryParams!!.removeAll(encodedName)

        for (value in values) {
            queryParams!!.put(encodedName, encode(value.toString(), UriComponent.Type.QUERY_PARAM))
        }
        return this
    }

    @Throws(IllegalArgumentException::class)
    fun resolveTemplate(name: String, value: Any): JerseyUriBuilder {
        resolveTemplate(name, value, true, true)

        return this
    }

    fun resolveTemplate(name: String, value: Any, encodeSlashInPath: Boolean): JerseyUriBuilder {
        resolveTemplate(name, value, true, encodeSlashInPath)
        return this
    }

    fun resolveTemplateFromEncoded(name: String, value: Any): JerseyUriBuilder {
        resolveTemplate(name, value, false, false)
        return this
    }

    private fun resolveTemplate(
        name: String,
        value: Any,
        encode: Boolean,
        encodeSlashInPath: Boolean
    ): JerseyUriBuilder {

        val templateValues = HashMap<String, Any>()
        templateValues[name] = value
        resolveTemplates(templateValues, encode, encodeSlashInPath)
        return this
    }

    @Throws(IllegalArgumentException::class)
    fun resolveTemplates(templateValues: Map<String, Any>): JerseyUriBuilder {
        resolveTemplates(templateValues, true, true)
        return this
    }

    @Throws(IllegalArgumentException::class)
    fun resolveTemplates(templateValues: Map<String, Any>, encodeSlashInPath: Boolean): JerseyUriBuilder {
        resolveTemplates(templateValues, true, encodeSlashInPath)
        return this
    }

    fun resolveTemplatesFromEncoded(templateValues: Map<String, Any>): JerseyUriBuilder {
        resolveTemplates(templateValues, false, false)
        return this
    }

    private fun resolveTemplates(
        templateValues: Map<String, Any>,
        encode: Boolean,
        encodeSlashInPath: Boolean
    ): JerseyUriBuilder {
        scheme = UriTemplate.resolveTemplateValues(UriComponent.Type.SCHEME, scheme, false, templateValues)
        userInfo = UriTemplate.resolveTemplateValues(UriComponent.Type.USER_INFO, userInfo, encode, templateValues)
        host = UriTemplate.resolveTemplateValues(UriComponent.Type.HOST, host, encode, templateValues)
        port = UriTemplate.resolveTemplateValues(UriComponent.Type.PORT, port, false, templateValues)
        authority = UriTemplate.resolveTemplateValues(UriComponent.Type.AUTHORITY, authority, encode, templateValues)

        // path template values are treated as path segments unless encodeSlashInPath is false.
        val pathComponent = if (encodeSlashInPath) UriComponent.Type.PATH_SEGMENT else UriComponent.Type.PATH
        val newPath = UriTemplate.resolveTemplateValues(pathComponent, path.toString(), encode, templateValues)
        path.setLength(0)
        path.append(newPath)

        val newQuery = UriTemplate.resolveTemplateValues(
            UriComponent.Type.QUERY_PARAM,
            query.toString(),
            encode,
            templateValues
        )
        query.setLength(0)
        query.append(newQuery)

        fragment = UriTemplate.resolveTemplateValues(UriComponent.Type.FRAGMENT, fragment, encode, templateValues)

        return this
    }

    fun fragment(fragment: String?): JerseyUriBuilder {
        this.fragment = if (fragment != null)
            encode(fragment, UriComponent.Type.FRAGMENT)
        else
            null
        return this
    }

    private fun checkSsp() {
        if (ssp != null) {
            throw IllegalArgumentException("Schema specific part is opaque.")
        }
    }

    private fun appendPath(path: String) {
        appendPath(path, false)
    }

    private fun appendPath(segments: String, isSegment: Boolean) {
        if (segments.isEmpty()) {
            return
        }

        // Encode matrix parameters on current path segment
        encodeMatrix()

        var encodedSegments = encode(
            segments,
            if (isSegment) UriComponent.Type.PATH_SEGMENT else UriComponent.Type.PATH
        )

        val pathEndsInSlash = path.length > 0 && path[path.length - 1] == '/'
        val segmentStartsWithSlash = encodedSegments[0] == '/'

        if (path.isNotEmpty() && !pathEndsInSlash && !segmentStartsWithSlash) {
            path.append('/')
        } else if (pathEndsInSlash && segmentStartsWithSlash) {
            encodedSegments = encodedSegments.substring(1)
            if (encodedSegments.isEmpty()) {
                return
            }
        }

        path.append(encodedSegments)
    }

    private fun encodeMatrix() {
        if (matrixParams == null || matrixParams!!.isEmpty) {
            return
        }

        for ((name, value1) in matrixParams!!.asMap()) {

            for (value in value1) {
                path.append(';').append(name)
                if (!value.isEmpty()) {
                    path.append('=').append(value)
                }
            }
        }
        matrixParams = null
    }

    private fun encodeQuery() {
        if (queryParams == null || queryParams!!.isEmpty) {
            return
        }

        for ((name, value1) in queryParams!!.asMap()) {

            for (value in value1) {
                if (query.length > 0) {
                    query.append('&')
                }
                query.append(name).append('=').append(value)
            }
        }
        queryParams = null
    }

    private fun encode(s: String, type: UriComponent.Type): String {
        return UriComponent.contextualEncode(s, type, true)
    }

    fun buildFromMap(values: Map<String, *>): URI {
        return xBuildFromMap(true, true, values)
    }

    fun buildFromMap(values: Map<String, *>, encodeSlashInPath: Boolean): URI {
        return xBuildFromMap(true, encodeSlashInPath, values)
    }

    @Throws(IllegalArgumentException::class)
    fun buildFromEncodedMap(values: Map<String, *>): URI {
        return xBuildFromMap(false, false, values)
    }

    private fun xBuildFromMap(encode: Boolean, encodeSlashInPath: Boolean, values: Map<String, *>): URI {
        if (ssp != null) {
            throw IllegalArgumentException("Schema specific part is opaque.")
        }

        encodeMatrix()
        encodeQuery()

        val uri = UriTemplate.createURI(
            scheme = scheme,
            authority = authority,
            userInfo = userInfo,
            host = host,
            port = port,
            path = path.toString(),
            query = query.toString(),
            fragment = fragment,
            values = values,
            encode = encode,
            encodeSlashInPath = encodeSlashInPath
        )
        return createURI(uri)
    }

    fun build(vararg values: Any): URI {
        return xBuild(true, true, *values)
    }

    fun build(values: Array<Any>, encodeSlashInPath: Boolean): URI {
        return xBuild(true, encodeSlashInPath, *values)
    }

    fun buildFromEncoded(vararg values: Any): URI {
        return xBuild(false, false, *values)
    }

    fun toTemplate(): String {
        encodeMatrix()
        encodeQuery()

        val sb = StringBuilder()

        if (scheme != null) {
            sb.append(scheme).append(':')
        }

        if (ssp != null) {
            sb.append(ssp)
        } else {
            var hasAuthority = false
            if (userInfo != null || host != null || port != null) {
                hasAuthority = true
                sb.append("//")

                if (userInfo != null && !userInfo!!.isEmpty()) {
                    sb.append(userInfo).append('@')
                }

                if (host != null) {
                    // TODO check IPv6 address
                    sb.append(host)
                }

                if (port != null) {
                    sb.append(':').append(port)
                }
            } else if (authority != null) {
                hasAuthority = true
                sb.append("//").append(authority)
            }

            if (path.length > 0) {
                if (hasAuthority && path[0] != '/') {
                    sb.append("/")
                }
                sb.append(path)
            } else if (hasAuthority && (query.length > 0 || fragment != null && !fragment!!.isEmpty())) {
                // if has authority and query or fragment and no path value, we need to append root '/' to the path
                // see URI RFC 3986 section 3.3
                sb.append("/")
            }

            if (query.length > 0) {
                sb.append('?').append(query)
            }
        }

        if (fragment != null && !fragment!!.isEmpty()) {
            sb.append('#').append(fragment)
        }

        return sb.toString()
    }

    private fun xBuild(encode: Boolean, encodeSlashInPath: Boolean, vararg values: Any): URI {
        if (ssp != null) {
            if (values.isEmpty()) {
                return createURI(create())
            }
            throw IllegalArgumentException("Schema specific part is opaque.")
        }

        encodeMatrix()
        encodeQuery()

        val uri = UriTemplate.createURI(
            scheme,
            authority,
            userInfo,
            host,
            port,
            path.toString(),
            query.toString(),
            fragment,
            values.toList().toTypedArray(),
            encode,
            encodeSlashInPath
        )
        return createURI(uri)
    }

    private fun create(): String {
        return UriComponent.encodeTemplateNames(toTemplate())
    }

    private fun createURI(uri: String): URI {
        try {
            return URI(uri)
        } catch (ex: URISyntaxException) {
            throw IllegalStateException(ex)
        }
    }

    override fun toString(): String {
        return toTemplate()
    }
}
