package com.sourceforgery.tachikoma.config

import com.sun.xml.internal.ws.spi.db.BindingContextFactory.LOGGER
import org.apache.logging.log4j.LogManager
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
        } else if (clazz.isPrimitive) {
            val boxed = java.lang.reflect.Array.get(java.lang.reflect.Array.newInstance(clazz, 1), 0)::class.java
            @Suppress("UNCHECKED_CAST")
            valueOf(boxed, stringValue) as T
        } else {
            valueOf(clazz, stringValue)
        }
    } catch (e: Exception) {
        throw RuntimeException("Error running " + clazz
            .name + ".valueOf((" + configKey + ") " + stringValue + ")", e)
    }
}

private fun <T> valueOf(clazz: Class<T>, stringValue: String): T {
    return try {
        val method = clazz.getMethod("valueOf", stringValue.javaClass)
        clazz.cast(method.invoke(null, stringValue)) as T
    } catch (e: NoSuchMethodException) {
        val c = clazz.getConstructor(stringValue.javaClass)
        c.newInstance(stringValue)
    }
}

fun readConfig(configKey: String, default: Boolean) =
    readConfig(configKey, default.toString(), Boolean::class.java)

fun readConfig(configKey: String, default: String) =
    readConfig(configKey, default, String::class.java)

private const val REALLY_BAD_KEY = "really_really_poor_dev_encryption_key"

fun readEncryptionConfig(configKey: String) =
    readConfig(configKey, REALLY_BAD_KEY)
        .also {
            if (it == REALLY_BAD_KEY) {
                LogManager.getLogger("change_dev_key")
                    .error("You're using a DEV key for $configKey. Do NOT use in production!!")
            }
        }

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
        val configFile = System.getenv("TACHIKOMA_CONFIG")
            ?.let {
                File(it)
            }
            ?: File(System.getProperty("user.home"), ".tachikoma.config")
        try {
            InputStreamReader(FileInputStream(configFile), StandardCharsets.UTF_8)
                .use { reader -> properties.load(reader) }
            LOGGER.info("Read config from '$configFile'")
        } catch (e: IOException) {
            LOGGER.info { "Couldn't find '$configFile'" }
        }
    }

    fun getProperty(key: String, default: String) =
        System.getenv(key)
            ?: properties.getProperty(key)
            ?: default
}
