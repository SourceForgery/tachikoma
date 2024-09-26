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

import java.net.URI
import java.util.ArrayDeque
import java.util.Collections
import java.util.Comparator
import java.util.HashMap
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

/**
 * A URI template.
 *
 * @author Paul Sandoz
 * @author Martin Matula
 * @author Gerard Davison (gerard.davison at oracle.com)
 */
class UriTemplate {
    /**
     * The URI template.
     */
    val template: String?

    /**
     * The normalized URI template. Any explicit regex are removed to leave
     * the template variables.
     */
    private val normalizedTemplate: String

    /**
     * The pattern generated from the template.
     */
    val pattern: PatternWithGroups

    /**
     * True if the URI template ends in a '/' character.
     */
    private val endsWithSlash: Boolean

    /**
     * The template variables in the URI template.
     */
    val templateVariables: List<String>

    /**
     * The number of explicit regular expressions declared for template
     * variables.
     */
    val numberOfExplicitRegexes: Int

    /**
     * The number of regular expression groups in this pattern.
     */
    val numberOfRegexGroups: Int

    /**
     * The number of characters in the regular expression not resulting
     * from conversion of template variables.
     */
    val numberOfExplicitCharacters: Int

    /**
     * Get the number of template variables.
     *
     * @return the number of template variables.
     */
    val numberOfTemplateVariables: Int
        get() = templateVariables.size

    /**
     * A strategy interface for processing parameters, should be replaced with
     * a JDK 8 one day in the future.
     */
    private interface TemplateValueStrategy {
        /**
         * Get a value for a given template variable.
         *
         * @param templateVariable template variable.
         * @param matchedGroup matched group string for a given template variable.
         * @return template value.
         *
         * @throws java.lang.IllegalArgumentException in case no value has been found and the strategy
         * does not support `null` values.
         */
        fun valueFor(
            templateVariable: String,
            matchedGroup: String,
        ): String?
    }

    /**
     * Constructor for `NULL` template.
     */
    private constructor() {
        this.normalizedTemplate = ""
        this.template = this.normalizedTemplate
        this.pattern = PatternWithGroups.EMPTY
        this.endsWithSlash = false
        this.templateVariables = emptyList()
        this.numberOfRegexGroups = 0
        this.numberOfExplicitCharacters = this.numberOfRegexGroups
        this.numberOfExplicitRegexes = this.numberOfExplicitCharacters
    }

    /**
     * Construct a new URI template.
     *
     *
     * The template will be parsed to extract template variables.
     *
     *
     *
     * A specific regular expression will be generated from the template
     * to match URIs according to the template and map template variables to
     * template values.
     *
     *
     * @param template the template.
     * @throws PatternSyntaxException if the specified
     * regular expression could not be generated
     * @throws IllegalArgumentException if the template is `null` or
     * an empty string.
     */
    @Throws(PatternSyntaxException::class, IllegalArgumentException::class)
    constructor(template: String) : this(UriTemplateParser(template)) {
    }

    /**
     * Construct a new URI template.
     *
     *
     * The template will be parsed to extract template variables.
     *
     *
     * A specific regular expression will be generated from the template
     * to match URIs according to the template and map template variables to
     * template values.
     *
     *
     *
     * @param templateParser the parser to parse the template.
     * @throws PatternSyntaxException if the specified
     * regular expression could not be generated
     * @throws IllegalArgumentException if the template is `null` or
     * an empty string.
     */
    @Throws(PatternSyntaxException::class, IllegalArgumentException::class)
    protected constructor(templateParser: UriTemplateParser) {
        this.template = templateParser.template

        this.normalizedTemplate = templateParser.getNormalizedTemplate()

        this.pattern = initUriPattern(templateParser)

        this.numberOfExplicitRegexes = templateParser.numberOfExplicitRegexes

        this.numberOfRegexGroups = templateParser.numberOfRegexGroups

        this.numberOfExplicitCharacters = templateParser.numberOfLiteralCharacters

        this.endsWithSlash = template!![template.length - 1] == '/'

        this.templateVariables = Collections.unmodifiableList(templateParser.getNames())
    }

    /**
     * Check if the URI template ends in a slash (`'/'`).
     *
     * @return `true` if the template ends in a '/', otherwise false.
     */
    fun endsWithSlash(): Boolean {
        return endsWithSlash
    }

