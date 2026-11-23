package com.sylesstacks.mobile

import android.app.Activity
import android.content.Intent
import android.content.pm.ResolveInfo
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private val backgroundColor = Color.rgb(12, 15, 28)
    private val panelColor = Color.rgb(24, 29, 49)
    private val accentColor = Color.rgb(126, 92, 255)
    private lateinit var appList: LinearLayout
    private lateinit var searchField: EditText
    private var installedApps: List<ResolveInfo> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildScreen()
        loadApps()
    }

    override fun onResume() {
        super.onResume()
        if (::searchField.isInitialized) loadApps()
    }

    private fun buildScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(16))
            setBackgroundColor(backgroundColor)
        }

        root.addView(TextView(this).apply {
            text = "SylesStacks"
            textSize = 27f
            setTextColor(Color.WHITE)
            typeface = Typeface.create("sans-serif-black", Typeface.BOLD)
        })

        root.addView(TextView(this).apply {
            text = "MOBILE  •  APP LAUNCHER"
            textSize = 11f
            letterSpacing = 0.12f
            setTextColor(Color.rgb(158, 164, 190))
            setPadding(0, dp(3), 0, dp(20))
        })

        root.addView(TextView(this).apply {
            text = "Мои приложения"
            textSize = 20f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
        })

        searchField = EditText(this).apply {
            hint = "Поиск приложений"
            textSize = 15f
            singleLine = true
            setTextColor(Color.WHITE)
            setHintTextColor(Color.rgb(145, 151, 176))
            background = rounded(panelColor, 14)
            setPadding(dp(15), 0, dp(15), 0)
        }
        root.addView(searchField, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(50)
        ).apply { topMargin = dp(14); bottomMargin = dp(14) })

        val scroll = ScrollView(this).apply { isFillViewport = true }
        appList = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(appList)
        root.addView(scroll, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ))

        root.addView(TextView(this).apply {
            text = "Этап 1 • Запуск установленных приложений"
            textSize = 11f
            setTextColor(Color.rgb(130, 137, 164))
            gravity = Gravity.CENTER
            setPadding(0, dp(12), 0, 0)
        })

        searchField.addTextChangedListener(SimpleTextWatcher { renderApps(it) })
        setContentView(root)
    }

    private fun loadApps() {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        installedApps = packageManager.queryIntentActivities(intent, 0)
            .filter { it.activityInfo.packageName != packageName }
            .sortedBy { it.loadLabel(packageManager).toString().lowercase() }
        renderApps(searchField.text.toString())
    }

    private fun renderApps(query: String) {
        appList.removeAllViews()
        val filtered = installedApps.filter {
            it.loadLabel(packageManager).toString().contains(query, ignoreCase = true)
        }

        if (filtered.isEmpty()) {
            appList.addView(TextView(this).apply {
                text = if (installedApps.isEmpty()) "Приложения не найдены." else "Ничего не найдено."
                textSize = 14f
                setTextColor(Color.LTGRAY)
                gravity = Gravity.CENTER
                setPadding(dp(12), dp(30), dp(12), dp(30))
            })
            return
        }

        filtered.forEach { info ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = rounded(panelColor, 16)
                setPadding(dp(13), dp(10), dp(13), dp(10))
            }
            row.addView(ImageView(this).apply {
                setImageDrawable(info.loadIcon(packageManager))
                scaleType = ImageView.ScaleType.FIT_CENTER
            }, LinearLayout.LayoutParams(dp(42), dp(42)))

            row.addView(TextView(this).apply {
                text = info.loadLabel(packageManager)
                textSize = 15f
                setTextColor(Color.WHITE)
                typeface = Typeface.DEFAULT_BOLD
                maxLines = 2
                setPadding(dp(12), 0, dp(8), 0)
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

            row.addView(TextView(this).apply {
                text = "ОТКРЫТЬ"
                textSize = 10f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                background = rounded(accentColor, 10)
                setPadding(dp(12), dp(10), dp(12), dp(10))
            })

            row.setOnClickListener { launchApp(info) }
            row.getChildAt(2).setOnClickListener { launchApp(info) }
            appList.addView(row, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(9) })
        }
    }

    private fun launchApp(info: ResolveInfo) {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            setClassName(info.activityInfo.packageName, info.activityInfo.name)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(this, "Не удалось открыть приложение", Toast.LENGTH_SHORT).show()
        }
    }

    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
