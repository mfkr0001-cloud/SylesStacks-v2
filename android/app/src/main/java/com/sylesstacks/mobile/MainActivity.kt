package com.sylesstacks.mobile

import android.app.Activity
import android.app.ActivityManager
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
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
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.webkit.CookieManager
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
    private var pendingDownload: PendingDownload? = null
    private var downloadCompletedReceiver: DownloadCompletedReceiver? = null

    private data class PendingDownload(
        val url: String,
        val title: String,
        val mimeType: String?,
        val userAgent: String?
    )

    private val websiteUrl =
        "https://mfkr0001-cloud.github.io/SylesStacks-v2/"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        applyImmersiveFullscreen()

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

        registerDownloadReceiver()
        configureWebView()
        webView.loadUrl(websiteUrl)
    }

    private fun applyImmersiveFullscreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)

            window.insetsController?.let { controller ->
                controller.hide(
                    WindowInsets.Type.statusBars() or
                        WindowInsets.Type.navigationBars()
                )

                controller.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                )
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)

        if (hasFocus) {
            applyImmersiveFullscreen()
        }
    }

    private fun registerDownloadReceiver() {
        val receiver = DownloadCompletedReceiver()
        downloadCompletedReceiver = receiver

        val filter = IntentFilter(
            DownloadManager.ACTION_DOWNLOAD_COMPLETE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(
                receiver,
                filter,
                Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(receiver, filter)
        }
    }

    override fun onDestroy() {
        downloadCompletedReceiver?.let { receiver ->
            try {
                unregisterReceiver(receiver)
            } catch (_: IllegalArgumentException) {
            }
        }

        downloadCompletedReceiver = null

        if (::webView.isInitialized) {
            webView.stopLoading()
            webView.removeJavascriptInterface("SylesStacksNative")
            webView.destroy()
        }

        super.onDestroy()
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
                val scheme = url.scheme?.lowercase()

                if (scheme == "https" || scheme == "http") {
                    return false
                }

                if (scheme == "tel" || scheme == "mailto") {
                    return try {
                        startActivity(Intent(Intent.ACTION_VIEW, url))
                        true
                    } catch (_: Exception) {
                        true
                    }
                }

                return scheme != null
            }

            override fun onPageFinished(
                view: WebView?,
                url: String?
            ) {
                super.onPageFinished(view, url)

                view?.evaluateJavascript(
                    """
                    document.documentElement.classList.add('sylesstacks-android');
                    if (document.body) {
                        document.body.classList.add('sylesstacks-android');
                    }
                    """.trimIndent(),
                    null
                )

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

                    startActivityForResult(
                        intent,
                        FILE_PICKER_REQUEST
                    )

                    true
                } catch (_: Exception) {
                    fileCallback?.onReceiveValue(null)
                    fileCallback = null
                    false
                }
            }
        }

        webView.setDownloadListener(
            DownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
                enqueueDownload(
                    url = url,
                    title = contentDisposition ?: "SylesStacks download",
                    mimeType = mimeType,
                    userAgent = userAgent
                )
            }
        )
    }

    private fun enqueueDownload(
        url: String,
        title: String,
        mimeType: String?,
        userAgent: String? = null
    ): Long? {

        if (
            Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
            checkSelfPermission(
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            pendingDownload = PendingDownload(
                url,
                title,
                mimeType,
                userAgent
            )

            requestPermissions(
                arrayOf(
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
                STORAGE_PERMISSION_REQUEST
            )

            Toast.makeText(
                this,
                "Разреши доступ к файлам для сохранения загрузки",
                Toast.LENGTH_LONG
            ).show()

            return null
        }

        return try {
            val parsed = Uri.parse(url)

            if (
                parsed.scheme != "https" &&
                parsed.scheme != "http"
            ) {
                throw IllegalArgumentException(
                    "Unsupported download URL"
                )
            }

            val safeTitle = title
                .replace(Regex("[\\\\/:*?\"<>|]"), "_")
                .take(100)
                .ifBlank { "SylesStacks download" }

            val apk =
                safeTitle.endsWith(".apk", true) ||
                    parsed.lastPathSegment?.endsWith(".apk", true) == true ||
                    mimeType.equals(
                        "application/vnd.android.package-archive",
                        true
                    )

            val request = DownloadManager.Request(parsed)

            request.setMimeType(
                if (apk) {
                    "application/vnd.android.package-archive"
                } else {
                    mimeType ?: "application/octet-stream"
                }
            )

            request.setTitle(safeTitle)
            request.setDescription("SylesStacks · Загрузка файла")

            request.setNotificationVisibility(
                DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
            )

            request.setAllowedOverMetered(true)
            request.setAllowedOverRoaming(false)

            val cookies =
                CookieManager.getInstance().getCookie(url)

            if (!cookies.isNullOrBlank()) {
                request.addRequestHeader("Cookie", cookies)
            }

            if (!userAgent.isNullOrBlank()) {
                request.addRequestHeader("User-Agent", userAgent)
            }

            val extension = (parsed.lastPathSegment ?: safeTitle)
                .substringAfterLast('.', "")
                .lowercase()
                .takeIf {
                    it.matches(Regex("[a-z0-9]{1,8}")) &&
                        it != "apk"
                }

            val fileName = if (apk) {
                "SylesStacks-${System.currentTimeMillis()}.apk"
            } else {
                "SylesStacks-${System.currentTimeMillis()}" +
                    (extension?.let { ".$it" } ?: "")
            }

            request.setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                fileName
            )

            val manager =
                getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

            val id = manager.enqueue(request)

            if (apk) {
                getSharedPreferences(
                    "sylesstacks_downloads",
                    Context.MODE_PRIVATE
                ).edit()
                    .putBoolean(id.toString(), true)
                    .apply()
            }

            Toast.makeText(
                this,
                "Загрузка началась",
                Toast.LENGTH_SHORT
            ).show()

            id

        } catch (error: Exception) {
            Toast.makeText(
                this,
                "Не удалось начать загрузку: ${error.message ?: "ошибка"}",
                Toast.LENGTH_LONG
            ).show()

            null
        }
    }

    private fun getDownloadInfo(downloadId: Long): JSONObject {
        val manager =
            getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        val cursor = manager.query(
            DownloadManager.Query().setFilterById(downloadId)
        )

        cursor.use {
            if (it == null || !it.moveToFirst()) {
                return JSONObject().put("status", "missing")
            }

            val status = it.getInt(
                it.getColumnIndexOrThrow(
                    DownloadManager.COLUMN_STATUS
                )
            )

            val downloaded = it.getLong(
                it.getColumnIndexOrThrow(
                    DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR
                )
            )

            val total = it.getLong(
                it.getColumnIndexOrThrow(
                    DownloadManager.COLUMN_TOTAL_SIZE_BYTES
                )
            )

            val reason = it.getInt(
                it.getColumnIndexOrThrow(
                    DownloadManager.COLUMN_REASON
                )
            )

            val mime = it.getString(
                it.getColumnIndexOrThrow(
                    DownloadManager.COLUMN_MEDIA_TYPE
                )
            ) ?: ""

            val apkFlag = getSharedPreferences(
                "sylesstacks_downloads",
                Context.MODE_PRIVATE
            ).getBoolean(downloadId.toString(), false)

            val state = when (status) {
                DownloadManager.STATUS_PENDING,
                DownloadManager.STATUS_RUNNING,
                DownloadManager.STATUS_PAUSED -> "running"

                DownloadManager.STATUS_SUCCESSFUL -> "complete"
                DownloadManager.STATUS_FAILED -> "failed"
                else -> "unknown"
            }

            return JSONObject().apply {
                put("status", state)
                put("downloaded", downloaded)
                put("total", total)
                put(
                    "percent",
                    if (total > 0) {
                        downloaded * 100 / total
                    } else {
                        0
                    }
                )
                put("reason", reason)
                put(
                    "isApk",
                    apkFlag || mime.equals(
                        "application/vnd.android.package-archive",
                        true
                    )
                )
            }
        }
    }

    private fun sendInstalledAppsToPage() {
        if (!::webView.isInitialized) return

        val apps = JSONArray()

        try {
            val intent = Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)

            val activities = packageManager.queryIntentActivities(
                intent,
                0
            )

            val sorted = activities
                .filter {
                    it.activityInfo.packageName != packageName
                }
                .distinctBy {
                    it.activityInfo.packageName
                }
                .sortedBy {
                    it.loadLabel(packageManager)
                        .toString()
                        .lowercase()
                }

            for (info in sorted) {
                val appPackage = info.activityInfo.packageName
                val appName = info.loadLabel(packageManager).toString()
                val icon = info.loadIcon(packageManager)

                val isGame =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        try {
                            val applicationInfo =
                                packageManager.getApplicationInfo(
                                    appPackage,
                                    0
                                )

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
                        put(
                            "icon",
                            "data:image/png;base64,$base64"
                        )
                        put("isGame", isGame)
                    }
                )
            }
        } catch (_: Exception) {
        }

        webView.evaluateJavascript(
            "window.SylesStacksInstalledApps = ${apps};" +
                "if(window.SylesStacksRefreshApps){" +
                "window.SylesStacksRefreshApps();" +
                "}",
            null
        )
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        val bitmap = Bitmap.createBitmap(
            96,
            96,
            Bitmap.Config.ARGB_8888
        )

        val canvas = Canvas(bitmap)

        drawable.setBounds(0, 0, 96, 96)
        drawable.draw(canvas)

        return bitmap
    }

    private fun openSystemScreen(
        intent: Intent,
        errorMessage: String
    ) {
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
        fun downloadFile(
            url: String,
            title: String
        ): String {
            val id = enqueueDownload(
                url = url,
                title = title.ifBlank {
                    "SylesStacks download"
                },
                mimeType = if (
                    url.substringBefore("?").endsWith(".apk", true)
                ) {
                    "application/vnd.android.package-archive"
                } else {
                    null
                }
            )

            return if (id != null) {
                JSONObject()
                    .put("success", true)
                    .put("downloadId", id)
                    .toString()
            } else {
                JSONObject()
                    .put("success", false)
                    .toString()
            }
        }

        @JavascriptInterface
        fun getDownloadProgress(downloadId: Long): String {
            return try {
                getDownloadInfo(downloadId).toString()
            } catch (e: Exception) {
                JSONObject()
                    .put("status", "failed")
                    .put("message", e.message ?: "Ошибка")
                    .toString()
            }
        }

        @JavascriptInterface
        fun installDownloadedFile(downloadId: Long) {
            runOnUiThread {
                try {
                    if (
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                        !packageManager.canRequestPackageInstalls()
                    ) {
                        startActivity(
                            Intent(
                                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES
                            ).apply {
                                data = Uri.parse("package:$packageName")
                            }
                        )

                        Toast.makeText(
                            this@MainActivity,
                            "Разреши установку приложений и нажми «Открыть» ещё раз",
                            Toast.LENGTH_LONG
                        ).show()

                        return@runOnUiThread
                    }

                    val manager =
                        getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

                    val uri =
                        manager.getUriForDownloadedFile(downloadId)

                    if (uri == null) {
                        Toast.makeText(
                            this@MainActivity,
                            "Файл ещё не готов",
                            Toast.LENGTH_SHORT
                        ).show()

                        return@runOnUiThread
                    }

                    startActivity(
                        Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(
                                uri,
                                "application/vnd.android.package-archive"
                            )
                            addFlags(
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                            )
                        }
                    )

                } catch (e: Exception) {
                    Toast.makeText(
                        this@MainActivity,
                        "Не удалось открыть установщик: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        @JavascriptInterface
        fun openDownloadedFile(downloadId: Long) {
            runOnUiThread {
                try {
                    val manager =
                        getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

                    val uri =
                        manager.getUriForDownloadedFile(downloadId)

                    if (uri == null) {
                        Toast.makeText(
                            this@MainActivity,
                            "Файл ещё не готов",
                            Toast.LENGTH_SHORT
                        ).show()

                        return@runOnUiThread
                    }

                    val mime = manager.query(
                        DownloadManager.Query().setFilterById(downloadId)
                    ).use { cursor ->
                        if (cursor != null && cursor.moveToFirst()) {
                            cursor.getString(
                                cursor.getColumnIndexOrThrow(
                                    DownloadManager.COLUMN_MEDIA_TYPE
                                )
                            )
                        } else {
                            null
                        }
                    } ?: "application/octet-stream"

                    startActivity(
                        Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, mime)
                            addFlags(
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                            )
                        }
                    )

                } catch (e: Exception) {
                    Toast.makeText(
                        this@MainActivity,
                        "Не удалось открыть файл: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        @JavascriptInterface
        fun deleteDownloadedFile(downloadId: Long): Boolean {
            return try {
                val manager =
                    getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

                val removed = manager.remove(downloadId) > 0

                getSharedPreferences(
                    "sylesstacks_downloads",
                    Context.MODE_PRIVATE
                ).edit()
                    .remove(downloadId.toString())
                    .apply()

                removed
            } catch (_: Exception) {
                false
            }
        }

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
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                ).apply {
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
                    for (mode in display.supportedModes.sortedBy {
                        it.refreshRate
                    }) {
                        modes.put(
                            JSONObject().apply {
                                put("id", mode.modeId)
                                put(
                                    "refreshRate",
                                    mode.refreshRate.toDouble()
                                )
                                put("width", mode.physicalWidth)
                                put("height", mode.physicalHeight)
                            }
                        )
                    }
                }

                JSONObject().apply {
                    put(
                        "currentRefreshRate",
                        display.refreshRate.toDouble()
                    )
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
                    put(
                        "message",
                        "Требуется Android 6.0 или новее"
                    )
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

                val selectedMode = display.supportedModes.minByOrNull {
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
                        put(
                            "message",
                            "Такая частота не поддерживается"
                        )
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

                    result.put(
                        "batteryPercent",
                        if (level >= 0 && scale > 0) {
                            level * 100 / scale
                        } else {
                            -1
                        }
                    )
                } else {
                    result.put("batteryPercent", -1)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val powerManager =
                        getSystemService(Context.POWER_SERVICE) as PowerManager

                    val thermalStatus =
                        powerManager.currentThermalStatus

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
        applyImmersiveFullscreen()

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
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        if (requestCode == STORAGE_PERMISSION_REQUEST) {
            val pending = pendingDownload
            pendingDownload = null

            if (
                grantResults.firstOrNull() ==
                PackageManager.PERMISSION_GRANTED &&
                pending != null
            ) {
                enqueueDownload(
                    pending.url,
                    pending.title,
                    pending.mimeType,
                    pending.userAgent
                )
            } else {
                Toast.makeText(
                    this,
                    "Без разрешения файл не будет сохранён",
                    Toast.LENGTH_LONG
                ).show()
            }
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
        private const val STORAGE_PERMISSION_REQUEST = 7002
    }
}
