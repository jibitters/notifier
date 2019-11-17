@file:Suppress("unused")

package ir.jibit.notifier.util

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.PropertyAccessor
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule

/**
 * Encapsulates a pre-configured ready-to-use [ObjectMapper].
 *
 * @author Younes Rahimi
 */
object Jackson {
    val mapper = configObjectMapper(ObjectMapper())

    /**
     * Configures an [ObjectMapper].
     */
    @JvmStatic
    fun configObjectMapper(objectMapper: ObjectMapper): ObjectMapper = objectMapper
        .registerModule(KotlinModule())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .setSerializationInclusion(JsonInclude.Include.NON_NULL)
        .configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false)
        .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
        .configure(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true)
        .configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, true)
        .setVisibility(PropertyAccessor.FIELD, Visibility.ANY)

    /**
     * Serializes an object to it's JSON representation.
     */
    @JvmStatic
    fun toJson(obj: Any): String = mapper.writer().writeValueAsString(obj)

    /**
     * Deserialize a JSON string to an object type.
     */
    @JvmStatic
    inline fun <reified T : Any> fromJson(json: String): T = mapper.reader().forType(object : TypeReference<T>() {})
        .readValue<T>(json)
}
