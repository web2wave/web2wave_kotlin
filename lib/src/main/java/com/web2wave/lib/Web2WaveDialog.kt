package com.web2wave.lib

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import org.json.JSONObject


private const val URL_KEY = "url_key"
const val TOP_OFFSET_KEY = "top_offset"
const val BOTTOM_OFFSET_KEY = "bottom_offset"
private const val EVENT_QUIZ_FINISHED = "Quiz finished"
private const val EVENT_CLOSE_WEB_VIEW = "Close webview"


internal class Web2WaveDialog : DialogFragment() {

    var listener: Web2WaveWebListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.AppTheme_FullScreenDialog)
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_web_view, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val url = requireArguments().getString(URL_KEY)
        val topOffset = requireArguments().getInt(TOP_OFFSET_KEY, 0)
        val bottomOffset = requireArguments().getInt(BOTTOM_OFFSET_KEY, 0)
        val webView = view as WebView
        setupWebView(webView, url!!, topOffset, bottomOffset)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView(webView: WebView, url: String, topOffset: Int, bottomOffset: Int) {
        webView.webViewClient = WebViewClient()
        webView.webChromeClient = WebChromeClient()

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        WebView.setWebContentsDebuggingEnabled(true)


        webView.addJavascriptInterface(object {
            @JavascriptInterface
            fun postMessage(message: String) {
                try {
                    val json = JSONObject(message)
                    handleJsEvent(json)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }, "Android")

        val newUrl = prepareUrl(url, topOffset, bottomOffset)

        webView.loadUrl(newUrl)
    }

    private fun prepareUrl(url: String, topOffset: Int, bottomOffset: Int): String {
        return Uri.parse(url)
            .buildUpon()
            .appendQueryParameter("webview_android", "1")
            .appendQueryParameter("top_padding", topOffset.toString())
            .appendQueryParameter("bottom_padding", bottomOffset.toString())
            .build()
            .toString()

    }

    private fun handleJsEvent(json: JSONObject) {
        val event = json.optString("event")
        val data = json.optJSONObject("data")?.toMap()

        if (event.isEmpty()) return

        when (event) {
            EVENT_QUIZ_FINISHED -> {
                listener?.onQuizFinished(data)
            }

            EVENT_CLOSE_WEB_VIEW -> {
                listener?.onClose(data)
            }

            else -> {
                listener?.onEvent(event, data)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        listener = null
    }


    companion object {
        fun create(
            url: String,
            listener: Web2WaveWebListener?,
            topOffset: Int,
            bottomOffset: Int
        ): Web2WaveDialog {
            return Web2WaveDialog().apply {
                this.arguments = bundleOf(
                    URL_KEY to url,
                    TOP_OFFSET_KEY to topOffset,
                    BOTTOM_OFFSET_KEY to bottomOffset
                )
                this.listener = listener
            }
        }
    }
}

