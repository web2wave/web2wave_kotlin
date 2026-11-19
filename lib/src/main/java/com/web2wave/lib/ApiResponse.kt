package com.web2wave.lib

import org.json.JSONObject

/** Raw HTTP result: status code plus the response body (or the error body for non-2xx). */
internal data class HttpResult(val code: Int, val body: String?) {
    val isSuccessful: Boolean get() = code in 200..299
}

/** Pure helpers for reading web2wave API responses. Kept free of Android APIs so they can be unit tested. */
internal object ApiResponse {

    private const val KEY_SUCCESS = "success"
    private const val KEY_RESULT = "result"
    private const val KEY_ERROR_MSG = "error_msg"
    private const val KEY_MESSAGE = "message"

    /** Parses a body as a JSON object, or returns null when it is empty or not a JSON object. */
    fun parse(body: String?): JSONObject? {
        if (body.isNullOrBlank()) return null
        return try {
            JSONObject(body)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * True when `success` is truthy. The backend returns it as boolean `true` (cancel, charge)
     * or number `1` (refund); both string forms are accepted as well.
     */
    fun isSuccess(json: JSONObject?): Boolean = isTruthy(json?.opt(KEY_SUCCESS))

    /** True when `result` is truthy (`"1"`, as returned by POST /api/user/properties). */
    fun isResultOk(json: JSONObject?): Boolean = isTruthy(json?.opt(KEY_RESULT))

    /**
     * Human-readable failure reason: `error_msg` first (where the backend puts it),
     * then `message`, then a generic fallback mentioning the HTTP code.
     */
    fun errorMessage(json: JSONObject?, httpCode: Int? = null): String {
        nonBlankString(json, KEY_ERROR_MSG)?.let { return it }
        nonBlankString(json, KEY_MESSAGE)?.let { return it }
        return if (httpCode != null && httpCode !in 200..299) {
            "Unexpected response (HTTP $httpCode)"
        } else {
            "Unknown error"
        }
    }

    private fun isTruthy(value: Any?): Boolean = when (value) {
        is Boolean -> value
        is Number -> value.toInt() == 1 && value.toDouble() == 1.0
        is String -> value.trim().let { it == "1" || it.equals("true", ignoreCase = true) }
        else -> false
    }

    private fun nonBlankString(json: JSONObject?, key: String): String? {
        if (json == null || !json.has(key) || json.isNull(key)) return null
        return json.optString(key).takeIf { it.isNotBlank() }
    }
}
