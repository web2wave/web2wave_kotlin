import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

    private const val KEY_USER = "user"
    private const val KEY_SUBSCRIPTION = "subscription"
    private const val KEY_PROPERTIES = "properties"
    private const val KEY_PROPERTY = "property"
    private const val KEY_VALUE = "value"
    private const val KEY_STATUS = "status"
    private const val KEY_RESULT = "result"

    private const val VALUE_ACTIVE = "active"
    private const val VALUE_TRIAL = "trialing"

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

    suspend fun fetchSubscriptionStatus(userID: String): Map<String, Any>? {
        checkNotNull(apiKey) { "You have to initialize apiKey before use" }

        val url = buildUrl(API_SUBSCRIPTIONS, mapOf(KEY_USER to userID))

        return withContext(Dispatchers.IO) {
            try {
                val response = makeRequest(url, "GET")
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
    }

    suspend fun hasActiveSubscription(userID: String): Boolean {
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

    suspend fun fetchSubscriptions(userID: String): List<Map<String, Any>>? {
        checkNotNull(apiKey) { "You have to initialize apiKey before use" }
        val result = fetchSubscriptionStatus(userID)
        return result?.get(KEY_SUBSCRIPTION) as? List<Map<String, Any>>
    }

    suspend fun fetchUserProperties(userID: String): Map<String, Any>? {
        checkNotNull(apiKey) { "You have to initialize apiKey before use" }
        val url = buildUrl(API_USER_PROPERTIES, mapOf(KEY_USER to userID))

        return withContext(Dispatchers.IO) {
            try {
                val response = makeRequest(url, "GET")
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
    }


    suspend fun setRevenuecatProfileID(appUserID: String, revenueCatProfileID: String): Result<Unit> {
        return updateUserProperty(appUserID, PROFILE_ID_REVENUECAT, revenueCatProfileID)
    }

    suspend fun setAdaptyProfileID(appUserID: String, adaptyProfileID: String): Result<Unit> {
        return updateUserProperty(appUserID, PROFILE_ID_ADAPTY, adaptyProfileID)
    }

    suspend fun setQonversionProfileID(appUserID: String, qonversionProfileID: String): Result<Unit> {
        return updateUserProperty(appUserID, PROFILE_ID_QONVERSION, qonversionProfileID)
    }

    suspend fun updateUserProperty(userID: String, property: String, value: String): Result<Unit> {
        checkNotNull(apiKey) { "You have to initialize apiKey before use" }

        val url = buildUrl(API_USER_PROPERTIES, mapOf(KEY_USER to userID))
        val body = Json.encodeToString(
            JsonObject(mapOf(KEY_PROPERTY to JsonPrimitive(property), KEY_VALUE to JsonPrimitive(value)))
        )

        return withContext(Dispatchers.IO) {
            try {
                val response = makeRequest(url, "POST", body)
                response?.let {
                    val jsonResponse = Json.parseToJsonElement(it).jsonObject
                    if (jsonResponse[KEY_RESULT]?.jsonPrimitive?.content == "1") Result.success(Unit)
                    else Result.failure(Exception("Unexpected response"))
                } ?: Result.failure(Exception("Empty response"))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }


    private fun makeRequest(url: URL, method: String, body: String? = null): String? {
        val connection = url.openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = method
            connection.setRequestProperty("api-key", apiKey)
            connection.setRequestProperty("Cache-Control", "no-cache")
            connection.setRequestProperty("Pragma", "no-cache")

            if (method == "POST") {
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true
                connection.outputStream.use { it.write(body?.toByteArray(Charsets.UTF_8)) }
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
