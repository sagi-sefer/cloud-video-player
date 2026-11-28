package com.example.cloudvideoplayer.presentation.ui

import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.cloudvideoplayer.BuildConfig

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    val appKey = BuildConfig.DROPBOX_VIDEO_APP_KEY
    var manualToken by remember { mutableStateOf("") }
    var showWebViewLogin by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val sharedPreferences = remember {
        context.getSharedPreferences("cloud_video_prefs", android.content.Context.MODE_PRIVATE)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Cloud Video Player",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Connect your Dropbox account to stream your movies, series, and video collections in high definition.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = {
                showWebViewLogin = true
            }
        ) {
            Text("Login with Dropbox (In-App)")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Or enter Dropbox Access Token manually:",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = manualToken,
            onValueChange = { manualToken = it },
            label = { Text("Access Token") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = {
                if (manualToken.isNotBlank()) {
                    sharedPreferences.edit().putString("dropbox_access_token", manualToken.trim()).apply()
                    onLoginSuccess()
                }
            },
            enabled = manualToken.isNotBlank()
        ) {
            Text("Save Token & Login")
        }
    }

    if (showWebViewLogin) {
        AlertDialog(
            onDismissRequest = { showWebViewLogin = false },
            title = { Text("Dropbox Sign-In") },
            text = {
                Box(modifier = Modifier.fillMaxWidth().height(450.dp)) {
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true

                                CookieManager.getInstance().setAcceptCookie(true)
                                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                                val injectAntiCookieScript = { view: WebView? ->
                                    view?.evaluateJavascript("""
                                        (function() {
                                            function purgeCookieBanner() {
                                                var buttons = document.querySelectorAll('button, div[role="button"], a, span');
                                                for (var i = 0; i < buttons.length; i++) {
                                                    var text = (buttons[i].innerText || buttons[i].textContent || '').trim().toLowerCase();
                                                    if (text === 'accept all' || text === 'accept' || text === 'accept cookies') {
                                                        try {
                                                            buttons[i].click();
                                                        } catch(e) {}
                                                    }
                                                }

                                                var allDivs = document.querySelectorAll('div, section, aside');
                                                for (var j = 0; j < allDivs.length; j++) {
                                                    var div = allDivs[j];
                                                    if (div.innerText && (div.innerText.includes('We use cookies') || div.innerText.includes('Customize cookies'))) {
                                                        div.style.setProperty('display', 'none', 'important');
                                                        div.style.setProperty('visibility', 'hidden', 'important');
                                                        div.style.setProperty('opacity', '0', 'important');
                                                        div.style.setProperty('pointer-events', 'none', 'important');
                                                    }
                                                }
                                            }

                                            purgeCookieBanner();

                                            if (!window.__cookieObserverSet) {
                                                window.__cookieObserverSet = true;
                                                var count = 0;
                                                var interval = setInterval(function() {
                                                    purgeCookieBanner();
                                                    count++;
                                                    if (count > 50) clearInterval(interval);
                                                }, 200);

                                                if (window.MutationObserver) {
                                                    var observer = new MutationObserver(function() {
                                                        purgeCookieBanner();
                                                    });
                                                    observer.observe(document.body || document.documentElement, { childList: true, subtree: true });
                                                }
                                            }
                                        })();
                                    """.trimIndent(), null)
                                }

                                webChromeClient = object : android.webkit.WebChromeClient() {
                                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                        super.onProgressChanged(view, newProgress)
                                        if (newProgress > 20) {
                                            injectAntiCookieScript(view)
                                        }
                                    }
                                }

                                webViewClient = object : WebViewClient() {
                                    override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                        super.onPageStarted(view, url, favicon)
                                        injectAntiCookieScript(view)
                                    }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        injectAntiCookieScript(view)
                                    }

                                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                        val url = request?.url?.toString() ?: return false
                                        return handleRedirect(url)
                                    }

                                    @Suppress("DEPRECATION")
                                    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                                        if (url == null) return false
                                        return handleRedirect(url)
                                    }

                                    private fun handleRedirect(url: String): Boolean {
                                        if (url.contains("cloudvideoplayer://oauth") || url.contains("access_token=")) {
                                            val uri = Uri.parse(url)
                                            var token = uri.getQueryParameter("access_token")
                                            if (token == null) {
                                                val fragment = uri.fragment
                                                if (fragment != null) {
                                                    for (param in fragment.split("&")) {
                                                        val kv = param.split("=")
                                                        if (kv.size == 2 && kv[0] == "access_token") {
                                                            token = kv[1]
                                                            break
                                                        }
                                                    }
                                                }
                                            }
                                            if (!token.isNullOrBlank()) {
                                                sharedPreferences.edit().putString("dropbox_access_token", token.trim()).apply()
                                                showWebViewLogin = false
                                                onLoginSuccess()
                                                return true
                                            }
                                        }
                                        return false
                                    }
                                }
                                val authUrl = "https://www.dropbox.com/oauth2/authorize?client_id=$appKey&response_type=token&redirect_uri=cloudvideoplayer://oauth&scope=files.metadata.read%20files.content.read"
                                loadUrl(authUrl)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showWebViewLogin = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
