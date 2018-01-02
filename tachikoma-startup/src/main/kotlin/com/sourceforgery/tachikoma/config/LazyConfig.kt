package com.sourceforgery.tachikoma.config

import com.sun.xml.internal.ws.spi.db.BindingContextFactory.LOGGER
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.Properties
import java.util.UUID

fun <T> readConfig(configKey: String, default: String, clazz: Class<T>): T {
    val stringValue = ConfigData.getProperty(configKey, default)
    try {
        return if (clazz == UUID::class.java) {
            @Suppress("UNCHECKED_CAST")
            UUID.fromString(stringValue) as T
        } else {
            try {
                val method = clazz.getMethod("valueOf", stringValue.javaClass)
                clazz.cast(method.invoke(null, stringValue)) as T
            } catch (e: NoSuchMethodException) {
                val c = clazz.getConstructor(stringValue.javaClass)
                c.newInstance(stringValue)
            }
        }
    } catch (e: Exception) {
        throw RuntimeException("Error running " + clazz
                .name + ".valueOf((" + configKey + ") " + stringValue + ")", e)
    }
}

fun readConfig(configKey: String, default: Boolean) =
        readConfig(configKey, default.toString(), Boolean::class.java)

fun readConfig(configKey: String, default: String) =
        readConfig(configKey, default, String::class.java)

fun readConfig(configKey: String, default: Int) =
        readConfig(configKey, default.toString(), Int::class.java)

fun readConfig(configKey: String, default: Long) =
        readConfig(configKey, default.toString(), Long::class.java)

fun <T> lazyConfig(configKey: String, default: String, clazz: Class<T>): Lazy<T> {
    if (configKey.toUpperCase(Locale.US) != configKey) {
        throw IllegalArgumentException("Only accepts uppercase")
    }
    return lazy(mode = LazyThreadSafetyMode.NONE) {
        readConfig(configKey, default, clazz)
    }
}

fun lazyConfig(configKey: String, default: Boolean) =
        lazyConfig(configKey, default.toString(), Boolean::class.java)

fun lazyConfig(configKey: String, default: String) =
        lazyConfig(configKey, default, String::class.java)

fun lazyConfig(configKey: String, default: Int) =
        lazyConfig(configKey, default.toString(), Int::class.java)

fun lazyConfig(configKey: String, default: Long) =
        lazyConfig(configKey, default.toString(), Long::class.java)

private object ConfigData {
    val properties = Properties()

    init {
        if (java.lang.Boolean.getBoolean("com.tachikoma.read.config")) {
            val configFile = File(System.getProperty("user.home"), ".tachikoma.config")
            try {
                InputStreamReader(FileInputStream(configFile), StandardCharsets.UTF_8)
                        .use { reader -> properties.load(reader) }
            } catch (e: IOException) {
                LOGGER.info { "Couldn't find '$configFile'" }
            }
        }
    }

    fun getProperty(key: String, default: String) =
            System.getenv(key)
                    ?: properties.getProperty(key)
                    ?: default
}
