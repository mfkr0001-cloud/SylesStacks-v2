
package com.sylesstacks.mobile

import android.app.Activity
import android.content.Intent
import android.content.pm.ResolveInfo
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private val bg = Color.rgb(12, 15, 28)
    private val panel = Color.rgb(24, 29, 49)
    private val accent = Color.rgb(126, 92, 255)
    private val muted = Color.rgb(158, 164, 190)

    private lateinit var content: LinearLayout
    private lateinit var search: EditText

    private var installedApps: List<ResolveInfo> = emptyList()
    private var currentTab = "store"
    private var category = "all"
    private var query = ""

    private data class StoreItem(
        val name: String,
        val developer: String,
        val category: String,
        val rating: String,
        val label: String,
        val emoji: String,
        val color: Int,
        val packageName: String,
        val description: String
    )

    private val items = listOf(
        StoreItem("Brawl Stars", "Supercell", "games", "4.3", "Экшен", "⭐",
            Color.rgb(239, 105, 53), "com.supercell.brawlstars",
            "Динамичные командные сражения и разные игровые режимы."),

        StoreItem("Standoff 2", "AXLEBOLT LTD", "games", "4.2", "Шутер", "🎯",
            Color.rgb(54, 91, 139), "com.axlebolt.standoff2",
            "Многопользовательский тактический шутер."),

        StoreItem("Geometry Dash", "RobTop Games", "games", "4.6", "Аркада", "🟩",
            Color.rgb(20, 153, 112), "com.robtopx.geometryjump",
            "Ритмичный платформер с редактором уровней."),

        StoreItem("Clash Royale", "Supercell", "games", "4.2", "Стратегия", "👑",
            Color.rgb(65, 111, 190), "com.supercell.clashroyale",
            "Карточные тактические битвы."),

        StoreItem("YouTube", "Google LLC", "apps", "4.1", "Видео", "▶",
            Color.rgb(205, 30, 48), "com.google.android.youtube",
            "Видео, каналы и трансляции."),

        StoreItem("Telegram", "Telegram FZ-LLC", "apps", "4.2", "Общение", "➤",
            Color.rgb(22, 135, 201), "org.telegram.messenger",
            "Мессенджер для сообщений и групп."),

        StoreItem("Spotify", "Spotify AB", "apps", "4.4", "Музыка", "♫",
            Color.rgb(16, 135, 75), "com.spotify.music",
            "Музыка и подкасты."),

        StoreItem("Google Фото", "Google LLC", "apps", "4.4", "Фото", "✿",
            Color.rgb(75, 158, 241), "com.google.android.apps.photos",
            "Фотографии и инструменты организации.")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        refreshInstalled()
        buildScreen()
    }

    override fun onResume() {
        super.onResume()
        if (::content.isInitialized) {
            refreshInstalled()
            showCurrentPage()
        }
    }

    private fun buildScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(22), dp(18), dp(14))
            setBackgroundColor(bg)
        }

        root.addView(TextView(this).apply {
            text = "SylesStacks"
            textSize = 27f
            setTextColor(Color.WHITE)
            typeface = Typeface.create("sans-serif-black", Typeface.BOLD)
        })

        root.addView(TextView(this).apply {
            text = "MOBILE  •  APP STORE"
            textSize = 11f
            letterSpacing = 0.12f
            setTextColor(muted)
            setPadding(0, dp(3), 0, dp(16))
        })

        val tabs = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = rounded(panel, 14)
            setPadding(dp(4), dp(4), dp(4), dp(4))
        }

        tabs.addView(tabButton("Магазин", "store"),
            LinearLayout.LayoutParams(0, dp(44), 1f))

        tabs.addView(tabButton("Мои приложения", "apps"),
            LinearLayout.LayoutParams(0, dp(44), 1f))

        root.addView(tabs,
            LinearLayout.LayoutParams(-1, dp(52)).apply {
                bottomMargin = dp(12)
            })

        search = EditText(this).apply {
            hint = "Поиск приложений и игр"
            textSize = 14f
            setSingleLine(true)
            setTextColor(Color.WHITE)
            setHintTextColor(muted)
            background = rounded(panel, 14)
            setPadding(dp(14), 0, dp(14), 0)
        }

        root.addView(search,
            LinearLayout.LayoutParams(-1, dp(48)).apply {
                bottomMargin = dp(12)
            })

        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?, start: Int, count: Int, after: Int
            ) {}

            override fun onTextChanged(
                s: CharSequence?, start: Int, before: Int, count: Int
            ) {
                query = s?.toString().orEmpty()
                showCurrentPage()
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        val scroll = ScrollView(this).apply {
            isFillViewport = true
        }

        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        scroll.addView(content)

        root.addView(scroll,
            LinearLayout.LayoutParams(-1, 0, 1f))

        setContentView(root)
        showCurrentPage()
    }

    private fun tabButton(label: String, tab: String): TextView {
        return TextView(this).apply {
            text = label
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            background = rounded(
                if (currentTab == tab) accent else panel, 11
            )

            setOnClickListener {
                if (currentTab != tab) {
                    currentTab = tab
                    query = ""
                    buildScreen()
                }
            }
        }
    }

    private fun showCurrentPage() {
        if (!::content.isInitialized) return

        content.removeAllViews()

        if (currentTab == "store") {
            buildStorePage()
        } else {
            buildAppsPage()
        }
    }

    private fun buildStorePage() {
        content.addView(heading("Магазин"))

        val categories = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val categoryNames = listOf(
            "Для вас" to "all",
            "Игры" to "games",
            "Приложения" to "apps",
            "Популярное" to "popular"
        )

        categoryNames.forEach { (label, value) ->
            val button = TextView(this).apply {
                text = label
                textSize = 12f
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
                background = rounded(
                    if (category == value) accent else panel, 18
                )
                setPadding(dp(13), dp(9), dp(13), dp(9))

                setOnClickListener {
                    category = value
                    showCurrentPage()
                }
            }

            categories.addView(button,
                LinearLayout.LayoutParams(-2, dp(38)).apply {
                    rightMargin = dp(7)
                })
        }

        val categoryScroll = android.widget.HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            addView(categories)
        }

        content.addView(categoryScroll,
            LinearLayout.LayoutParams(-1, dp(44)).apply {
                bottomMargin = dp(10)
            })

        val filtered = items.filter { item ->
            val categoryMatch =
                category == "all" ||
                item.category == category ||
                (category == "popular" &&
                    (item.rating.toFloatOrNull() ?: 0f) >= 4.3f)

            val searchMatch = query.isBlank() ||
                "${item.name} ${item.developer} ${item.label} ${item.description}"
                    .contains(query, ignoreCase = true)

            categoryMatch && searchMatch
        }

        if (filtered.isEmpty()) {
            content.addView(emptyMessage("Ничего не найдено."))
            return
        }

        filtered.forEach {
            content.addView(storeCard(it))
        }
    }

    private fun storeCard(item: StoreItem): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = rounded(panel, 16)
            setPadding(dp(12), dp(12), dp(12), dp(12))
            isClickable = true
            isFocusable = true
            setOnClickListener { showDetails(item) }
        }

        row.addView(TextView(this).apply {
            text = item.emoji
            textSize = 25f
            gravity = Gravity.CENTER
            background = rounded(item.color, 13)
        }, LinearLayout.LayoutParams(dp(54), dp(54)))

        val info = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, dp(5), 0)
        }

        info.addView(TextView(this).apply {
            text = item.name
            textSize = 15f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
        })

        info.addView(TextView(this).apply {
            text = item.developer
            textSize = 12f
            setTextColor(muted)
            setPadding(0, dp(3), 0, dp(4))
        })

        info.addView(TextView(this).apply {
            text = "★ ${item.rating}  ·  ${item.label}"
            textSize = 11f
            setTextColor(Color.rgb(205, 198, 255))
        })

        row.addView(info,
            LinearLayout.LayoutParams(0, -2, 1f))

        row.addView(TextView(this).apply {
            text = "›"
            textSize = 27f
            setTextColor(muted)
            gravity = Gravity.CENTER
        })

        row.layoutParams = LinearLayout.LayoutParams(-1, -2).apply {
            bottomMargin = dp(9)
        }

        return row
    }

    private fun showDetails(item: StoreItem) {
        content.removeAllViews()

        content.addView(actionText("‹  Назад") {
            showCurrentPage()
        })

        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(18), 0, dp(18))
        }

        hero.addView(TextView(this).apply {
            text = item.emoji
            textSize = 35f
            gravity = Gravity.CENTER
            background = rounded(item.color, 18)
        }, LinearLayout.LayoutParams(dp(76), dp(76)))

        val details = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), 0, 0, 0)
        }

        details.addView(heading(item.name))

        details.addView(TextView(this).apply {
            text = item.developer
            textSize = 13f
            setTextColor(muted)
        })

        details.addView(TextView(this).apply {
            text = "★ ${item.rating}  ·  ${item.label}"
            textSize = 12f
            setTextColor(Color.rgb(205, 198, 255))
            setPadding(0, dp(5), 0, 0)
        })

        hero.addView(details)
        content.addView(hero)

        val installed = isInstalled(item.packageName)

        content.addView(
            actionButton(
                if (installed) "ОТКРЫТЬ"
                else "ОТКРЫТЬ В GOOGLE PLAY"
            ) {
                if (installed) {
                    launchPackage(item.packageName)
                } else {
                    openGooglePlay(item.packageName)
                }
            },
            LinearLayout.LayoutParams(-1, dp(48)).apply {
                bottomMargin = dp(18)
            }
        )

        content.addView(sectionTitle("Об этом приложении"))
        content.addView(bodyText(item.description))

        content.addView(sectionTitle("Информация"))
        content.addView(bodyText(
            "Категория: ${item.label}\n" +
            "Разработчик: ${item.developer}\n" +
            "Пакет Android: ${item.packageName}"
        ))

        content.addView(bodyText(
            if (installed)
                "Приложение уже установлено на устройстве."
            else
                "Загрузка доступна на официальной странице Google Play."
        ))
    }

    private fun buildAppsPage() {
        content.addView(heading("Мои приложения"))

        val filtered = installedApps.filter {
            it.loadLabel(packageManager).toString()
                .contains(query, ignoreCase = true)
        }

        if (filtered.isEmpty()) {
            content.addView(emptyMessage(
                if (installedApps.isEmpty())
                    "Запускаемые приложения не найдены."
                else
                    "Ничего не найдено."
            ))
            return
        }

        filtered.forEach { info ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = rounded(panel, 15)
                setPadding(dp(12), dp(10), dp(12), dp(10))
            }

            val icon = ImageView(this).apply {
                setImageDrawable(info.loadIcon(packageManager))
                scaleType = ImageView.ScaleType.FIT_CENTER
            }

            row.addView(icon,
                LinearLayout.LayoutParams(dp(42), dp(42)))

            row.addView(TextView(this).apply {
                text = info.loadLabel(packageManager)
                textSize = 14f
                setTextColor(Color.WHITE)
                typeface = Typeface.DEFAULT_BOLD
                setPadding(dp(12), 0, dp(8), 0)
                maxLines = 2
            }, LinearLayout.LayoutParams(0, -2, 1f))

            row.addView(actionButton("ОТКРЫТЬ") {
                launchApp(info)
            })

            content.addView(row,
                LinearLayout.LayoutParams(-1, -2).apply {
                    bottomMargin = dp(8)
                })
        }
    }

    private fun refreshInstalled() {
        val intent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)

        installedApps = packageManager
            .queryIntentActivities(intent, 0)
            .filter {
                it.activityInfo.packageName != packageName
            }
            .sortedBy {
                it.loadLabel(packageManager).toString().lowercase()
            }
    }

    private fun isInstalled(packageName: String): Boolean {
        return installedApps.any {
            it.activityInfo.packageName == packageName
        }
    }

    private fun launchPackage(packageName: String) {
        val intent = packageManager.getLaunchIntentForPackage(packageName)

        if (intent == null) {
            openGooglePlay(packageName)
        } else {
            try {
                startActivity(intent)
            } catch (_: Exception) {
                Toast.makeText(
                    this, "Не удалось открыть приложение",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun launchApp(info: ResolveInfo) {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            setClassName(
                info.activityInfo.packageName,
                info.activityInfo.name
            )
        }

        try {
            startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(
                this, "Не удалось открыть приложение",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun openGooglePlay(packageName: String) {
        val market = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("market://details?id=$packageName")
        )

        try {
            startActivity(market)
        } catch (_: Exception) {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(
                        "https://play.google.com/store/apps/details?id=$packageName"
                    )
                )
            )
        }
    }

    private fun heading(text: String) = TextView(this).apply {
        this.text = text
        textSize = 20f
        setTextColor(Color.WHITE)
        typeface = Typeface.DEFAULT_BOLD
        setPadding(0, dp(4), 0, dp(12))
    }

    private fun sectionTitle(text: String) = TextView(this).apply {
        this.text = text
        textSize = 16f
        setTextColor(Color.WHITE)
        typeface = Typeface.DEFAULT_BOLD
        setPadding(0, dp(15), 0, dp(7))
    }

    private fun bodyText(text: String) = TextView(this).apply {
        this.text = text
        textSize = 13f
        setTextColor(muted)
        setPadding(0, 0, 0, dp(8))
    }

    private fun emptyMessage(text: String) = TextView(this).apply {
        this.text = text
        textSize = 14f
        setTextColor(muted)
        gravity = Gravity.CENTER
        setPadding(dp(12), dp(30), dp(12), dp(30))
    }

    private fun actionText(
        text: String,
        action: () -> Unit
    ) = TextView(this).apply {
        this.text = text
        textSize = 14f
        setTextColor(Color.rgb(205, 198, 255))
        setPadding(0, dp(8), 0, dp(8))
        setOnClickListener { action() }
    }

    private fun actionButton(
        text: String,
        action: () -> Unit
    ) = TextView(this).apply {
        this.text = text
        textSize = 10f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(Color.WHITE)
        gravity = Gravity.CENTER
        background = rounded(accent, 10)
        setPadding(dp(12), dp(11), dp(12), dp(11))
        setOnClickListener { action() }
    }

    private fun rounded(color: Int, radius: Int) =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
        }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
