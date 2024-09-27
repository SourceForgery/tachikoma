package com.sourceforgery.jersey.uribuilder

import com.google.common.collect.Multimap
import com.google.common.collect.MultimapBuilder
import com.google.common.collect.Multimaps

private val EMPTY_MULTIMAP = Multimaps.unmodifiableMultimap(MultimapBuilder.hashKeys().arrayListValues().build<Any, Any>())

@Suppress("UNCHECKED_CAST")
fun <K, V> emptyMultimap(): Multimap<K, V> = EMPTY_MULTIMAP as Multimap<K, V>
