package com.sourceforgery.tachikoma.logging

import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LoggingException
import org.apache.logging.log4j.core.Layout
import org.apache.logging.log4j.core.LogEvent
import org.apache.logging.log4j.core.appender.TlsSyslogFrame
import org.apache.logging.log4j.core.config.Configuration
import org.apache.logging.log4j.core.config.Node
import org.apache.logging.log4j.core.config.plugins.Plugin
import org.apache.logging.log4j.core.config.plugins.PluginAttribute
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration
import org.apache.logging.log4j.core.config.plugins.PluginElement
import org.apache.logging.log4j.core.config.plugins.PluginFactory
import org.apache.logging.log4j.core.layout.AbstractLayout
import org.apache.logging.log4j.core.layout.AbstractStringLayout
import org.apache.logging.log4j.core.layout.LoggerFields
import org.apache.logging.log4j.core.layout.PatternLayout
import org.apache.logging.log4j.core.net.Facility
import org.apache.logging.log4j.core.net.Priority
import org.apache.logging.log4j.core.pattern.LogEventPatternConverter
import org.apache.logging.log4j.core.pattern.PatternConverter
import org.apache.logging.log4j.core.pattern.PatternFormatter
import org.apache.logging.log4j.core.pattern.PatternParser
import org.apache.logging.log4j.core.pattern.ThrowablePatternConverter
import org.apache.logging.log4j.core.util.NetUtils
import org.apache.logging.log4j.core.util.Patterns
import org.apache.logging.log4j.message.Message
import org.apache.logging.log4j.message.MessageCollectionMessage
import org.apache.logging.log4j.message.StructuredDataCollectionMessage
import org.apache.logging.log4j.message.StructuredDataId
import org.apache.logging.log4j.message.StructuredDataMessage
import org.apache.logging.log4j.util.ProcessIdUtil
import org.apache.logging.log4j.util.StringBuilders
import org.apache.logging.log4j.util.Strings
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Calendar
import java.util.HashMap
import java.util.TreeMap
import java.util.regex.Matcher

