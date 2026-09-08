package com.github.libretube.api.poToken

import okio.ByteString.Companion.decodeBase64
import okio.ByteString.Companion.toByteString

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long

data class Challenge(
    var interpreterJavascript: String?,
    val interpreterScriptUrl: String?,
    val interpreterHash: String,
    val program: String,
    val globalName: String,
    val clientExperimentsStateBlob: String,
) {
    /**
     * Returns an object that can be embedded in a JavaScript snippet.
     */
    fun encode() = Json.encodeToString(
            JsonObject.serializer(), JsonObject(
                mapOf(
                    "interpreterJavascript" to JsonObject(
                        mapOf(
                            "privateDoNotAccessOrElseSafeScriptWrappedValue" to JsonPrimitive(interpreterJavascript),
                            "privateDoNotAccessOrElseTrustedResourceUrlWrappedValue" to JsonPrimitive(interpreterScriptUrl)
                        )
                    ),
                    "interpreterHash" to JsonPrimitive(interpreterHash),
                    "program" to JsonPrimitive(program),
                    "globalName" to JsonPrimitive(globalName),
                    "clientExperimentsStateBlob" to JsonPrimitive(clientExperimentsStateBlob)
                )
            )
        )
    }


/**
 * Parses the raw challenge data obtained from the Create endpoint.
 */
fun parseChallengeData(rawChallengeData: String): Challenge {
    val challenge = Json.parseToJsonElement(rawChallengeData).jsonObject["bgChallenge"]!!.jsonObject

    val interpreterHash = challenge["interpreterHash"]!!.jsonPrimitive.content
    val program = challenge["program"]!!.jsonPrimitive.content
    val globalName = challenge["globalName"]!!.jsonPrimitive.content
    val clientExperimentsStateBlob = challenge["clientExperimentsStateBlob"]!!.jsonPrimitive.content

    val interpreterUrl = challenge["interpreterUrl"]!!.jsonObject
    val interpreterJavascript =
        interpreterUrl["privateDoNotAccessOrElseSafeScriptWrappedValue"]?.jsonPrimitive?.content

    val interpreterScriptUrl =
        interpreterUrl["privateDoNotAccessOrElseTrustedResourceUrlWrappedValue"]?.jsonPrimitive?.content?.let {
            if (it.startsWith("//")) "https:$it" else it
        }

    assert(interpreterJavascript != null || interpreterScriptUrl != null,
        { "No valid botguard scrip found" })

    return Challenge(
        interpreterJavascript,
        interpreterScriptUrl,
        interpreterHash,
        program,
        globalName,
        clientExperimentsStateBlob
    )
}

/**
 * Parses the raw integrity token data obtained from the GenerateIT endpoint to a JavaScript
 * `Uint8Array` that can be embedded directly in JavaScript code, and an [Int] representing the
 * duration of this token in seconds.
 */
fun parseIntegrityTokenData(rawIntegrityTokenData: String): Pair<String, Long> {
    val integrityTokenData = Json.parseToJsonElement(rawIntegrityTokenData).jsonArray
    return base64ToU8(integrityTokenData[0].jsonPrimitive.content) to integrityTokenData[1].jsonPrimitive.long
}

/**
 * Converts a string (usually the identifier used as input to `obtainPoToken`) to a JavaScript
 * `Uint8Array` that can be embedded directly in JavaScript code.
 */
fun stringToU8(identifier: String): String {
    return newUint8Array(identifier.toByteArray())
}

/**
 * Takes a poToken encoded as a sequence of bytes represented as integers separated by commas
 * (e.g. "97,98,99" would be "abc"), which is the output of `Uint8Array::toString()` in JavaScript,
 * and converts it to the specific base64 representation for poTokens.
 */
fun u8ToBase64(poToken: String): String {
    return poToken.split(",")
        .map { it.toUByte().toByte() }
        .toByteArray()
        .toByteString()
        .base64()
        .replace("+", "-")
        .replace("/", "_")
}

