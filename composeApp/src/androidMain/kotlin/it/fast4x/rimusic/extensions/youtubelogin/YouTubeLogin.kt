package it.fast4x.rimusic.extensions.youtubelogin

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import it.fast4x.innertube.Innertube
import it.fast4x.rimusic.LocalPlayerAwareWindowInsets
import it.fast4x.rimusic.R
import it.fast4x.rimusic.enums.NavRoutes
import it.fast4x.rimusic.ui.components.themed.IconButton
import it.fast4x.rimusic.utils.ytVisitorDataKey
import it.fast4x.rimusic.utils.ytCookieKey
import it.fast4x.rimusic.utils.ytAccountNameKey
import it.fast4x.rimusic.utils.ytAccountEmailKey
import it.fast4x.rimusic.utils.ytAccountChannelHandleKey
import it.fast4x.rimusic.utils.rememberEncryptedPreference
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class, DelicateCoroutinesApi::class)
@Composable
fun YouTubeLogin(
    navController: NavController,
    onLogin: (Boolean) -> Unit
) {

    val scope = rememberCoroutineScope()

    var visitorData by rememberEncryptedPreference(key = ytVisitorDataKey, defaultValue = Innertube.DEFAULT_VISITOR_DATA)
    var cookie by rememberEncryptedPreference(key = ytCookieKey, defaultValue = "")
    var accountName by rememberEncryptedPreference(key = ytAccountNameKey, defaultValue = "")
    var accountEmail by rememberEncryptedPreference(key = ytAccountEmailKey, defaultValue = "")
    var accountChannelHandle by rememberEncryptedPreference(key = ytAccountChannelHandleKey, defaultValue = "")


    var webView: WebView? = null

    AndroidView(
        modifier = Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
            .fillMaxSize(0.8f),
        factory = { context ->
            WebView(context).apply {
                webViewClient = object : WebViewClient() {
                    override fun doUpdateVisitedHistory(view: WebView, url: String, isReload: Boolean) {

                        if (url.startsWith("https://music.youtube.com")) {
                            val cookieManager = CookieManager.getInstance()
                            cookie = cookieManager.getCookie(url)
                            //cookieManager.removeAllCookies(null)
                            //cookieManager.flush()
                            //WebStorage.getInstance().deleteAllData()
                            println("YoutubeLogin Cookie: $cookie")
                            scope.launch {
                                Innertube.accountInfo().onSuccess {
                                    accountName = it.name
                                    accountEmail = it.email.orEmpty()
                                    accountChannelHandle = it.channelHandle.orEmpty()
                                    println("YoutubeLogin webClient AccountInfo: $it")
                                    onLogin(true)
                                }.onFailure {
                                    Timber.e("Error YoutubeLogin: $it.stackTraceToString()")
                                    println("Error YoutubeLogin: ${it.stackTraceToString()}")
                                }
                            }
                        }

                    }

                    override fun onPageFinished(view: WebView, url: String?) {
                        if (url != null) {
                            if (url.startsWith("https://music.youtube.com")) {
                                println("YoutubeLogin webClient onPageFinished inside: $url")
                                val cookieManager = CookieManager.getInstance()
                                cookie = cookieManager.getCookie(url)
                                //cookieManager.removeAllCookies(null)
                                //cookieManager.flush()
                                //WebStorage.getInstance().deleteAllData()
                                println("YoutubeLogin Cookie: $cookie")
                                scope.launch {
                                    Innertube.accountInfo().onSuccess {
                                        accountName = it.name
                                        accountEmail = it.email.orEmpty()
                                        accountChannelHandle = it.channelHandle.orEmpty()
                                        println("YoutubeLogin webClient AccountInfo: $it")
                                        onLogin(true)
                                    }.onFailure {
                                        Timber.e("Error YoutubeLogin: $it.stackTraceToString()")
                                        println("Error YoutubeLogin: ${it.stackTraceToString()}")
                                    }
                                }
                            }
                        }
                        println("YoutubeLogin webClient onPageFinished: $url")
                        loadUrl("javascript:Android.onRetrieveVisitorData(window.yt.config_.VISITOR_DATA)")
                    }
                }
                settings.apply {
                    javaScriptEnabled = true
                    setSupportZoom(true)
                    builtInZoomControls = true
                }
                addJavascriptInterface(object {
                    @JavascriptInterface
                    fun onRetrieveVisitorData(newVisitorData: String?) {
                        if (newVisitorData != null) {
                            visitorData = newVisitorData
                        }
                    }
                }, "Android")
                webView = this
                loadUrl("https://accounts.google.com/ServiceLogin?ltmpl=music&service=youtube&passive=true&continue=https%3A%2F%2Fwww.youtube.com%2Fsignin%3Faction_handle_signin%3Dtrue%26next%3Dhttps%253A%252F%252Fmusic.youtube.com%252F")
            }
        }
    )

    TopAppBar(
        title = { Text("Login to YouTube") },
        navigationIcon = {
            IconButton(
                icon = R.drawable.chevron_back,
                onClick = navController::navigateUp,
                color = Color.White
            )
        }
    )

    BackHandler(enabled = webView?.canGoBack() == true) {
        webView?.goBack()
    }
}