    /**
     * Ascertain if a template variable is a member of this
     * template.
     *
     * @param name name The template variable.
     * @return `true` if the template variable is a member of the template, otherwise `false`.
     */
    fun isTemplateVariablePresent(name: String): Boolean {
        for (s in templateVariables) {
            if (s == name) {
                return true
            }
        }

        return false
    }

    /**
     * Match a URI against the template.
     *
     *
     * If the URI matches against the pattern then the template variable to value
     * map will be filled with template variables as keys and template values as
     * values.
     *
     *
     *
     * @param uri the uri to match against the template.
     * @param templateVariableToValue the map where to put template variables (as keys)
     * and template values (as values). The map is cleared before any
     * entries are put.
     * @return true if the URI matches the template, otherwise false.
     *
     * @throws IllegalArgumentException if the uri or
     * templateVariableToValue is null.
     */
    @Throws(IllegalArgumentException::class)
    fun match(
        uri: CharSequence,
        templateVariableToValue: MutableMap<String, String>?,
    ): Boolean {
        if (templateVariableToValue == null) {
            throw IllegalArgumentException()
        }

        return pattern.match(uri, templateVariables, templateVariableToValue)
    }

    /**
     * Match a URI against the template.
     *
     *
     * If the URI matches against the pattern the capturing group values (if any)
     * will be added to a list passed in as parameter.
     *
     *
     *
     * @param uri the uri to match against the template.
     * @param groupValues the list to store the values of a pattern's
     * capturing groups is matching is successful. The values are stored
     * in the same order as the pattern's capturing groups.
     * @return true if the URI matches the template, otherwise false.
     *
     * @throws IllegalArgumentException if the uri or
     * templateVariableToValue is null.
     */
    @Throws(IllegalArgumentException::class)
    fun match(
        uri: CharSequence,
        groupValues: MutableList<String>?,
    ): Boolean {
        if (groupValues == null) {
            throw IllegalArgumentException()
        }

        return pattern.match(uri, groupValues)
    }

    /**
     * Create a URI by substituting any template variables
     * for corresponding template values.
     *
     *
     * A URI template variable without a value will be substituted by the
     * empty string.
     *
     * @param values the map of template variables to template values.
     * @return the URI.
     */
    fun createURI(values: Map<String, String>): String {
        val sb = StringBuilder()
        resolveTemplate(
            normalizedTemplate,
            sb,
            object : TemplateValueStrategy {
                override fun valueFor(
                    templateVariable: String,
                    matchedGroup: String,
                ): String? {
                    return values[templateVariable]
                }
            },
        )
        return sb.toString()
    }

    /**
     * Create a URI by substituting any template variables
     * for corresponding template values.
     *
     *
     * A URI template variable without a value will be substituted by the
     * empty string.
     *
     * @param values the array of template values. The values will be
     * substituted in order of occurrence of unique template variables.
     * @return the URI.
     */
    fun createURI(vararg values: String): String {
        return createURI(values, 0, values.size)
    }

    /**
     * Create a URI by substituting any template variables
     * for corresponding template values.
     *
     *
     * A URI template variable without a value will be substituted by the
     * empty string.
     *
     * @param values the array of template values. The values will be
     * substituted in order of occurrence of unique template variables.
     * @param offset the offset into the template value array.
     * @param length the length of the template value array.
     * @return the URI.
     */
    fun createURI(
        values: Array<out String>,
        offset: Int,
        length: Int,
    ): String {
        val ns =
            object : TemplateValueStrategy {
                private val lengthPlusOffset = length + offset
                private var v = offset
                private val mapValues = HashMap<String, String>()

                override fun valueFor(
                    templateVariable: String,
                    matchedGroup: String,
                ): String? {
                    // Check if a template variable has already occurred
                    // If so use the value to ensure that two or more declarations of
                    // a template variable have the same value
                    var tValue: String? = mapValues[templateVariable]
                    if (tValue == null) {
                        if (v < lengthPlusOffset) {
                            tValue = values[v++]
                            mapValues[templateVariable] = tValue
                        }
                    }

                    return tValue
                }
            }

        val sb = StringBuilder()
        resolveTemplate(normalizedTemplate, sb, ns)
        return sb.toString()
    }

    override fun toString(): String {
        return pattern.toString()
    }

    /**
     * Hash code is calculated from String of the regular expression
     * generated from the template.
     *
     * @return the hash code.
     */
    override fun hashCode(): Int {
        return pattern.hashCode()
    }