/**
 * Takes the scrambled challenge, decodes it from base64, adds 97 to each byte.
 */
private fun descramble(scrambledChallenge: String): String {
    return base64ToByteString(scrambledChallenge)
        .map { (it + 97).toByte() }
        .toByteArray()
        .decodeToString()
}

/**
 * Decodes a base64 string encoded in the specific base64 representation used by YouTube, and
 * returns a JavaScript `Uint8Array` that can be embedded directly in JavaScript code.
 */
private fun base64ToU8(base64: String): String {
    return newUint8Array(base64ToByteString(base64))
}

private fun newUint8Array(contents: ByteArray): String {
    return "new Uint8Array([" + contents.joinToString(separator = ",") { it.toUByte().toString() } + "])"
}

/**
 * Decodes a base64 string encoded in the specific base64 representation used by YouTube.
 */
private fun base64ToByteString(base64: String): ByteArray {
    val base64Mod = base64
        .replace('-', '+')
        .replace('_', '/')
        .replace('.', '=')

    return (base64Mod.decodeBase64() ?: throw PoTokenException("Cannot base64 decode"))
        .toByteArray()
}

internal fun parseYoutubePageAttestation(pageHtml: String): Pair<String, Challenge> {
    val eventId = EVENT_ID_PATTERN.find(pageHtml)?.groupValues?.get(1)
        ?: throw PoTokenException("YouTube page has no EVENT_ID")
    val call = YT_AT_N_PATTERN.find(pageHtml)
        ?: throw PoTokenException("YouTube page has no initial attestation call")
    val responseProperty = YT_AT_N_RESPONSE_PATTERN.find(pageHtml, call.range.last + 1)
        ?: throw PoTokenException("YouTube page attestation has no response payload")

    val quote = responseProperty.groupValues[1].single()
    val rawChallengeData = decodeJavascriptString(
        pageHtml,
        responseProperty.range.last + 1,
        quote,
    )
    return Pair(eventId, parseChallengeData(rawChallengeData))
}

private fun decodeJavascriptString(source: String, start: Int, quote: Char): String {
    val result = StringBuilder()
    var index = start
    while (index < source.length) {
        val character = source[index++]
        if (character == quote) {
            return result.toString()
        }
        if (character != '\\') {
            result.append(character)
            continue
        }
        require(index < source.length) { "Incomplete JavaScript string escape" }
        when (val escaped = source[index++]) {
            'b' -> result.append('\b')
            'f' -> result.append('\u000C')
            'n' -> result.append('\n')
            'r' -> result.append('\r')
            't' -> result.append('\t')
            'v' -> result.append('\u000B')
            'x' -> {
                result.append(readJavascriptHex(source, index, 2).toChar())
                index += 2
            }
            'u' -> {
                result.append(readJavascriptHex(source, index, 4).toChar())
                index += 4
            }
            '\n' -> Unit
            '\r' -> if (index < source.length && source[index] == '\n') index++
            else -> result.append(escaped)
        }
    }
    throw IllegalArgumentException("Unterminated JavaScript string")
}

private fun readJavascriptHex(source: String, start: Int, length: Int): Int {
    require(start + length <= source.length) { "Incomplete hexadecimal escape" }
    var value = 0
    repeat(length) { offset ->
        val digit = source[start + offset].digitToIntOrNull(16)
            ?: throw IllegalArgumentException("Invalid hexadecimal escape")
        value = value * 16 + digit
    }
    return value
}

private val EVENT_ID_PATTERN = Regex("\"EVENT_ID\"\\s*:\\s*\"([A-Za-z0-9_-]+)\"")
private val YT_AT_N_PATTERN = Regex("""window\.ytAtN\s*\(""")
private val YT_AT_N_RESPONSE_PATTERN = Regex("""['"]R['"]\s*:\s*(['"])""")