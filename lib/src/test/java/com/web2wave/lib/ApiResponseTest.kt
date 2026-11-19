package com.web2wave.lib

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiResponseTest {

    private fun json(body: String) = ApiResponse.parse(body)

    @Test
    fun cancelAndChargeBooleanSuccessIsSuccess() {
        assertTrue(ApiResponse.isSuccess(json("""{"success": true, "error": 0, "message": "Subscription canceled"}""")))
        assertTrue(ApiResponse.isSuccess(json("""{"success": true, "message": "Charged", "subscription_id": 42}""")))
    }

    @Test
    fun refundNumericSuccessIsSuccess() {
        assertTrue(ApiResponse.isSuccess(json("""{"success": 1, "error": 0, "message": "Request accepted"}""")))
    }

    @Test
    fun stringSuccessFormsAreSuccess() {
        assertTrue(ApiResponse.isSuccess(json("""{"success": "1"}""")))
        assertTrue(ApiResponse.isSuccess(json("""{"success": "true"}""")))
    }

    @Test
    fun falsyOrMissingSuccessIsFailure() {
        assertFalse(ApiResponse.isSuccess(json("""{"success": false, "message": "Card declined"}""")))
        assertFalse(ApiResponse.isSuccess(json("""{"success": 0}""")))
        assertFalse(ApiResponse.isSuccess(json("""{"success": "0"}""")))
        assertFalse(ApiResponse.isSuccess(json("""{"success": null}""")))
        assertFalse(ApiResponse.isSuccess(json("""{"error": 1, "error_msg": "Subscription not found"}""")))
        assertFalse(ApiResponse.isSuccess(null))
    }

    @Test
    fun propertiesResult() {
        assertTrue(ApiResponse.isResultOk(json("""{"result":"1"}""")))
        assertTrue(ApiResponse.isResultOk(json("""{"result":1}""")))
        assertFalse(ApiResponse.isResultOk(json("""{"error":1,"error_msg":"The value field is required."}""")))
    }

    @Test
    fun errorMessagePrefersErrorMsg() {
        assertEquals(
            "Subscription not found",
            ApiResponse.errorMessage(json("""{"error":1,"error_msg":"Subscription not found","message":""}"""), 400)
        )
        assertEquals(
            "This API key does not have the 'subscriptions:write' scope",
            ApiResponse.errorMessage(
                json("""{"error":1,"error_msg":"This API key does not have the 'subscriptions:write' scope","required_scope":"subscriptions:write"}"""),
                403
            )
        )
    }

    @Test
    fun errorMessageFallsBackToMessage() {
        assertEquals("Card declined", ApiResponse.errorMessage(json("""{"success": false, "message": "Card declined"}"""), 200))
        assertEquals("Card declined", ApiResponse.errorMessage(json("""{"error_msg": null, "message": "Card declined"}"""), 200))
    }

    @Test
    fun errorMessageGenericFallback() {
        assertEquals("Unexpected response (HTTP 500)", ApiResponse.errorMessage(null, 500))
        assertEquals("Unexpected response (HTTP 502)", ApiResponse.errorMessage(json("""{"message": ""}"""), 502))
        assertEquals("Unknown error", ApiResponse.errorMessage(json("""{}"""), 200))
    }

    @Test
    fun parseToleratesNonJsonBodies() {
        assertNull(ApiResponse.parse(null))
        assertNull(ApiResponse.parse(""))
        assertNull(ApiResponse.parse("<html>Bad gateway</html>"))
    }

    @Test
    fun httpResultSuccessRange() {
        assertTrue(HttpResult(200, null).isSuccessful)
        assertTrue(HttpResult(201, null).isSuccessful)
        assertFalse(HttpResult(400, null).isSuccessful)
        assertFalse(HttpResult(403, null).isSuccessful)
    }
}
