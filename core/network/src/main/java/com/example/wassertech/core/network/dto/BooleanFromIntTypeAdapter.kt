package ru.wassertech.core.network.dto

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter

/**
 * TypeAdapter для Boolean, который принимает как boolean, так и число (0/1)
 * Используется для полей, которые backend может возвращать как число вместо boolean
 */
class BooleanFromIntTypeAdapter : TypeAdapter<Boolean>() {
    
    override fun write(out: JsonWriter, value: Boolean?) {
        if (value == null) {
            out.nullValue()
        } else {
            out.value(if (value) 1 else 0)
        }
    }
    
    override fun read(`in`: JsonReader): Boolean? {
        val token = `in`.peek()
        return when (token) {
            JsonToken.NULL -> {
                `in`.nextNull()
                null
            }
            JsonToken.BOOLEAN -> `in`.nextBoolean()
            JsonToken.NUMBER -> {
                val value = `in`.nextInt()
                value != 0
            }
            JsonToken.STRING -> {
                val str = `in`.nextString()
                when {
                    str.equals("true", ignoreCase = true) -> true
                    str.equals("false", ignoreCase = true) -> false
                    str == "1" -> true
                    str == "0" -> false
                    else -> throw IllegalStateException("Expected boolean or number (0/1), but was STRING: $str")
                }
            }
            else -> throw IllegalStateException("Expected boolean or number (0/1), but was ${token.name}")
        }
    }
}

