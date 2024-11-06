package com.sourceforgery.tachikoma.config

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.kotlin.logger
import java.io.File
import java.io.IOException
import java.util.Properties
import java.util.UUID
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

private fun <T> convert(
    clazz: Class<T>,
    stringValue: String,
): T {
    return try {
        @Suppress("UNCHECKED_CAST")
        if (clazz == UUID::class.java) {
            UUID.fromString(stringValue) as T
        } else if (clazz == ByteArray::class.java) {
            stringValue.toByteArray() as T
        } else if (clazz.isPrimitive) {
            val boxed = java.lang.reflect.Array.get(java.lang.reflect.Array.newInstance(clazz, 1), 0)::class.java
            valueOf(boxed, stringValue) as T
        } else {
            valueOf(clazz, stringValue)
        }
    } catch (e: Exception) {
        throw RuntimeException("Error running ${clazz.name}.valueOf($stringValue)", e)
    }
}

private fun <T> valueOf(
    clazz: Class<T>,
    stringValue: String,
): T {
    return try {
        val method = clazz.getMethod("valueOf", stringValue.javaClass)
        clazz.cast(method.invoke(null, stringValue)) as T
    } catch (e: NoSuchMethodException) {
        val c = clazz.getConstructor(stringValue.javaClass)
        c.newInstance(stringValue)
    }
}

private const val REALLY_BAD_KEY = "really_really_poor_dev_encryption_key"

inline fun <reified R, reified T> readConfig(
    propertyName: String,
    default: T,
): ReadOnlyProperty<R, T> = ConfigReader(propertyName, default, T::class.java)

inline fun <reified T> getEnvironment(
    environmentKey: String,
    defaultValue: String? = null,
): ReadOnlyProperty<Any, T> = EnvGetter(environmentKey, defaultValue, T::class.java)

class EnvGetter<T>(
    private val environmentKey: String,
    private val defaultValue: String?,
    private val clazz: Class<T>,
) : ReadOnlyProperty<Any, T> {
    private var data: T? = null
    private var set = false

    private fun readValue(): T {
        if (!set) {
            val stringValue =
                System.getenv(environmentKey)
                    ?: defaultValue
                    ?: throw IllegalArgumentException("Didn't find any environment variable $environmentKey")
            data = convert(clazz, stringValue)
            set = true
        }
        @Suppress("UNCHECKED_CAST")
        return data as T
    }

    override fun getValue(
        thisRef: Any,
        property: KProperty<*>,
    ): T {
        return readValue()
    }
}

fun <R> readEncryptionConfig(propertyName: String) = EncryptionConfig<R, String>(propertyName, REALLY_BAD_KEY, String::class.java)

inline fun <reified R, reified T> readEncryptionConfig(
    propertyName: String,
    defaultValue: T,
) = EncryptionConfig<R, T>(propertyName, defaultValue, T::class.java)

inline fun <reified R, reified T> readListConfig(
    propertyName: String,
    defaultValue: List<T>,
) = ReadListConfig<R, T>(propertyName, defaultValue, T::class.java)

@Suppress("UNCHECKED_CAST")
class ReadListConfig<R, T2>(
    propertyName: String,
    defaultValue: List<T2>,
    private val listClazz: Class<T2>,
) : ConfigReader<R, List<T2>>(
        propertyName = propertyName,
        defaultValue = defaultValue,
        clazz = List::class.java as Class<List<T2>>,
    ) {
    override fun <T> readConfig(configKey: String): T {
        return ConfigData.getProperty(configKey, defaultValue) {
            (it.split(',').map { convert(listClazz, it) })
        } as T
    }
}

class EncryptionConfig<R, T>(
    propertyName: String,
    defaultValue: T,
    clazz: Class<T>,
) : ConfigReader<R, T>(
        propertyName = propertyName,
        defaultValue = defaultValue,
        clazz = clazz,
    ) {
    override fun readValue(property: KProperty<*>) =
        super.readValue(property)
            .also {
                if (it == defaultValue) {
                    LogManager.getLogger("change_dev_key")
                        .error("You're using a DEV key for ${property.name}/$propertyName. Do NOT use in production!!")
                }
            }
}

open class ConfigReader<R, T>(
    internal val propertyName: String,
    internal val defaultValue: T,
    internal val clazz: Class<T>,
) : ReadOnlyProperty<R, T> {
    private var data: T? = null
    var set = false
        set

    override fun getValue(
        thisRef: R,
        property: KProperty<*>,
    ): T {
        return readValue(property)
    }

    protected open fun readValue(property: KProperty<*>): T {
        if (!set) {
            data = readConfig(propertyName)
            set = true
        }
        return data!!
    }

    @Suppress("UNCHECKED_CAST")
    protected open fun <T> readConfig(configKey: String): T {
        return ConfigData.getProperty(configKey, defaultValue) {
            convert(clazz, it)
        } as T
    }
}

private object ConfigData {
    val properties = Properties()
    private val LOGGER = logger()

    init {
        val configFile =
            (
                System.getProperty("tachikomaConfig")
                    ?: System.getenv("TACHIKOMA_CONFIG")
                    ?: (System.getProperty("user.home") + "/.tachikoma.config")
            )
                .let {
                    File(it)
                }
        try {
            configFile.reader()
                .use { properties.load(it) }
            LOGGER.info("Read config from '$configFile'")
        } catch (e: IOException) {
            LOGGER.info { "Couldn't find '$configFile'" }
        }
    }

    fun <T> getProperty(
        key: String,
        default: T,
        converter: (String) -> T,
    ): T =
        (
            System.getenv(key)
                ?: properties.getProperty(key)
        )
            ?.let { converter(it) }
            ?: default
}
