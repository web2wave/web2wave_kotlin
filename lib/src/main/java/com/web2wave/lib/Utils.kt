package com.web2wave.lib

import org.json.JSONArray
import org.json.JSONObject

internal fun JSONObject.toMap(): Map<String, Any> {
    val map = mutableMapOf<String, Any>()
    val keys = keys()
    while (keys.hasNext()) {
        val key = keys.next()
        val value = when (val v = this.get(key)) {
            is JSONArray -> List(v.length()) { idx -> v.get(idx) }
            is JSONObject -> v.toMap()
            else -> v
        }
        map[key] = value
    }
    return map
}