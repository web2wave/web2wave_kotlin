import kotlinx.serialization.json.*
import java.net.HttpURLConnection
import java.net.URL


private const val BASE_URL = "https://api.web2wave.com"

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
                val json = Json.decodeFromString<Map<String, JsonElement>>(resp)
                json.map {
                    it.key to it.value.toAny()
                }.toMap()
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
            subscriptions as List<Map<String, Any>>
            subscriptions.any() { sub ->
                val value = sub[KEY_STATUS]
                value == VALUE_ACTIVE || value == VALUE_TRIAL
            }
        } catch (e: Exception) {
            println("Failed to fetch subscription status: ${e.localizedMessage}")
            false
        }
    }

    fun fetchSubscriptions(userID: String): List<Map<String, Any>>? {
        checkNotNull(apiKey) { "You have to initialize apiKey before use" }
        val result = fetchSubscriptionStatus(userID)
        return result?.get(KEY_SUBSCRIPTION) as? List<Map<String, Any>>
    }

    fun cancelSubscription(
        paySystemId: String,
        comment: String? = null
    ): Result<Boolean> {
        checkNotNull(apiKey) { "You must initialize apiKey before use" }
        try {
            val url = buildUrl(API_SUBSCRIPTION_CANCEL)
            val bodyMap = mutableMapOf(KEY_PAY_SYSTEM to paySystemId)

            if (!comment.isNullOrBlank()) {
                bodyMap[KEY_COMMENT] = comment
            }

            val body = Json.encodeToString(bodyMap)
            val response = makeRequest(
                url = url,
                method = METHOD_TYPE_PUT,
                body = body
            )

            val json = response?.let { Json.parseToJsonElement(it).jsonObject }

            return if (json?.get("success")?.jsonPrimitive?.content == "1") {
                Result.success(true)
            } else {
                Result.failure(Exception(json?.get("message")?.jsonPrimitive?.content ?: "Unknown error"))
            }

        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    fun chargeUser(
        web2waveUserId: String,
        priceId: Int
    ): Result<Boolean> {
        checkNotNull(apiKey) { "You must initialize apiKey before use" }
        try {
            val url = buildUrl(API_SUBSCRIPTION_CHARGE)
            val body = Json.encodeToString(
                mapOf(
                    KEY_USER_ID to web2waveUserId,
                    KEY_PRICE_ID to priceId.toString()
                )
            )

            val response = makeRequest(url = url, method = METHOD_TYPE_PUT, body = body)
            val json = response?.let { Json.parseToJsonElement(it).jsonObject }

            return if (json?.get("success")?.jsonPrimitive?.content == "1") {
                Result.success(true)
            } else {
                Result.failure(Exception(json?.get("message")?.jsonPrimitive?.content ?: "Unknown error"))
            }

        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    suspend fun refundSubscription(
        paySystemId: String,
        invoiceId: String,
        comment: String? = null
    ): Result<Boolean> {
        checkNotNull(apiKey) { "You must initialize apiKey before use" }
        try {
            val url = buildUrl(API_SUBSCRIPTION_REFUND)

            val bodyMap = mutableMapOf(
                KEY_PAY_SYSTEM to paySystemId,
                KEY_INVOICE_ID to invoiceId
            )
            if (!comment.isNullOrBlank()) {
                bodyMap[KEY_COMMENT] = comment
            }

            val body = Json.encodeToString(bodyMap)
            val response = makeRequest(url = url, method = METHOD_TYPE_PUT, body = body)
            val json = response?.let { Json.parseToJsonElement(it).jsonObject }

            return if (json?.get("success")?.jsonPrimitive?.content == "1") {
                Result.success(true)
            } else {
                Result.failure(Exception(json?.get("message")?.jsonPrimitive?.content ?: "Unknown error"))
            }

        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    suspend fun fetchUserProperties(userID: String): Map<String, Any>? {
        checkNotNull(apiKey) { "You have to initialize apiKey before use" }
        val url = buildUrl(API_USER_PROPERTIES, mapOf(KEY_USER to userID))

        return try {
            val response = makeRequest(url, METHOD_TYPE_GET)
            response?.let { resp ->
                val json = Json.decodeFromString<Map<String, JsonElement>>(resp)
                json[KEY_PROPERTIES]?.jsonArray?.associate {
                    val key = it.jsonObject[KEY_PROPERTY]?.jsonPrimitive?.content ?: ""
                    val value = it.jsonObject[KEY_VALUE]?.jsonPrimitive?.content ?: ""
                    key to value
                }
            }
        } catch (e: Exception) {
            println("Failed to fetch properties: ${e.localizedMessage}")
            null
        }
    }

    suspend fun setRevenuecatProfileID(appUserID: String, revenueCatProfileID: String): Result<Unit> {
        return updateUserProperty(appUserID, PROFILE_ID_REVENUECAT, revenueCatProfileID)
    }

    fun setAdaptyProfileID(appUserID: String, adaptyProfileID: String): Result<Unit> {
        return updateUserProperty(appUserID, PROFILE_ID_ADAPTY, adaptyProfileID)
    }

    fun setQonversionProfileID(appUserID: String, qonversionProfileID: String): Result<Unit> {
        return updateUserProperty(appUserID, PROFILE_ID_QONVERSION, qonversionProfileID)
    }

    fun updateUserProperty(userID: String, property: String, value: String): Result<Unit> {
        checkNotNull(apiKey) { "You have to initialize apiKey before use" }

        val url = buildUrl(API_USER_PROPERTIES, mapOf(KEY_USER to userID))
        val body = Json.encodeToString(
            JsonObject(mapOf(KEY_PROPERTY to JsonPrimitive(property), KEY_VALUE to JsonPrimitive(value)))
        )

        return try {
            val response = makeRequest(url, METHOD_TYPE_POST, body)
            response?.let {
                val jsonResponse = Json.parseToJsonElement(it).jsonObject
                if (jsonResponse[KEY_RESULT]?.jsonPrimitive?.content == "1") Result.success(Unit)
                else Result.failure(Exception("Unexpected response"))
            } ?: Result.failure(Exception("Empty response"))
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

            if (method == METHOD_TYPE_POST) {
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true
                body?.let { bd ->
                    connection.outputStream.use { it.write(bd.toByteArray(Charsets.UTF_8)) }
                }
            }

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                println("Unexpected response code: ${connection.responseCode}")
                null
            }
        } catch (e: Exception) {
            println("Request failed: ${e.localizedMessage}")
            null
        } finally {
            connection.disconnect()
        }
    }

    private fun JsonObject.toMapAny(): Map<String, Any> {
        return this.mapValues { (_, value) ->
            value.toAny()
        }
    }

    private fun JsonElement.toAny(): Any {
        return when (this) {
            is JsonPrimitive -> this.toPrimitiveValue()
            is JsonObject -> this.toMapAny()
            is JsonArray -> this.toListAny()
            else -> this
        }
    }

    private fun JsonPrimitive.toPrimitiveValue(): Any {
        return when {
            this.isString -> this.content
            this.booleanOrNull != null -> this.boolean
            this.intOrNull != null -> this.int
            this.longOrNull != null -> this.long
            this.doubleOrNull != null -> this.double
            else -> this.content
        }
    }

    private fun JsonArray.toListAny(): List<Any> {
        return this.map { it.toAny() }
    }

}
