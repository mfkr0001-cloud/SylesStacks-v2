
package com.sylesstacks.mobile

import android.app.Activity
import android.app.ActivityManager
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.PowerManager
import android.provider.Settings
import android.util.Base64
import android.view.Display
import android.webkit.DownloadListener
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream

class MainActivity : Activity() {

    private lateinit var webView: WebView
    private var fileCallback: ValueCallback<Array<Uri>>? = null

    private val websiteUrl =
        "https://mfkr0001-cloud.github.io/SylesStacks-v2/"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this)

        setContentView(
            FrameLayout(this).apply {
                addView(
                    webView,
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                )
            }
        )

        configureWebView()
        webView.loadUrl(websiteUrl)
    }

    private fun configureWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            allowFileAccess = false
            allowContentAccess = true
            javaScriptCanOpenWindowsAutomatically = false
            setSupportMultipleWindows(false)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                safeBrowsingEnabled = true
            }
        }

        webView.addJavascriptInterface(
            AndroidBridge(),
            "SylesStacksNative"
        )

        webView.webViewClient = object : WebViewClient() {

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val url = request?.url ?: return false

                if (url.host == "mfkr0001-cloud.github.io") {
                    return false
                }

                return try {
                    startActivity(Intent(Intent.ACTION_VIEW, url))
                    true
                } catch (_: Exception) {
                    false
                }
            }

            override fun onPageFinished(
                view: WebView?,
                url: String?
            ) {
                super.onPageFinished(view, url)
                sendInstalledAppsToPage()
            }
        }

        webView.webChromeClient = object : WebChromeClient() {

            override fun onShowFileChooser(
                webView: WebView?,
                callback: ValueCallback<Array<Uri>>?,
                params: FileChooserParams?
            ): Boolean {
                fileCallback?.onReceiveValue(null)
                fileCallback = callback

                return try {
                    val intent = params?.createIntent()
                        ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                            type = "*/*"
                        }

                    startActivityForResult(intent, FILE_PICKER_REQUEST)
                    true
                } catch (_: Exception) {
                    fileCallback?.onReceiveValue(null)
                    fileCallback = null
                    false
                }
            }
        }

        webView.setDownloadListener(
            DownloadListener { url, _, contentDisposition, mimeType, _ ->
                try {
                    val request = DownloadManager.Request(Uri.parse(url))

                    request.setMimeType(mimeType)
                    request.setTitle(
                        contentDisposition ?: "SylesStacks download"
                    )
                    request.setDescription("Downloading file...")
                    request.setNotificationVisibility(
                        DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                    )
                    request.setDestinationInExternalPublicDir(
                        Environment.DIRECTORY_DOWNLOADS,
                        "SylesStacks-${System.currentTimeMillis()}.apk"
                    )

                    val manager =
                        getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

                    manager.enqueue(request)

                    Toast.makeText(
                        this,
                        "Загрузка началась",
                        Toast.LENGTH_SHORT
                    ).show()
                } catch (_: Exception) {
                    Toast.makeText(
                        this,
                        "Не удалось начать загрузку",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }

    private fun sendInstalledAppsToPage() {
        if (!::webView.isInitialized) return

        val apps = JSONArray()

        try {
            val intent = Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)

            val activities = packageManager.queryIntentActivities(intent, 0)
            val ownPackage = packageName

            val sorted = activities
                .filter { it.activityInfo.packageName != ownPackage }
                .distinctBy { it.activityInfo.packageName }
                .sortedBy {
                    it.loadLabel(packageManager).toString().lowercase()
                }

            for (info in sorted) {
                val appPackage = info.activityInfo.packageName
                val appName = info.loadLabel(packageManager).toString()
                val icon = info.loadIcon(packageManager)

                val isGame =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        try {
                            val applicationInfo =
                                packageManager.getApplicationInfo(appPackage, 0)

                            applicationInfo.category ==
                                ApplicationInfo.CATEGORY_GAME
                        } catch (_: Exception) {
                            false
                        }
                    } else {
                        false
                    } || appPackage in listOf(
                        "com.supercell.brawlstars",
                        "com.axlebolt.standoff2",
                        "com.robtopx.geometryjump",
                        "com.supercell.clashroyale"
                    )

                val bitmap = drawableToBitmap(icon)
                val output = ByteArrayOutputStream()

                bitmap.compress(
                    Bitmap.CompressFormat.PNG,
                    100,
                    output
                )

                val base64 = Base64.encodeToString(
                    output.toByteArray(),
                    Base64.NO_WRAP
                )

                apps.put(
                    JSONObject().apply {
                        put("name", appName)
                        put("packageName", appPackage)
                        put("icon", "data:image/png;base64,$base64")
                        put("isGame", isGame)
                    }
                )
            }
        } catch (_: Exception) {
        }

        val json = apps.toString()

        webView.evaluateJavascript(
            "window.SylesStacksInstalledApps = $json;" +
                "if(window.SylesStacksRefreshApps){" +
                "window.SylesStacksRefreshApps();" +
                "}",
            null
        )
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        val width = 96
        val height = 96

        val bitmap = Bitmap.createBitmap(
            width,
            height,
            Bitmap.Config.ARGB_8888
        )

        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, width, height)
        drawable.draw(canvas)

        return bitmap
    }

    private fun openSystemScreen(intent: Intent, errorMessage: String) {
        runOnUiThread {
            try {
                startActivity(intent)
            } catch (_: Exception) {
                Toast.makeText(
                    this,
                    errorMessage,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    inner class AndroidBridge {

        @JavascriptInterface
        fun launchPackage(packageName: String) {
            runOnUiThread {
                if (!packageName.matches(Regex("[a-zA-Z0-9_.]+"))) {
                    return@runOnUiThread
                }

                try {
                    val launchIntent =
                        packageManager.getLaunchIntentForPackage(packageName)

                    if (launchIntent != null) {
                        startActivity(launchIntent)
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            "Приложение не найдено",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (_: Exception) {
                    Toast.makeText(
                        this@MainActivity,
                        "Не удалось запустить приложение",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        @JavascriptInterface
        fun openAppSettings() {
            openPackageSettings(packageName)
        }

        @JavascriptInterface
        fun openPackageSettings(targetPackage: String) {
            if (!targetPackage.matches(Regex("[a-zA-Z0-9_.]+"))) {
                return
            }

            openSystemScreen(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:$targetPackage")
                },
                "Не удалось открыть настройки приложения"
            )
        }

        @JavascriptInterface
        fun openBatterySettings() {
            openSystemScreen(
                Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS),
                "Настройки энергосбережения недоступны"
            )
        }

        @JavascriptInterface
        fun openDisplaySettings() {
            openSystemScreen(
                Intent(Settings.ACTION_DISPLAY_SETTINGS),
                "Настройки экрана недоступны"
            )
        }

        @JavascriptInterface
        fun getDisplayModes(): String {
            return try {
                val display = windowManager.defaultDisplay
                val modes = JSONArray()

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val supportedModes = display.supportedModes
                        .sortedBy { it.refreshRate }

                    for (mode in supportedModes) {
                        modes.put(JSONObject().apply {
                            put("id", mode.modeId)
                            put("refreshRate", mode.refreshRate.toDouble())
                            put("width", mode.physicalWidth)
                            put("height", mode.physicalHeight)
                        })
                    }
                }

                JSONObject().apply {
                    put("currentRefreshRate", display.refreshRate.toDouble())
                    put("modes", modes)
                }.toString()
            } catch (e: Exception) {
                JSONObject().apply {
                    put(
                        "error",
                        e.message ?: "Ошибка получения режимов экрана"
                    )
                }.toString()
            }
        }

        @JavascriptInterface
        fun setDisplayRefreshRate(refreshRate: Double): String {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                return JSONObject().apply {
                    put("success", false)
                    put("message", "Требуется Android 6.0 или новее")
                }.toString()
            }

            return try {
                val display = windowManager.defaultDisplay

                if (refreshRate <= 0.0) {
                    runOnUiThread {
                        val params = window.attributes
                        params.preferredDisplayModeId = 0
                        window.attributes = params
                    }

                    return JSONObject().apply {
                        put("success", true)
                        put("message", "Автоматический режим")
                    }.toString()
                }

                val selectedMode = display.supportedModes
                    .minByOrNull {
                        kotlin.math.abs(
                            it.refreshRate.toDouble() - refreshRate
                        )
                    }

                if (
                    selectedMode == null ||
                    kotlin.math.abs(
                        selectedMode.refreshRate.toDouble() - refreshRate
                    ) > 1.0
                ) {
                    return JSONObject().apply {
                        put("success", false)
                        put("message", "Такая частота не поддерживается")
                    }.toString()
                }

                runOnUiThread {
                    val params = window.attributes
                    params.preferredDisplayModeId = selectedMode.modeId
                    window.attributes = params
                }

                JSONObject().apply {
                    put("success", true)
                    put(
                        "message",
                        "Запрошено ${selectedMode.refreshRate} Hz"
                    )
                    put(
                        "refreshRate",
                        selectedMode.refreshRate.toDouble()
                    )
                }.toString()

            } catch (e: Exception) {
                JSONObject().apply {
                    put("success", false)
                    put(
                        "message",
                        e.message ?: "Не удалось изменить частоту"
                    )
                }.toString()
            }
        }

        @JavascriptInterface
        fun getDeviceStats(): String {
            return try {
                val result = JSONObject()

                val activityManager =
                    getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

                val memoryInfo = ActivityManager.MemoryInfo()
                activityManager.getMemoryInfo(memoryInfo)

                result.put(
                    "totalRamMb",
                    memoryInfo.totalMem / (1024 * 1024)
                )
                result.put(
                    "availableRamMb",
                    memoryInfo.availMem / (1024 * 1024)
                )
                result.put("lowMemory", memoryInfo.lowMemory)

                val batteryIntent = registerReceiver(
                    null,
                    IntentFilter(Intent.ACTION_BATTERY_CHANGED)
                )

                if (batteryIntent != null) {
                    val level = batteryIntent.getIntExtra(
                        BatteryManager.EXTRA_LEVEL,
                        -1
                    )
                    val scale = batteryIntent.getIntExtra(
                        BatteryManager.EXTRA_SCALE,
                        -1
                    )

                    val percent =
                        if (level >= 0 && scale > 0) {
                            level * 100 / scale
                        } else {
                            -1
                        }

                    result.put("batteryPercent", percent)
                } else {
                    result.put("batteryPercent", -1)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val powerManager =
                        getSystemService(Context.POWER_SERVICE) as PowerManager

                    val thermalStatus = powerManager.currentThermalStatus

                    val thermalName = when (thermalStatus) {
                        PowerManager.THERMAL_STATUS_NONE -> "Норма"
                        PowerManager.THERMAL_STATUS_LIGHT -> "Низкая нагрузка"
                        PowerManager.THERMAL_STATUS_MODERATE -> "Умеренная"
                        PowerManager.THERMAL_STATUS_SEVERE -> "Высокая"
                        PowerManager.THERMAL_STATUS_CRITICAL -> "Критическая"
                        PowerManager.THERMAL_STATUS_EMERGENCY -> "Аварийная"
                        PowerManager.THERMAL_STATUS_SHUTDOWN -> "Выключение"
                        else -> "Неизвестно"
                    }

                    result.put("thermalStatus", thermalName)
                    result.put("thermalStatusCode", thermalStatus)
                } else {
                    result.put(
                        "thermalStatus",
                        "Не поддерживается Android"
                    )
                    result.put("thermalStatusCode", -1)
                }

                result.put(
                    "androidVersion",
                    Build.VERSION.RELEASE ?: "Unknown"
                )
                result.put(
                    "deviceModel",
                    "${Build.MANUFACTURER} ${Build.MODEL}"
                )

                result.toString()
            } catch (e: Exception) {
                JSONObject().apply {
                    put(
                        "error",
                        e.message ?: "Не удалось получить данные"
                    )
                }.toString()
            }
        }

        @JavascriptInterface
        fun refreshApps() {
            runOnUiThread {
                sendInstalledAppsToPage()
            }
        }
    }

    override fun onResume() {
        super.onResume()

        if (::webView.isInitialized) {
            webView.postDelayed({
                sendInstalledAppsToPage()
                webView.evaluateJavascript(
                    "if(window.SylesStacksOptimizerRefresh){" +
                        "window.SylesStacksOptimizerRefresh();" +
                        "}",
                    null
                )
            }, 500)
        }
    }

    @Deprecated("Deprecated in Android API")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == FILE_PICKER_REQUEST) {
            val result = if (resultCode == RESULT_OK) {
                WebChromeClient.FileChooserParams.parseResult(
                    resultCode,
                    data
                )
            } else {
                null
            }

            fileCallback?.onReceiveValue(result)
            fileCallback = null
        }
    }

    @Deprecated("Deprecated in Android API")
    override fun onBackPressed() {
        if (!::webView.isInitialized) {
            finish()
            return
        }

        webView.evaluateJavascript(
            "(document.querySelector('.page.active') || {}).id || 'home'"
        ) { result ->
            if (result == "\"home\"") {
                finish()
            } else {
                webView.evaluateJavascript(
                    "if (typeof go === 'function') go('home');",
                    null
                )
            }
        }
    }

    companion object {
        private const val FILE_PICKER_REQUEST = 7001
    }
}
