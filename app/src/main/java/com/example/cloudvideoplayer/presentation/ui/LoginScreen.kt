package com.example.cloudvideoplayer.presentation.ui

import android.content.Intent
import android.net.Uri
import android.view.KeyEvent as AndroidKeyEvent
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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

    val authUrl = "https://www.dropbox.com/oauth2/authorize?client_id=$appKey&response_type=token&redirect_uri=cloudvideoplayer://oauth&scope=files.metadata.read%20files.content.read"

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

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = {
                    showWebViewLogin = true
                }
            ) {
                Text("Login with Dropbox (In-App)")
            }
            OutlinedButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(authUrl))
                    context.startActivity(intent)
                }
            ) {
                Text("Open in Browser")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
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
                Box(modifier = Modifier.fillMaxWidth().height(480.dp)) {
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.setNeedInitialFocus(true)
                                isFocusable = true
                                isFocusableInTouchMode = true

                                // Desktop user agent prevents Dropbox mobile one-tap cookie overlays
                                settings.userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

                                CookieManager.getInstance().setAcceptCookie(true)
                                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                                val injectTvSpatialNavAndAntiCookie = { view: WebView? ->
                                    view?.evaluateJavascript("""
                                        (function() {
                                            try {
                                                if (!document.getElementById('tv-anti-cookie-css')) {
                                                    var style = document.createElement('style');
                                                    style.id = 'tv-anti-cookie-css';
                                                    style.innerHTML = ' [id*="cookie" i], [class*="cookie" i], [id*="onetrust" i], [class*="onetrust" i], [id*="consent" i], [class*="consent" i], .onetrust-pc-dark-filter, div[role="dialog"] { display: none !important; visibility: hidden !important; opacity: 0 !important; pointer-events: none !important; height: 0px !important; width: 0px !important; } input, button, a { outline-offset: 3px !important; } .tv-focused { outline: 4px solid #00E5FF !important; background-color: rgba(0,229,255,0.2) !important; transform: scale(1.03) !important; transition: all 0.15s ease !important; } ';
                                                    (document.head || document.documentElement).appendChild(style);
                                                }
                                            } catch(e) {}

                                            if (!window.__tvFocusManagerInjected) {
                                                window.__tvFocusManagerInjected = true;
                                                var currentIndex = -1;

                                                function getFocusables() {
                                                    var selector = 'input:not([type="hidden"]), button, a, div[role="button"], [tabindex="0"], iframe, div[class*="google" i], button[class*="google" i], div[id*="google" i]';
                                                    return Array.from(document.querySelectorAll(selector))
                                                                .filter(function(el) { return el.offsetWidth > 0 && el.offsetHeight > 0 && window.getComputedStyle(el).display !== 'none'; });
                                                }

                                                function setFocus(index) {
                                                    var focusables = getFocusables();
                                                    if (focusables.length === 0) return;
                                                    if (currentIndex >= 0 && currentIndex < focusables.length) {
                                                        focusables[currentIndex].classList.remove('tv-focused');
                                                    }
                                                    if (index < 0) index = 0;
                                                    if (index >= focusables.length) index = focusables.length - 1;
                                                    currentIndex = index;
                                                    var target = focusables[currentIndex];
                                                    target.classList.add('tv-focused');
                                                    try {
                                                        target.focus();
                                                    } catch(err) {}
                                                    target.scrollIntoView({ block: 'center', behavior: 'smooth' });
                                                }

                                                window.addEventListener('keydown', function(e) {
                                                    var focusables = getFocusables();
                                                    if (focusables.length === 0) return;

                                                    if (e.key === 'ArrowDown') {
                                                        e.preventDefault();
                                                        setFocus(currentIndex + 1);
                                                    } else if (e.key === 'ArrowUp') {
                                                        e.preventDefault();
                                                        setFocus(currentIndex - 1);
                                                    } else if (e.key === 'Enter' || e.key === ' ') {
                                                        if (currentIndex >= 0 && currentIndex < focusables.length) {
                                                            var el = focusables[currentIndex];
                                                            if (el.tagName !== 'INPUT') {
                                                                el.click();
                                                                try {
                                                                    var ev = new MouseEvent('click', { bubbles: true, cancelable: true, view: window });
                                                                    el.dispatchEvent(ev);
                                                                } catch(err) {}
                                                            }
                                                        }
                                                    }
                                                }, true);

                                                setTimeout(function() {
                                                    setFocus(0);
                                                }, 1000);
                                            }
                                        })();
                                    """.trimIndent(), null)
                                }

                                setOnKeyListener { _, keyCode, event ->
                                    if (event.action == AndroidKeyEvent.ACTION_DOWN) {
                                        when (keyCode) {
                                            AndroidKeyEvent.KEYCODE_DPAD_DOWN -> {
                                                evaluateJavascript("window.dispatchEvent(new KeyboardEvent('keydown', {key: 'ArrowDown'}));", null)
                                                true
                                            }
                                            AndroidKeyEvent.KEYCODE_DPAD_UP -> {
                                                evaluateJavascript("window.dispatchEvent(new KeyboardEvent('keydown', {key: 'ArrowUp'}));", null)
                                                true
                                            }
                                            AndroidKeyEvent.KEYCODE_DPAD_CENTER, AndroidKeyEvent.KEYCODE_ENTER, AndroidKeyEvent.KEYCODE_NUMPAD_ENTER -> {
                                                evaluateJavascript("window.dispatchEvent(new KeyboardEvent('keydown', {key: 'Enter'}));", null)
                                                true
                                            }
                                            else -> false
                                        }
                                    } else {
                                        false
                                    }
                                }

                                webChromeClient = object : android.webkit.WebChromeClient() {
                                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                        super.onProgressChanged(view, newProgress)
                                        if (newProgress > 10) {
                                            injectTvSpatialNavAndAntiCookie(view)
                                        }
                                    }
                                }

                                webViewClient = object : WebViewClient() {
                                    override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                        super.onPageStarted(view, url, favicon)
                                        injectTvSpatialNavAndAntiCookie(view)
                                    }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        injectTvSpatialNavAndAntiCookie(view)
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
