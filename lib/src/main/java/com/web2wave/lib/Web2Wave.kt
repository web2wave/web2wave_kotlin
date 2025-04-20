package com.web2wave.lib

import android.webkit.URLUtil
import androidx.fragment.app.FragmentManager
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private const val BASE_URL = "https://api.web2wave.com"
private const val WEB_2_WAVE = "web2wave_tag"

object Web2Wave {

    private const val PROFILE_ID_REVENUECAT = "revenuecat_profile_id"
    private const val PROFILE_ID_ADAPTY = "adapty_profile_id"
    private const val PROFILE_ID_QONVERSION = "qonversion_profile_id"

    private const val API_SUBSCRIPTIONS = "api/user/subscriptions"
    private const val API_USER_PROPERTIES = "api/user/properties"
    private const val API_SUBSCRIPTION_CANCEL = "api/subscription/cancel"
    private const val API_SUBSCRIPTION_REFUND = "api/subscription/refund"
    private const val API_SUBSCRIPTION_CHARGE = "api/subscription/user/charge"

    private const val KEY_USER = "user"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_PRICE_ID = "price_id"
    private const val KEY_SUBSCRIPTION = "subscription"
    private const val KEY_PROPERTIES = "properties"
    private const val KEY_PROPERTY = "property"
    private const val KEY_COMMENT = "comment"
    private const val KEY_VALUE = "value"
    private const val KEY_PAY_SYSTEM = "pay_system_id"
    private const val KEY_INVOICE_ID = "invoice_id"
    private const val KEY_STATUS = "status"
    private const val KEY_RESULT = "result"

    private const val VALUE_ACTIVE = "active"
    private const val VALUE_TRIAL = "trialing"
    private const val METHOD_TYPE_POST = "POST"
    private const val METHOD_TYPE_GET = "GET"
    private const val METHOD_TYPE_PUT = "PUT"

    private var apiKey: String? = null

    fun initWith(apiKey: String) {
        this.apiKey = apiKey
    }

    private fun buildUrl(path: String, queryParams: Map<String, String>? = null): URL {
        val sb = StringBuilder("$BASE_URL/$path")
        queryParams?.let {
            sb.append("?")
            sb.append(it.entries.joinToString("&") { (key, value) -> "$key=$value" })
        }
        return URL(sb.toString())
    }

    fun fetchSubscriptionStatus(userID: String): Map<String, Any>? {
        checkNotNull(apiKey) { "You have to initialize apiKey before use" }

        val url = buildUrl(API_SUBSCRIPTIONS, mapOf(KEY_USER to userID))

        return try {
            val response = makeRequest(url, METHOD_TYPE_GET)
            response?.let { resp ->
                val json = JSONObject(resp)
                json.toMap()
            }
        } catch (e: Exception) {
            println("Failed to fetch subscription status: ${e.localizedMessage}")
            null
        }
    }

    fun hasActiveSubscription(userID: String): Boolean {
        val status = fetchSubscriptionStatus(userID) ?: return false
        val subscriptions = status[KEY_SUBSCRIPTION] ?: return false
        return try {
            @Suppress("UNCHECKED_CAST")
            val list = subscriptions as List<Map<String, Any>>
            list.any { sub ->
                val value = sub[KEY_STATUS]
                value == VALUE_ACTIVE || value == VALUE_TRIAL
            }
        } catch (e: Exception) {
            println("Failed to fetch subscription status: ${e.localizedMessage}")
            false
        }
    }

    fun fetchSubscriptions(userID: String): List<Map<String, Any>>? {
        val result = fetchSubscriptionStatus(userID)
        return result?.get(KEY_SUBSCRIPTION) as? List<Map<String, Any>>
    }

