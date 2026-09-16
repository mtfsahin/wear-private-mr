package com.sample.trdtse.m00859088.sample_music_app.healthkit.auth

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.sample.trdtse.m00859088.sample_music_app.healthkit.AuthorizationOutcome
import com.sample.trdtse.m00859088.sample_music_app.healthkit.HealthKitConfig

class WebAuthActivity : Activity() {
    private lateinit var webView: WebView
    private var finished = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val authorizeUrl = intent.getStringExtra(EXTRA_AUTHORIZE_URL)
        if (authorizeUrl.isNullOrBlank()) {
            finish()
            return
        }

        CookieManager.getInstance().setAcceptCookie(true)

        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest,
                ): Boolean = interceptRedirect(request.url)

                override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                    if (interceptRedirect(Uri.parse(url ?: return))) {
                        view.stopLoading()
                    }
                }

                override fun onPageFinished(view: WebView, url: String?) {
                    BODY_CHECK_DELAYS_MS.forEach { delay ->
                        view.postDelayed({ readPageError(view) }, delay)
                    }
                }
            }
        }

        setContentView(webView)
        webView.loadUrl(authorizeUrl)
    }

    private fun interceptRedirect(uri: Uri): Boolean {
        if (!HealthKitConfig.isRedirectUri(uri) && !HealthKitConfig.isDeepLink(uri)) return false

        finishWith(Intent().setData(uri))
        return true
    }

    private fun readPageError(view: WebView) {
        if (finished) return
        view.evaluateJavascript(BODY_TEXT_SCRIPT) { raw ->
            if (finished || raw == null || raw == "null") return@evaluateJavascript
            val text = raw.removeSurrounding("\"")
                .replace("\\\"", "\"")
                .replace("\\n", " ")
                .replace("\\/", "/")
            OAuthPageError.parse(text)?.let { message ->
                finishWith(Intent().putExtra(EXTRA_ERROR, message))
            }
        }
    }

    private fun finishWith(data: Intent) {
        if (finished) return
        finished = true
        setResult(RESULT_OK, data)
        finish()
    }

    override fun onDestroy() {
        if (::webView.isInitialized) {
            webView.destroy()
        }
        super.onDestroy()
    }

    companion object {
        const val REQUEST_CODE = 2001

        private const val EXTRA_AUTHORIZE_URL = "authorize_url"
        private val BODY_CHECK_DELAYS_MS = listOf(0L, 400L, 1200L)
        private const val EXTRA_ERROR = "authorize_error"
        private const val BODY_TEXT_SCRIPT =
            "(function(){return document.body ? document.body.innerText : '';})();"

        fun intent(context: Context, authorizeUrl: String): Intent =
            Intent(context, WebAuthActivity::class.java)
                .putExtra(EXTRA_AUTHORIZE_URL, authorizeUrl)

        fun readOutcome(requestCode: Int, data: Intent?): AuthorizationOutcome? {
            if (requestCode != REQUEST_CODE) return null

            data?.getStringExtra(EXTRA_ERROR)?.let { return AuthorizationOutcome.Failed(it) }
            data?.data?.let { return AuthorizationOutcome.Redirect(it) }

            return AuthorizationOutcome.Failed("Authorization cancelled.")
        }
    }
}
