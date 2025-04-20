package com.web2wave.lib

interface Web2WaveWebListener {
    fun onEvent(event: String, data: Map<String, Any>?)
    fun onClose(data: Map<String, Any>?)
    fun onQuizFinished(data: Map<String, Any>?)
}