    fun cancelSubscription(paySystemId: String, comment: String? = null): Result<Boolean> {
        checkNotNull(apiKey) { "You must initialize apiKey before use" }
        return try {
            val url = buildUrl(API_SUBSCRIPTION_CANCEL)
            val body = JSONObject().apply {
                put(KEY_PAY_SYSTEM, paySystemId)
                if (!comment.isNullOrBlank()) put(KEY_COMMENT, comment)
            }.toString()

            val response = makeRequest(url, METHOD_TYPE_PUT, body)
            val json = response?.let { JSONObject(it) }

            if (json?.optString("success") == "1") Result.success(true)
            else Result.failure(Exception(json?.optString("message") ?: "Unknown error"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun chargeUser(web2waveUserId: String, priceId: Int): Result<Boolean> {
        checkNotNull(apiKey) { "You must initialize apiKey before use" }
        return try {
            val url = buildUrl(API_SUBSCRIPTION_CHARGE)
            val body = JSONObject().apply {
                put(KEY_USER_ID, web2waveUserId)
                put(KEY_PRICE_ID, priceId.toString())
            }.toString()

            val response = makeRequest(url, METHOD_TYPE_POST, body)
            val json = response?.let { JSONObject(it) }

            if (json?.optString("success") == "1") Result.success(true)
            else Result.failure(Exception(json?.optString("message") ?: "Unknown error"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun refundSubscription(
        paySystemId: String,
        invoiceId: String,
        comment: String? = null
    ): Result<Boolean> {
        checkNotNull(apiKey) { "You must initialize apiKey before use" }
        return try {
            val url = buildUrl(API_SUBSCRIPTION_REFUND)
            val body = JSONObject().apply {
                put(KEY_PAY_SYSTEM, paySystemId)
                put(KEY_INVOICE_ID, invoiceId)
                if (!comment.isNullOrBlank()) put(KEY_COMMENT, comment)
            }.toString()

            val response = makeRequest(url, METHOD_TYPE_PUT, body)
            val json = response?.let { JSONObject(it) }

            if (json?.optString("success") == "1") Result.success(true)
            else Result.failure(Exception(json?.optString("message") ?: "Unknown error"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchUserProperties(userID: String): Map<String, Any>? {
        checkNotNull(apiKey) { "You have to initialize apiKey before use" }
        val url = buildUrl(API_USER_PROPERTIES, mapOf(KEY_USER to userID))

        return try {
            val response = makeRequest(url, METHOD_TYPE_GET)
            response?.let {
                val json = JSONObject(it)
                val properties = json.optJSONArray(KEY_PROPERTIES) ?: return@let null
                val result = mutableMapOf<String, String>()
                for (i in 0 until properties.length()) {
                    val item = properties.getJSONObject(i)
                    val key = item.optString(KEY_PROPERTY)
                    val value = item.optString(KEY_VALUE)
                    result[key] = value
                }
                result
            }
        } catch (e: Exception) {
            println("Failed to fetch properties: ${e.localizedMessage}")
            null
        }
    }

    fun setAdaptyProfileID(appUserID: String, adaptyProfileID: String): Result<Unit> =
        updateUserProperty(appUserID, PROFILE_ID_ADAPTY, adaptyProfileID)

    fun setQonversionProfileID(appUserID: String, qonversionProfileID: String): Result<Unit> =
        updateUserProperty(appUserID, PROFILE_ID_QONVERSION, qonversionProfileID)

    suspend fun setRevenuecatProfileID(
        appUserID: String,
        revenueCatProfileID: String
    ): Result<Unit> =
        updateUserProperty(appUserID, PROFILE_ID_REVENUECAT, revenueCatProfileID)

    fun updateUserProperty(userID: String, property: String, value: String): Result<Unit> {
        checkNotNull(apiKey) { "You have to initialize apiKey before use" }

        val url = buildUrl(API_USER_PROPERTIES, mapOf(KEY_USER to userID))
        val body = JSONObject().apply {
            put(KEY_PROPERTY, property)
            put(KEY_VALUE, value)
        }.toString()

        return try {
            val response = makeRequest(url, METHOD_TYPE_POST, body)
            val json = response?.let { JSONObject(it) }
            if (json?.optString(KEY_RESULT) == "1") Result.success(Unit)
            else Result.failure(Exception("Unexpected response"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun makeRequest(url: URL, method: String, body: String? = null): String? {
        val connection = url.openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = method
            connection.setRequestProperty("api-key", apiKey)
            connection.setRequestProperty("Cache-Control", "no-cache")
            connection.setRequestProperty("Pragma", "no-cache")

            if (method == METHOD_TYPE_POST || method == METHOD_TYPE_PUT) {
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true
                body?.let { connection.outputStream.use { os -> os.write(it.toByteArray()) } }
            }

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                println(
                    "Unexpected response code: ${connection.responseCode}," +
                            " message: ${connection.responseMessage}, " +
                            " details: ${
                                connection.errorStream.bufferedReader().use { it.readText() }
                            }"
                )
                null
            }
        } catch (e: Exception) {
            println("Request failed: ${e.localizedMessage}")
            null
        } finally {
            connection.disconnect()
        }
    }

    fun showWebView(
        fragmentManager: FragmentManager,
        url: String, listener: Web2WaveWebListener,
        topOffset: Int = 0,
        bottomOffset: Int = 0
    ) {
        checkNotNull(apiKey) { "You must initialize apiKey before use" }
        check(URLUtil.isValidUrl(url)) { "You must provide valid url" }
        Web2WaveDialog.create(
            url, listener, topOffset, bottomOffset
        ).show(fragmentManager, WEB_2_WAVE)
    }

    fun closeWebView(fragmentManager: FragmentManager) {
        fragmentManager.findFragmentByTag(WEB_2_WAVE)?.let {
            (it as Web2WaveDialog).dismissAllowingStateLoss()
        }
    }
}


