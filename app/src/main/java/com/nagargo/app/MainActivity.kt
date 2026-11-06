package com.nagargo.app

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.webkit.GeolocationPermissions
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient

private const val OFFLINE_HTML = """
<html><head><meta name="viewport" content="width=device-width,initial-scale=1"></head>
<body style="margin:0;background:#050806;color:#fff;font-family:sans-serif;display:flex;flex-direction:column;align-items:center;justify-content:center;height:100vh;text-align:center">
<div style="font-size:26px;font-weight:700;color:#00D26A">NagarGo</div>
<p style="color:#9aa7a0;margin:10px 24px 22px">No internet connection</p>
<button onclick="location.href='SITE'" style="background:#00D26A;color:#04110a;border:0;border-radius:24px;padding:12px 28px;font-size:16px;font-weight:700">Try again</button>
</body></html>
"""

class MainActivity : Activity() {
    private lateinit var web: WebView
    private var filePicker: ValueCallback<Array<Uri>>? = null
    private var geoCb: GeolocationPermissions.Callback? = null
    private var geoOrigin: String = ""

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        web = WebView(this)
        setContentView(web)

        web.setBackgroundColor(0xFF050806.toInt())
        web.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            setGeolocationEnabled(true)
            mediaPlaybackRequiresUserGesture = false
            cacheMode = if (online()) WebSettings.LOAD_DEFAULT else WebSettings.LOAD_CACHE_ELSE_NETWORK
            allowFileAccess = false
        }

        web.webViewClient = object : WebViewClient() {
            override fun onReceivedError(view: WebView, req: WebResourceRequest, err: WebResourceError) {
                if (!req.isForMainFrame) return
                if (view.settings.cacheMode != WebSettings.LOAD_CACHE_ELSE_NETWORK) {
                    view.settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
                    view.loadUrl(req.url.toString())
                } else {
                    view.loadDataWithBaseURL(null, OFFLINE_HTML.replace("SITE", BuildConfig.SITE_URL), "text/html", "utf-8", null)
                }
            }
            override fun shouldOverrideUrlLoading(view: WebView, req: WebResourceRequest): Boolean {
                val host = req.url.host ?: ""
                if (host == BuildConfig.SITE_HOST || host.endsWith(".supabase.co")) return false
                try { startActivity(Intent(Intent.ACTION_VIEW, req.url)) } catch (e: Exception) {}
                return true
            }
        }
        web.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(w: WebView, cb: ValueCallback<Array<Uri>>, p: FileChooserParams): Boolean {
                filePicker?.onReceiveValue(null)
                filePicker = cb
                return try { startActivityForResult(p.createIntent(), 77); true }
                catch (e: Exception) { filePicker = null; false }
            }
            override fun onGeolocationPermissionsShowPrompt(origin: String, cb: GeolocationPermissions.Callback) {
                if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    cb.invoke(origin, true, false)
                } else {
                    geoCb = cb
                    geoOrigin = origin
                    requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 2)
                }
            }
        }

        web.loadUrl(intent?.data?.toString() ?: BuildConfig.SITE_URL)
    }

    private fun online(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val n = cm.activeNetwork ?: return false
        val c = cm.getNetworkCapabilities(n) ?: return false
        return c.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.data?.let { web.loadUrl(it.toString()) }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 77) {
            filePicker?.onReceiveValue(
                if (resultCode == RESULT_OK && data != null) WebChromeClient.FileChooserParams.parseResult(resultCode, data) else null)
            filePicker = null
        } else super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 2) {
            val ok = grantResults.any { it == PackageManager.PERMISSION_GRANTED }
            geoCb?.invoke(geoOrigin, ok, false)
            geoCb = null
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (web.canGoBack()) web.goBack() else moveTaskToBack(true)
    }

    override fun onDestroy() {
        if (isFinishing) web.destroy()
        super.onDestroy()
    }
}