    /**
     * Equality is calculated from the String of the regular expression
     * generated from the templates.
     *
     * @param other the reference object with which to compare.
     * @return true if equals, otherwise false.
     */
    override fun equals(other: Any?): Boolean {
        if (other is UriTemplate) {
            val that = other as UriTemplate?
            return this.pattern == that!!.pattern
        } else {
            return false
        }
    }

    companion object {
        private val EMPTY_VALUES = arrayOf<String>()

        /**
         * Order the templates according to JAX-RS specification.
         *
         *
         * Sort the set of matching resource classes using the number of
         * characters in the regular expression not resulting from template
         * variables as the primary key, the number of matching groups
         * as a secondary key, and the number of explicit regular expression
         * declarations as the tertiary key.
         *
         */
        val COMPARATOR: Comparator<UriTemplate> =
            Comparator { o1, o2 ->
                if (o1 == null && o2 == null) {
                    return@Comparator 0
                }
                if (o1 == null) {
                    return@Comparator 1
                }
                if (o2 == null) {
                    return@Comparator -1
                }

                if (o1 === EMPTY && o2 === EMPTY) {
                    return@Comparator 0
                }
                if (o1 === EMPTY) {
                    return@Comparator 1
                }
                if (o2 === EMPTY) {
                    return@Comparator -1
                }

                // Compare the number of explicit characters
                // Note that it is important that o2 is compared against o1
                // so that a regular expression with say 10 explicit characters
                // is less than a regular expression with say 5 explicit characters.
                var i = o2.numberOfExplicitCharacters - o1.numberOfExplicitCharacters
                if (i != 0) {
                    return@Comparator i
                }

                // If the number of explicit characters is equal
                // compare the number of template variables
                // Note that it is important that o2 is compared against o1
                // so that a regular expression with say 10 template variables
                // is less than a regular expression with say 5 template variables.
                i = o2.numberOfTemplateVariables - o1.numberOfTemplateVariables
                if (i != 0) {
                    return@Comparator i
                }

                // If the number of template variables is equal
                // compare the number of explicit regexes
                i = o2.numberOfExplicitRegexes - o1.numberOfExplicitRegexes
                if (i != 0) {
                    i
                } else {
                    o2.pattern.regex.compareTo(o1.pattern.regex)
                }

                // If the number of explicit characters and template variables
                // are equal then comapre the regexes
                // The order does not matter as long as templates with different
                // explicit characters are distinguishable
            }

        /**
         * The regular expression for matching URI templates and names.
         */
        private val TEMPLATE_NAMES_PATTERN = Pattern.compile("\\{([\\w?;][-\\w.,]*)}")

        /**
         * The empty URI template that matches the `null` or empty URI path.
         */
        val EMPTY = UriTemplate()

        /**
         * Create the URI pattern from a URI template parser.
         *
         * @param templateParser the URI template parser.
         * @return the URI pattern.
         */
        private fun initUriPattern(templateParser: UriTemplateParser): PatternWithGroups {
            return PatternWithGroups(templateParser.pattern, templateParser.groupIndexes)
        }

        /**
         * Resolve a relative URI reference against a base URI as defined in
         * [RFC 3986](http://tools.ietf.org/html/rfc3986#section-5.4).
         *
         * @param baseUri base URI to be used for resolution.
         * @param refUri reference URI string to be resolved against the base URI.
         * @return resolved URI.
         *
         * @throws IllegalArgumentException If the given string violates the URI specification RFC.
         */
        fun resolve(
            baseUri: URI,
            refUri: String,
        ): URI {
            return resolve(baseUri, URI.create(refUri))
        }

        /**
         * Resolve a relative URI reference against a base URI as defined in
         * [RFC 3986](http://tools.ietf.org/html/rfc3986#section-5.4).
         *
         * @param baseUri base URI to be used for resolution.
         * @param refUri reference URI to be resolved against the base URI.
         * @return resolved URI.
         */
        fun resolve(
            baseUri: URI,
            refUri: URI,
        ): URI {
            val refString = refUri.toString()
            val nonEmptyRefUri =
                if (refString.isEmpty()) {
                    // we need something to resolve against
                    URI.create("#")
                } else if (refString.startsWith("?")) {
                    var baseString = baseUri.toString()
                    val qIndex = baseString.indexOf('?')
                    baseString = if (qIndex > -1) baseString.substring(0, qIndex) else baseString
                    return URI.create(baseString + refString)
                } else {
                    refUri
                }

            var result = baseUri.resolve(nonEmptyRefUri)
            if (refString.isEmpty()) {
                val resolvedString = result.toString()
                result = URI.create(resolvedString.substring(0, resolvedString.indexOf('#')))
            }

            return normalize(result)
        }

        /**
         * Normalize the URI by resolve the dot & dot-dot path segments as described in
         * [RFC 3986](http://tools.ietf.org/html/rfc3986#section-5.2.4).
         *
         * This method provides a workaround for issues with [java.net.URI.normalize] which
         * is not able to properly normalize absolute paths that start with a `".."` segment,
         * e.g. `"/../a/b"` as required by RFC 3986 (according to RFC 3986 the path `"/../a/b"`
         * should resolve to `"/a/b"`, while `URI.normalize()` keeps the `".."` segment
         * in the URI path.
         *
         * @param uri the original URI string.
         * @return the URI with dot and dot-dot segments resolved.
         *
         * @throws IllegalArgumentException If the given string violates the URI specification RFC.
         * @see java.net.URI.normalize
         */
        fun normalize(uri: String): URI {
            return normalize(URI.create(uri))
        }

        /**
         * Normalize the URI by resolve the dot & dot-dot path segments as described in
         * [RFC 3986](http://tools.ietf.org/html/rfc3986#section-5.2.4).
         *
         * This method provides a workaround for issues with [java.net.URI.normalize] which
         * is not able to properly normalize absolute paths that start with a `".."` segment,
         * e.g. `"/../a/b"` as required by RFC 3986 (according to RFC 3986 the path `"/../a/b"`
         * should resolve to `"/a/b"`, while `URI.normalize()` keeps the `".."` segment
         * in the URI path.
         *
         * @param uri the original URI.
         * @return the URI with dot and dot-dot segments resolved.
         *
         * @see java.net.URI.normalize
         */
        fun normalize(uri: URI): URI {
            val path = uri.path

            if (path == null || path.isEmpty() || !path.contains("/.")) {
                return uri
            }

            val segments = path.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val resolvedSegments = ArrayDeque<String>(segments.size)

            for (segment in segments) {
                if (segment.isEmpty() || "." == segment) {
                    // skip
                } else if (".." == segment) {
                    resolvedSegments.pollLast()
                } else {
                    resolvedSegments.offer(segment)
                }
            }

            val pathBuilder = StringBuilder()
            for (segment in resolvedSegments) {
                pathBuilder.append('/').append(segment)
            }

            val resultString =
                createURIWithStringValues(
                    scheme = uri.scheme,
                    authority = uri.authority, userInfo = null, host = null, port = null,
                    path = pathBuilder.toString(),
                    query = uri.query,
                    fragment = uri.fragment,
                    values = EMPTY_VALUES,
                    encode = false,
                    encodeSlashInPath = false,
                )

            return URI.create(resultString)
        }

        /**
         * Relativize URI with respect to a base URI.
         *
         * After the relativization is done, dots in paths of both URIs are [resolved][normalize].
         *
         * @param baseUri base URI to be used for relativization.
         * @param refUri URI to be relativized.
         * @return relativized URI.
         */
        fun relativize(
            baseUri: URI,
            refUri: URI,
        ): URI {
            return normalize(baseUri.relativize(refUri))
        }

        /**
         * Build a URI based on the parameters provided by the variable name strategy.
         *
         * @param normalizedTemplate normalized URI template. A normalized template is a template without any explicit regular
         * expressions.
         * @param builder URI string builder to be used.
         * @param valueStrategy The template value producer strategy to use.
         */
        private fun resolveTemplate(
            normalizedTemplate: String,
            builder: StringBuilder,
            valueStrategy: TemplateValueStrategy,
        ) {
            // Find all template variables
            val m = TEMPLATE_NAMES_PATTERN.matcher(normalizedTemplate)

            var i = 0
            while (m.find()) {
                builder.append(normalizedTemplate, i, m.start())
                val variableName = m.group(1)
                // TODO matrix
                val firstChar = variableName[0]
                if (firstChar == '?' || firstChar == ';') {
                    val prefix: Char
                    val separator: Char
                    val emptyValueAssignment: String
                    if (firstChar == '?') {
                        // query
                        prefix = '?'
                        separator = '&'
                        emptyValueAssignment = "="
                    } else {
                        // matrix
                        prefix = ';'
                        separator = ';'
                        emptyValueAssignment = ""
                    }

                    val index = builder.length
                    val variables = variableName.substring(1).split(", ?".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    for (variable in variables) {
                        try {
                            val value = valueStrategy.valueFor(variable, m.group())
                            if (value != null) {
                                if (index != builder.length) {
                                    builder.append(separator)
                                }

                                builder.append(variable)
                                if (value.isEmpty()) {
                                    builder.append(emptyValueAssignment)
                                } else {
                                    builder.append('=')
                                    builder.append(value)
                                }
                            }
                        } catch (ex: IllegalArgumentException) {
                            // no value found => ignore the variable
                        }
                    }

                    if (index != builder.length && (index == 0 || builder[index - 1] != prefix)) {
                        builder.insert(index, prefix)
                    }
                } else {
                    val value = valueStrategy.valueFor(variableName, m.group())

                    if (value != null) {
                        builder.append(value)
                    }
                }

                i = m.end()
            }
            builder.append(normalizedTemplate, i, normalizedTemplate.length)
        }

        /**
         * Construct a URI from the component parts each of which may contain
         * template variables.
         *
         *
         * A template values is an Object instance MUST support the toString()
         * method to convert the template value to a String instance.
         *
         *
         * @param scheme the URI scheme component.
         * @param authority the URI authority component.
         * @param userInfo the URI user info component.
         * @param host the URI host component.
         * @param port the URI port component.
         * @param path the URI path component.
         * @param query the URI query component.
         * @param fragment the URI fragment component.
         * @param values the template variable to value map.
         * @param encode if true encode a template value according to the correspond
         * component type of the associated template variable, otherwise
         * contextually encode the template value.
         * @param encodeSlashInPath if `true`, the slash (`'/'`) characters
         * in parameter values will be encoded if the template
         * is placed in the URI path component, otherwise the slash
         * characters will not be encoded in path templates.
         * @return a URI.
         */
        fun createURI(
            scheme: String?,
            authority: String?,
            userInfo: String?,
            host: String?,
            port: String?,
            path: String?,
            query: String?,
            fragment: String?,
            values: Map<String, *>,
            encode: Boolean,
            encodeSlashInPath: Boolean,
        ): String {
            val stringValues = HashMap<String, String>()
            for ((key, value) in values) {
                if (value != null) {
                    stringValues[key] = value.toString()
                }
            }

            return createURIWithStringValues(
                scheme, authority,
                userInfo, host, port, path, query, fragment,
                stringValues, encode, encodeSlashInPath,
            )
        }

        /**
         * Construct a URI from the component parts each of which may contain
         * template variables.
         *
         *
         * A template value is an Object instance that MUST support the toString()
         * method to convert the template value to a String instance.
         *
         *
         * @param scheme the URI scheme component.
         * @param authority the URI authority info component.
         * @param userInfo the URI user info component.
         * @param host the URI host component.
         * @param port the URI port component.
         * @param path the URI path component.
         * @param query the URI query component.
         * @param fragment the URI fragment component.
         * @param values the template variable to value map.
         * @param encode if true encode a template value according to the correspond
         * component type of the associated template variable, otherwise
         * contextually encode the template value.
         * @param encodeSlashInPath if `true`, the slash (`'/'`) characters
         * in parameter values will be encoded if the template
         * is placed in the URI path component, otherwise the slash
         * characters will not be encoded in path templates.
         * @return a URI.
         */
        fun createURIWithStringValues(
            scheme: String?,
            authority: String?,
            userInfo: String?,
            host: String?,
            port: String?,
            path: String?,
            query: String?,
            fragment: String?,
            values: Map<String, Any>,
            encode: Boolean,
            encodeSlashInPath: Boolean,
        ): String {
            return createURIWithStringValues(
                scheme,
                authority,
                userInfo,
                host,
                port,
                path,
                query,
                fragment,
                EMPTY_VALUES,
                encode,
                encodeSlashInPath,
                values.toMutableMap(),
            )
        }

        /**
         * Construct a URI from the component parts each of which may contain
         * template variables.
         *
         *
         * The template values are an array of Object and each Object instance
         * MUST support the toString() method to convert the template value to
         * a String instance.
         *
         *
         * @param scheme the URI scheme component.
         * @param authority the URI authority component.
         * @param userInfo the URI user info component.
         * @param host the URI host component.
         * @param port the URI port component.
         * @param path the URI path component.
         * @param query the URI query component.
         * @param fragment the URI fragment component.
         * @param values the array of template values.
         * @param encode if true encode a template value according to the correspond
         * component type of the associated template variable, otherwise
         * contextually encode the template value.
         * @param encodeSlashInPath if `true`, the slash (`'/'`) characters
         * in parameter values will be encoded if the template
         * is placed in the URI path component, otherwise the slash
         * characters will not be encoded in path templates.
         * @return a URI.
         */
        fun createURI(
            scheme: String?,
            authority: String?,
            userInfo: String?,
            host: String?,
            port: String?,
            path: String?,
            query: String?,
            fragment: String?,
            values: Array<Any>,
            encode: Boolean,
            encodeSlashInPath: Boolean,
        ): String {
            val stringValues = values.map { it.toString() }.toTypedArray()

            return createURIWithStringValues(
                scheme,
                authority,
                userInfo,
                host,
                port,
                path,
                query,
                fragment,
                stringValues,
                encode,
                encodeSlashInPath,
            )
        }

        /**
         * Construct a URI from the component parts each of which may contain
         * template variables.
         *
         * @param scheme the URI scheme component.
         * @param authority the URI authority component.
         * @param userInfo the URI user info component.
         * @param host the URI host component.
         * @param port the URI port component.
         * @param path the URI path component.
         * @param query the URI query component.
         * @param fragment the URI fragment component.
         * @param values the array of template values.
         * @param encode if true encode a template value according to the correspond
         * component type of the associated template variable, otherwise
         * contextually encode the template value.
         * @param encodeSlashInPath if `true`, the slash (`'/'`) characters
         * in parameter values will be encoded if the template
         * is placed in the URI path component, otherwise the slash
         * characters will not be encoded in path templates.
         * @return a URI.
         */
        fun createURIWithStringValues(
            scheme: String?,
            authority: String?,
            userInfo: String?,
            host: String?,
            port: String?,
            path: String?,
            query: String?,
            fragment: String?,
            values: Array<String>,
            encode: Boolean,
            encodeSlashInPath: Boolean,
        ): String {
            val mapValues = HashMap<String, Any>()
            return createURIWithStringValues(
                scheme, authority, userInfo, host, port, path, query, fragment, values, encode, encodeSlashInPath, mapValues,
            )
        }

        private fun createURIWithStringValues(
            scheme: String?,
            authority: String?,
            userInfo: String?,
            host: String?,
            port: String?,
            path: String?,
            query: String?,
            fragment: String?,
            values: Array<String>,
            encode: Boolean,
            encodeSlashInPath: Boolean,
            mapValues: MutableMap<String, Any>,
        ): String {
            val sb = StringBuilder()
            var offset = 0

            if (scheme != null) {
                offset =
                    createUriComponent(
                        UriComponent.Type.SCHEME,
                        scheme,
                        values,
                        offset,
                        false,
                        mapValues,
                        sb,
                    )
                sb.append(':')
            }

            var hasAuthority = false
            if (notEmpty(userInfo) || notEmpty(host) || notEmpty(port)) {
                hasAuthority = true
                sb.append("//")

                if (notEmpty(userInfo)) {
                    offset =
                        createUriComponent(
                            UriComponent.Type.USER_INFO,
                            userInfo!!,
                            values,
                            offset,
                            encode,
                            mapValues,
                            sb,
                        )
                    sb.append('@')
                }

                if (notEmpty(host)) {
                    // TODO check IPv6 address
                    offset =
                        createUriComponent(
                            UriComponent.Type.HOST,
                            host!!,
                            values,
                            offset,
                            encode,
                            mapValues,
                            sb,
                        )
                }

                if (notEmpty(port)) {
                    sb.append(':')
                    offset =
                        createUriComponent(
                            UriComponent.Type.PORT,
                            port!!,
                            values,
                            offset,
                            false,
                            mapValues,
                            sb,
                        )
                }
            } else if (notEmpty(authority)) {
                hasAuthority = true
                sb.append("//")

                offset =
                    createUriComponent(
                        UriComponent.Type.AUTHORITY,
                        authority!!,
                        values,
                        offset,
                        encode,
                        mapValues,
                        sb,
                    )
            }

            if (notEmpty(path) || notEmpty(query) || notEmpty(fragment)) {
                // make sure we append at least the root path if only query or fragment is present
                if (hasAuthority && (path == null || path.isEmpty() || path[0] != '/')) {
                    sb.append('/')
                }

                if (notEmpty(path)) {
                    // path template values are treated as path segments unless encodeSlashInPath is false.
                    val t = if (encodeSlashInPath) UriComponent.Type.PATH_SEGMENT else UriComponent.Type.PATH

                    offset =
                        createUriComponent(
                            t,
                            path!!,
                            values,
                            offset,
                            encode,
                            mapValues,
                            sb,
                        )
                }

                if (notEmpty(query)) {
                    sb.append('?')
                    offset =
                        createUriComponent(
                            UriComponent.Type.QUERY_PARAM,
                            query!!,
                            values,
                            offset,
                            encode,
                            mapValues,
                            sb,
                        )
                }

                if (notEmpty(fragment)) {
                    sb.append('#')
                    createUriComponent(
                        UriComponent.Type.FRAGMENT,
                        fragment!!,
                        values,
                        offset,
                        encode,
                        mapValues,
                        sb,
                    )
                }
            }
            return sb.toString()
        }

        private fun notEmpty(string: String?): Boolean {
            return string != null && !string.isEmpty()
        }

        private fun createUriComponent(
            componentType: UriComponent.Type,
            template: String,
            values: Array<String>,
            valueOffset: Int,
            encode: Boolean,
            mapValues: MutableMap<String, Any>,
            b: StringBuilder,
        ): Int {
            var parsedTemplate = template

            if (parsedTemplate.indexOf('{') == -1) {
                b.append(parsedTemplate)
                return valueOffset
            }

            // Find all template variables
            parsedTemplate = UriTemplateParser(parsedTemplate).getNormalizedTemplate()

            class ValuesFromArrayStrategy : TemplateValueStrategy {
                var offset = valueOffset

                override fun valueFor(
                    templateVariable: String,
                    matchedGroup: String,
                ): String? {
                    var value: Any? = mapValues[templateVariable]
                    if (value == null && offset < values.size) {
                        value = values[offset++]
                        mapValues.put(templateVariable, value)
                    }
                    if (value == null) {
                        throw IllegalArgumentException(
                            String.format("The template variable '%s' has no value", templateVariable),
                        )
                    }
                    return if (encode) {
                        UriComponent.encode(value.toString(), componentType)
                    } else {
                        UriComponent.contextualEncode(value.toString(), componentType)
                    }
                }
            }

            val cs = ValuesFromArrayStrategy()
            resolveTemplate(parsedTemplate, b, cs)

            return cs.offset
        }

        /**
         * Resolves template variables in the given `template` from `_mapValues`. Resolves only these variables which are
         * defined in the `_mapValues` leaving other variables unchanged.
         *
         * @param type Type of the `template` (port, path, query, ...).
         * @param template Input uri component to resolve.
         * @param encode True if template values from `_mapValues` should be percent encoded.
         * @param mapValues Map with template variables as keys and template values as values. None of them should be null.
         * @return String with resolved template variables.
         *
         * @throws IllegalArgumentException when `_mapValues` value is null.
         */
        fun resolveTemplateValues(
            type: UriComponent.Type,
            template: String?,
            encode: Boolean,
            mapValues: Map<String, Any>,
        ): String? {
            if (template == null || template.isEmpty() || template.indexOf('{') == -1) {
                return template
            }

            // Find all template variables
            val normalizedTemplate = UriTemplateParser(template).getNormalizedTemplate()

            val sb = StringBuilder()
            resolveTemplate(
                normalizedTemplate,
                sb,
                object : TemplateValueStrategy {
                    override fun valueFor(
                        templateVariable: String,
                        matchedGroup: String,
                    ): String? {
                        var value: Any? = mapValues[templateVariable]

                        if (value != null) {
                            if (encode) {
                                value = UriComponent.encode(value.toString(), type)
                            } else {
                                value = UriComponent.contextualEncode(value.toString(), type)
                            }
                            return value.toString()
                        } else {
                            if (mapValues.containsKey(templateVariable)) {
                                throw IllegalArgumentException(
                                    String.format(
                                        "The value associated of the template value map for key '%s' is 'null'.",
                                        templateVariable,
                                    ),
                                )
                            }

                            return matchedGroup
                        }
                    }
                },
            )

            return sb.toString()
        }
    }
}