@Suppress("unused")
@Plugin(name = "Rfc5424PatternLayout", category = Node.CATEGORY, elementType = Layout.ELEMENT_TYPE, printObject = true)
class Rfc5424PatternLayout
private constructor(
        config: Configuration?,
        private val facility: Facility,
        id: String?,
        private val enterpriseNumber: Int,
        private val includeMdc: Boolean,
        private val includeNewLine: Boolean,
        escapeNL: String?,
        mdcId: String,
        private val mdcPrefix: String?,
        private val eventPrefix: String?,
        private val appName: String?,
        private val messageId: String?,
        excludes: String?,
        includes: String?,
        required: String?,
        charset: Charset,
        exceptionPattern: String?,
        private val useTlsMessageFormat: Boolean,
        loggerFields: Array<LoggerFields>?,
        pattern: String
) : AbstractStringLayout(charset) {
    private val defaultId: String?
    private val mdcId: String?
    private val mdcSdId: StructuredDataId
    private val localHostName: String
    private var configName: String?
    private var mdcExcludes: List<String>?
    private var mdcIncludes: List<String>?
    private var mdcRequired: List<String>?
    private val listChecker: ListChecker?
    private val noopChecker = NoopChecker()
    private val escapeNewLine: String?

    private var lastTimestamp: Long = -1
    private var timestampStr: String? = null

    private var exceptionFormatters: List<PatternFormatter>?
    private val fieldFormatters: Map<String, FieldFormatter>?
    private val procId: String
    private val pattern: List<PatternFormatter>

    init {
        val exceptionParser = createPatternParser(config, ThrowablePatternConverter::class.java)
        exceptionFormatters = exceptionPattern
                ?.let {
                    exceptionParser.parse(exceptionPattern)
                }
        this.defaultId = id ?: DEFAULT_ID
        this.escapeNewLine = escapeNL
                ?.let {
                    Matcher.quoteReplacement(escapeNL)
                }
        this.mdcId = id ?: DEFAULT_MDCID
        this.mdcSdId = StructuredDataId(mdcId, enterpriseNumber, null, null)
        this.localHostName = NetUtils.getLocalHostname()
        var c: ListChecker? = null
        mdcExcludes = toTrimmedArray(excludes)
                ?.also {
                    c = ExcludeChecker()
                }
        mdcIncludes = toTrimmedArray(includes)
                ?.also {
                    c = IncludeChecker()
                }
        mdcRequired = toTrimmedArray(required)
        this.listChecker = c ?: noopChecker
        configName = config
                ?.name
                ?.let {
                    if (it.isNotEmpty()) {
                        it
                    } else {
                        null
                    }
                }
        this.fieldFormatters = createFieldFormatters(loggerFields, config)
        this.procId = ProcessIdUtil.getProcessId()
        this.pattern = createPatternParser(config, null)
                .parse(pattern)
    }

    private fun toTrimmedArray(str: String?): List<String>? {
        if (str == null) {
            return null
        }
        val list = str.split(Patterns.COMMA_SEPARATOR.toRegex()).dropLastWhile { it.isEmpty() }
        return if (list.isNotEmpty()) {
            list.map { str.trim(' ') }
        } else {
            null
        }
    }

    private fun createFieldFormatters(loggerFields: Array<LoggerFields>?,
                                      config: Configuration?): Map<String, FieldFormatter>? {
        val sdIdMap = HashMap<String, FieldFormatter>(loggerFields?.size ?: 0)
        if (loggerFields != null) {
            for (loggerField in loggerFields) {
                val key = if (loggerField.sdId == null) mdcSdId else loggerField.sdId
                val sdParams = HashMap<String, List<PatternFormatter>>()
                val fields = loggerField.map
                if (!fields.isEmpty()) {
                    val fieldParser = createPatternParser(config, null)

                    for ((key1, value) in fields) {
                        val formatters = fieldParser.parse(value)
                        sdParams[key1] = formatters
                    }
                    val fieldFormatter = FieldFormatter(sdParams,
                            loggerField.discardIfAllFieldsAreEmpty)
                    sdIdMap[key.toString()] = fieldFormatter
                }
            }
        }
        return if (sdIdMap.size > 0) sdIdMap else null
    }

    /**
     * Create a PatternParser.
     *
     * @param config The Configuration.
     * @param filterClass Filter the returned plugins after calling the plugin manager.
     * @return The PatternParser.
     */
    private fun createPatternParser(config: Configuration?,
                                    filterClass: Class<out PatternConverter>?): PatternParser {
        if (config == null) {
            return PatternParser(config, PatternLayout.KEY, LogEventPatternConverter::class.java, filterClass)
        }
        var parser: PatternParser? = config.getComponent<PatternParser>(COMPONENT_KEY)
        if (parser == null) {
            parser = PatternParser(config, PatternLayout.KEY, ThrowablePatternConverter::class.java)
            config.addComponent(COMPONENT_KEY, parser)
            parser = config.getComponent<PatternParser>(COMPONENT_KEY)!!
        }
        return parser
    }

    /**
     * Gets this Rfc5424Layout's content format. Specified by:
     *
     *  * Key: "structured" Value: "true"
     *  * Key: "format" Value: "RFC5424"
     *
     *
     * @return Map of content format keys supporting Rfc5424Layout
     */
    override fun getContentFormat(): Map<String, String> {
        val result = HashMap<String, String>()
        result["structured"] = "true"
        result["formatType"] = "RFC5424"
        return result
    }

    /**
     * Formats a org.apache.logging.log4j.core.LogEvent in conformance with the RFC 5424 Syslog specification.
     *
     * @param event The LogEvent.
     * @return The RFC 5424 String representation of the LogEvent.
     */
    override fun toSerializable(event: LogEvent): String {
        val buf = AbstractStringLayout.getStringBuilder()
        addHeader(event, buf)
        appendStructuredElements(buf, event)
        appendMessage(buf, event)
        return if (useTlsMessageFormat) {
            TlsSyslogFrame(buf.toString()).toString()
        } else buf.toString()
    }

    private fun addHeader(event: LogEvent, buf: StringBuilder) {
        appendPriority(buf, event.level)
        appendTimestamp(buf, event.timeMillis)
        appendSpace(buf)
        appendHostName(buf)
        appendSpace(buf)
        appendAppName(buf)
        appendSpace(buf)
        appendProcessId(buf)
        appendSpace(buf)
        appendMessageId(buf, event.message)
        appendSpace(buf)
    }

    private fun appendPriority(buffer: StringBuilder, logLevel: Level) {
        buffer.append('<')
        buffer.append(Priority.getPriority(facility, logLevel))
        buffer.append(">1 ")
    }

    private fun appendTimestamp(buffer: StringBuilder, milliseconds: Long) {
        buffer.append(computeTimeStampString(milliseconds))
    }

    private fun appendSpace(buffer: StringBuilder) {
        buffer.append(' ')
    }

    private fun appendHostName(buffer: StringBuilder) {
        buffer.append(localHostName)
    }

    private fun appendAppName(buffer: StringBuilder) {
        if (appName != null) {
            buffer.append(appName)
        } else if (configName != null) {
            buffer.append(configName)
        } else {
            buffer.append('-')
        }
    }

    private fun appendProcessId(buffer: StringBuilder) {
        buffer.append(getProcId())
    }

    private fun appendMessageId(buffer: StringBuilder, message: Message) {
        val isStructured = message is StructuredDataMessage
        val type = if (isStructured) (message as StructuredDataMessage).type else null
        if (type != null) {
            buffer.append(type)
        } else if (messageId != null) {
            buffer.append(messageId)
        } else {
            buffer.append('-')
        }
    }

    private fun appendMessage(buffer: StringBuilder, event: LogEvent) {
        val message = event.message
        // This layout formats StructuredDataMessages instead of delegating to the Message itself.
        val text = if (message is StructuredDataMessage || message is MessageCollectionMessage<*>) {
            message.format
        } else {
            val sb = StringBuilder()
            for (formatter in pattern) {
                formatter.format(event, sb)
            }
            sb.toString()
        }

        if (text != null && text.isNotEmpty()) {
            buffer.append(' ').append(escapeNewlines(text, escapeNewLine))
        }

        if (includeNewLine) {
            buffer.append(LF)
        }

        if (exceptionFormatters != null && event.thrown != null) {
            val exception = StringBuilder(LF)
            for (formatter in exceptionFormatters!!) {
                formatter.format(event, exception)
            }
            escapeException(event, exception.toString(), buffer)
        }
    }

    private fun appendStructuredElements(buffer: StringBuilder, event: LogEvent) {
        val message = event.message
        val isStructured = message is StructuredDataMessage || message is StructuredDataCollectionMessage

        if (!isStructured && fieldFormatters != null && fieldFormatters.isEmpty() && !includeMdc) {
            buffer.append('-')
            return
        }

        val sdElements = HashMap<String, StructuredDataElement>()
        val contextMap = event.contextData.toMap()

        if (mdcRequired != null) {
            checkRequired(contextMap)
        }

        if (fieldFormatters != null) {
            for ((sdId, value) in fieldFormatters) {
                val elem = value.format(event)
                sdElements[sdId] = elem
            }
        }

        if (includeMdc && contextMap.size > 0) {
            val mdcSdIdStr = mdcSdId.toString()
            val union = sdElements[mdcSdIdStr]
            if (union != null) {
                union.union(contextMap)
                sdElements[mdcSdIdStr] = union
            } else {
                val formattedContextMap = StructuredDataElement(contextMap, mdcPrefix, false)
                sdElements[mdcSdIdStr] = formattedContextMap
            }
        }

        if (isStructured) {
            if (message is MessageCollectionMessage<*>) {
                for (data in message as StructuredDataCollectionMessage) {
                    addStructuredData(sdElements, data)
                }
            } else {
                addStructuredData(sdElements, message as StructuredDataMessage)
            }
        }

        if (sdElements.isEmpty()) {
            buffer.append('-')
            return
        }

        for ((key, value) in sdElements) {
            formatStructuredElement(key, value, buffer, listChecker)
        }
    }

    private fun addStructuredData(sdElements: MutableMap<String, StructuredDataElement>, data: StructuredDataMessage) {
        val map = data.data
        val id = data.id
        val sdId = getId(id)

        if (sdElements.containsKey(sdId)) {
            val union = sdElements[id.toString()]!!
            union.union(map)
            sdElements[sdId] = union
        } else {
            val formattedData = StructuredDataElement(map, eventPrefix, false)
            sdElements[sdId] = formattedData
        }
    }

    private fun escapeNewlines(text: String, replacement: String?): String {
        return if (null == replacement) {
            text
        } else NEWLINE_PATTERN.replace(text, replacement)
    }

    private fun escapeException(event: LogEvent, text: String, buffer: StringBuilder) {
        val addHeader = StringBuilder().let {
            addHeader(event, it)
            it.toString()
        }
        for (item in NEWLINE_PATTERN.split(text.trim())) {
            buffer.append("$addHeader-  ${item.replace("\t", "    ")}$LF")
        }
    }

    protected fun getProcId(): String {
        return procId
    }

    protected fun getMdcExcludes(): List<String>? {
        return mdcExcludes
    }

    protected fun getMdcIncludes(): List<String>? {
        return mdcIncludes
    }

    private fun computeTimeStampString(now: Long): String? {
        val last: Long =
                synchronized(this) {
                    if (now == lastTimestamp) {
                        return timestampStr
                    }
                    lastTimestamp
                }
        val dt = ZonedDateTime
                .ofInstant(
                        Instant.ofEpochMilli(now),
                        ZoneId.systemDefault()
                )!!

        val dateString = "${dt.toLocalDateTime()}${dt.offset}"
        synchronized(this) {
            if (last == lastTimestamp) {
                lastTimestamp = now
                timestampStr = dateString
            }
        }
        return dateString
    }

    private fun pad(`val`: Int, max: Int, buf: StringBuilder) {
        var maxxer = max
        while (maxxer > 1) {
            if (`val` < maxxer) {
                buf.append('0')
            }
            maxxer /= TWO_DIGITS
        }
        buf.append(Integer.toString(`val`))
    }

    private fun formatStructuredElement(id: String?, data: StructuredDataElement,
                                        sb: StringBuilder, checker: ListChecker?) {
        if (id == null && defaultId == null || data.discard()) {
            return
        }

        sb.append('[')
        sb.append(id)
        if (mdcSdId.toString() != id) {
            appendMap(data.prefix, data.getFields(), sb, noopChecker)
        } else {
            appendMap(data.prefix, data.getFields(), sb, checker)
        }
        sb.append(']')
    }

    private fun getId(id: StructuredDataId?): String {
        val sb = StringBuilder()
        if (id == null || id.name == null) {
            sb.append(defaultId)
        } else {
            sb.append(id.name)
        }
        var ein = id?.enterpriseNumber ?: enterpriseNumber
        if (ein < 0) {
            ein = enterpriseNumber
        }
        if (ein >= 0) {
            sb.append('@').append(ein)
        }
        return sb.toString()
    }

    private fun checkRequired(map: Map<String, String>) {
        for (key in mdcRequired!!) {
            map[key] ?: throw LoggingException("Required key $key is missing from the $mdcId")
        }
    }

    private fun appendMap(prefix: String?, map: Map<String, String>, sb: StringBuilder,
                          checker: ListChecker?) {
        val sorted = TreeMap(map)
        for ((key, value) in sorted) {
            if (checker!!.check(key) && value != null) {
                sb.append(' ')
                if (prefix != null) {
                    sb.append(prefix)
                }
                val safeKey = escapeNewlines(escapeSDParams(key), escapeNewLine)
                val safeValue = escapeNewlines(escapeSDParams(value), escapeNewLine)
                StringBuilders.appendKeyDqValue(sb, safeKey, safeValue)
            }
        }
    }

    private fun escapeSDParams(value: String): String {
        return PARAM_VALUE_ESCAPE_PATTERN.replace(value, "\\\\$0")
    }

    /**
     * Interface used to check keys in a Map.
     */
    private interface ListChecker {
        fun check(key: String): Boolean
    }

    /**
     * Includes only the listed keys.
     */
    private inner class IncludeChecker : ListChecker {
        override fun check(key: String): Boolean {
            return mdcIncludes!!.contains(key)
        }
    }

    /**
     * Excludes the listed keys.
     */
    private inner class ExcludeChecker : ListChecker {
        override fun check(key: String): Boolean {
            return !mdcExcludes!!.contains(key)
        }
    }

    /**
     * Does nothing.
     */
    private inner class NoopChecker : ListChecker {
        override fun check(key: String): Boolean {
            return true
        }
    }

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append("facility=").append(facility.name)
        sb.append(" appName=").append(appName)
        sb.append(" defaultId=").append(defaultId)
        sb.append(" enterpriseNumber=").append(enterpriseNumber)
        sb.append(" newLine=").append(includeNewLine)
        sb.append(" includeMDC=").append(includeMdc)
        sb.append(" messageId=").append(messageId)
        return sb.toString()
    }

    private inner class FieldFormatter(private val delegateMap: Map<String, List<PatternFormatter>>, private val discardIfEmpty: Boolean) {

        fun format(event: LogEvent): StructuredDataElement {
            val map = HashMap<String, String>(delegateMap.size)

            for ((key, value) in delegateMap) {
                val buffer = StringBuilder()
                for (formatter in value) {
                    formatter.format(event, buffer)
                }
                map[key] = buffer.toString()
            }
            return StructuredDataElement(map, eventPrefix, discardIfEmpty)
        }
    }

    private inner class StructuredDataElement(
            private val fields: MutableMap<String, String>,
            internal val prefix: String?,
            private val discardIfEmpty: Boolean
    ) {

        internal fun discard(): Boolean {
            if (discardIfEmpty == false) {
                return false
            }
            var foundNotEmptyValue = false
            for ((_, value) in fields) {
                if (Strings.isNotEmpty(value)) {
                    foundNotEmptyValue = true
                    break
                }
            }
            return !foundNotEmptyValue
        }

        internal fun union(addFields: Map<String, String>) {
            this.fields.putAll(addFields)
        }

        internal fun getFields(): Map<String, String> {
            return this.fields
        }
    }

    fun getFacility(): Facility {
        return facility
    }

    companion object {
        /**
         * Create the RFC 5424 Layout.
         *
         * @param facility The Facility is used to try to classify the message.
         * @param id The default structured data id to use when formatting according to RFC 5424.
         * @param enterpriseNumber The IANA enterprise number.
         * @param includeMDC Indicates whether data from the ThreadContextMap will be included in the RFC 5424 Syslog
         * record. Defaults to "true:.
         * @param mdcId The id to use for the MDC Structured Data Element.
         * @param mdcPrefix The prefix to add to MDC key names.
         * @param eventPrefix The prefix to add to event key names.
         * @param newLine If true, a newline will be appended to the end of the syslog record. The default is false.
         * @param escapeNL String that should be used to replace newlines within the message text.
         * @param appName The value to use as the APP-NAME in the RFC 5424 syslog record.
         * @param msgId The default value to be used in the MSGID field of RFC 5424 syslog records.
         * @param excludes A comma separated list of MDC keys that should be excluded from the LogEvent.
         * @param includes A comma separated list of MDC keys that should be included in the FlumeEvent.
         * @param required A comma separated list of MDC keys that must be present in the MDC.
         * @param exceptionPattern The pattern for formatting exceptions.
         * @param useTlsMessageFormat If true the message will be formatted according to RFC 5425.
         * @param loggerFields Container for the KeyValuePairs containing the patterns
         * @param config The Configuration. Some Converters require access to the Interpolator.
         * @return An Rfc5424Layout.
         */
        @PluginFactory
        @JvmStatic
        fun createLayout(
                // @formatter:off
                @PluginAttribute(value = "facility", defaultString = "LOCAL0") facility: Facility,
                @PluginAttribute("id") id: String?,
                @PluginAttribute(value = "enterpriseNumber", defaultInt = DEFAULT_ENTERPRISE_NUMBER)
                enterpriseNumber: Int,
                @PluginAttribute(value = "includeMDC", defaultBoolean = true) includeMDC: Boolean,
                @PluginAttribute(value = "mdcId", defaultString = DEFAULT_MDCID) mdcId: String,
                @PluginAttribute("mdcPrefix") mdcPrefix: String?,
                @PluginAttribute("eventPrefix") eventPrefix: String?,
                @PluginAttribute(value = "newLine", defaultBoolean = true) newLine: Boolean,
                @PluginAttribute("newLineEscape") escapeNL: String?,
                @PluginAttribute("appName") appName: String,
                @PluginAttribute("messageId") msgId: String?,
                @PluginAttribute("mdcExcludes") excludes: String?,
                @PluginAttribute("mdcIncludes") includes: String?,
                @PluginAttribute("mdcRequired") required: String?,
                @PluginAttribute("exceptionPattern", defaultString = "%ex") exceptionPattern: String,
                // RFC 5425
                @PluginAttribute(value = "useTlsMessageFormat") useTlsMessageFormat: Boolean?,
                @PluginElement("LoggerFields") loggerFields: Array<LoggerFields>?,
                @PluginConfiguration config: Configuration,
                @PluginAttribute(value = "pattern", defaultString = "%m") pattern: String): Rfc5424PatternLayout {
            val fixedIncludes = includes
                    ?.let {
                        if (excludes != null) {
                            AbstractLayout.LOGGER.error("mdcIncludes and mdcExcludes are mutually exclusive. Includes wil be ignored")
                            null
                        } else {
                            it
                        }
                    }
            return Rfc5424PatternLayout(
                    config = config,
                    facility = facility,
                    id = id,
                    enterpriseNumber = enterpriseNumber,
                    includeMdc = includeMDC,
                    includeNewLine = newLine,
                    escapeNL = escapeNL,
                    mdcId = mdcId,
                    mdcPrefix = mdcPrefix,
                    eventPrefix = eventPrefix,
                    appName = appName,
                    messageId = msgId,
                    excludes = excludes,
                    includes = fixedIncludes,
                    required = required,
                    charset = StandardCharsets.UTF_8,
                    exceptionPattern = exceptionPattern,
                    useTlsMessageFormat = useTlsMessageFormat ?: false,
                    loggerFields = loggerFields,
                    pattern = pattern
            )
        }

        /**
         * Not a very good default - it is the Apache Software Foundation's enterprise number.
         */
        private const val DEFAULT_ENTERPRISE_NUMBER = 18060
        /**
         * The default event id.
         */
        private const val DEFAULT_ID = "Audit"
        /**
         * Match newlines in a platform-independent manner.
         */
        val NEWLINE_PATTERN = Regex("\\r?\\n")
        /**
         * Match characters which require escaping.
         */
        val PARAM_VALUE_ESCAPE_PATTERN = Regex("[\"\\]\\\\]")

        val TAB_FINDER = Regex("\t")
        /**
         * Default MDC ID: {@value} .
         */
        const val DEFAULT_MDCID = "mdc"

        private val LF = "\n"
        private val TWO_DIGITS = 10
        private val THREE_DIGITS = 100
        private val MILLIS_PER_MINUTE = 60000
        private val MINUTES_PER_HOUR = 60
        private val COMPONENT_KEY = "RFC5424-Converter"
    }
}
