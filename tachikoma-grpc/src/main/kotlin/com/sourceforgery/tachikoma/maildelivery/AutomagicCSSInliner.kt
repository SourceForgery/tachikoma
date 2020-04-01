package com.sourceforgery.tachikoma.maildelivery

import com.google.common.base.Strings
import java.util.StringTokenizer
import java.util.TreeMap
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

/**
 * Css inliner for email, inspiration taken from
 * http://stackoverflow.com/questions/4521557/automatically-convert-style-sheets-to-inline-style
 *
 * Thanks to Hecho por Grekz
 */
fun inlineStyles(html: String?): Document? {
    if (html == null) {
        return null
    }
    val doc = Jsoup.parse(html)
    val style = "style"
    val els = doc.select(style) // to get all the style elements
    val selectorMap: TreeMap<String, String> = TreeMap()
    val inlineProps: HashMap<Int, String> = HashMap()
    for (e in els) {
        val styleRules = e.allElements[0].data().replace("\n".toRegex(), "").trim { it <= ' ' }
        val delims = "{}"
        val st = StringTokenizer(styleRules, delims)

        while (st.countTokens() > 1) {
            val selector = st.nextToken().trim().replace(Regex(" +"), " ")
            val properties = st.nextToken().trim().replace(Regex(" +"), " ").replace("\"", "'")
            selectorMap[selector] = properties
        }
    }

    selectorMap.forEach { elem ->
        var selector = elem.key
        val properties = elem.value
        var ignoreSelector = false

        // Process selectors such as "a:hover"
        if (selector.indexOf(":") > 0) {
            selector = selector.substring(0, selector.indexOf(":"))
        }
        if (Strings.isNullOrEmpty(selector)) {
            ignoreSelector = true
        }
        if (selector.contains("*") || selector.contains("@")) {
            ignoreSelector = true
        }
        if (!ignoreSelector) {
            val selectedElements = doc.select(selector)
            for (selElem in selectedElements) {
                if (!inlineProps.containsKey(selElem.hashCode())) {
                    inlineProps[selElem.hashCode()] = selElem.attr(style)
                }
                val oldProperties = selElem.attr(style)
                selElem.attr(
                    style,
                    if (oldProperties.isNotEmpty()) {
                        val inlineProperties = inlineProps[selElem.hashCode()]!!
                        concatenateProperties(oldProperties, properties, inlineProperties)
                    } else {
                        properties
                    }
                )
            }
        }
    }
    return doc
}

private fun concatenateProperties(
    oldProps: String,
    newProps: String,
    inlineProps: String
): String {
    val resultingProps = TreeMap<String, String>()
    oldProps.split(";").filter { it.isNotBlank() }.forEach { el ->
        val (selector, prop) = el.split(":")
        resultingProps[selector.trim()] = prop.trim().replace(";", "")
    }

    newProps.split(";").filter { it.isNotBlank() }.forEach { el ->
        val (selector, prop) = el.split(":")
        resultingProps[selector.trim()] = prop.trim().replace(";", "")
    }

    if (inlineProps.isNotBlank()) {
        inlineProps.split(";").filter { it.isNotBlank() }.forEach { el ->
            val (selector, prop) = el.split(":")
            resultingProps[selector.trim()] = prop.trim().replace(";", "")
        }
    }

    return resultingProps.map { "${it.key}: ${it.value}" }.joinToString("; ")
}