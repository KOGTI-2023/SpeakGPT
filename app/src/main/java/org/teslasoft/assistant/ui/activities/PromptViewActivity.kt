/**************************************************************************
 * Copyright (c) 2023-2024 Dmytro Ostapenko. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **************************************************************************/

package org.teslasoft.assistant.ui.activities

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.FragmentActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.elevation.SurfaceColors
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.teslasoft.assistant.Api
import org.teslasoft.assistant.Config
import org.teslasoft.assistant.R
import org.teslasoft.assistant.preferences.Preferences
import org.teslasoft.assistant.ui.assistant.AssistantActivity
import org.teslasoft.core.api.network.RequestNetwork
import java.net.MalformedURLException
import java.net.URL

class PromptViewActivity : FragmentActivity(), SwipeRefreshLayout.OnRefreshListener {

    private var activityTitle: TextView? = null
    private var content: ConstraintLayout? = null
    private var progressBar: CircularProgressIndicator? = null
    private var noInternetLayout: ConstraintLayout? = null
    private var btnReconnect: MaterialButton? = null
    private var btnShowDetails: MaterialButton? = null
    private var promptBy: TextView? = null
    private var promptText: EditText? = null
    private var textCat: TextView? = null
    private var refreshPage: SwipeRefreshLayout? = null
    private var requestNetwork: RequestNetwork? = null
    private var btnCopy: MaterialButton? = null
    private var btnLike: MaterialButton? = null
    private var btnTry: MaterialButton? = null
    private var btnFlag: ImageButton? = null
    private var promptBg: ConstraintLayout? = null
    private var promptActions: ConstraintLayout? = null

    private var id = ""
    private var title = ""
    private var networkError = ""
    private var likeState = false
    private var settings: SharedPreferences? = null
    private var promptFor: String? = null
    private var btnBack: ImageButton? = null
    private var root: ConstraintLayout? = null

    private val dataListener: RequestNetwork.RequestListener = object : RequestNetwork.RequestListener {
        @SuppressLint("SetTextI18n")
        override fun onResponse(tag: String, message: String) {
            noInternetLayout?.visibility = View.GONE
            progressBar?.visibility = View.GONE
            content?.visibility = View.VISIBLE

            try {
                val map: HashMap<String, String> = Gson().fromJson(
                    message, TypeToken.getParameterized(HashMap::class.java, String::class.java, String::class.java).type
                )

                promptText?.setText(map["prompt"])
                promptBy?.text = "By " + map["author"]
                btnLike?.text = map["likes"]
                promptFor = map["type"]

                title = map["name"].toString()
                activityTitle?.text = title

                textCat?.text = when (map["category"]) {
                    "development" -> String.format(resources.getString(R.string.cat), resources.getString(R.string.cat_development))
                    "music" -> String.format(resources.getString(R.string.cat), resources.getString(R.string.cat_music))
                    "art" -> String.format(resources.getString(R.string.cat), resources.getString(R.string.cat_art))
                    "culture" -> String.format(resources.getString(R.string.cat), resources.getString(R.string.cat_culture))
                    "business" -> String.format(resources.getString(R.string.cat), resources.getString(R.string.cat_business))
                    "gaming" -> String.format(resources.getString(R.string.cat), resources.getString(R.string.cat_gaming))
                    "education" -> String.format(resources.getString(R.string.cat), resources.getString(R.string.cat_education))
                    "history" -> String.format(resources.getString(R.string.cat), resources.getString(R.string.cat_history))
                    "health" -> String.format(resources.getString(R.string.cat), resources.getString(R.string.cat_health))
                    "food" -> String.format(resources.getString(R.string.cat), resources.getString(R.string.cat_food))
                    "tourism" -> String.format(resources.getString(R.string.cat), resources.getString(R.string.cat_tourism))
                    "productivity" -> String.format(resources.getString(R.string.cat), resources.getString(R.string.cat_productivity))
                    "tools" -> String.format(resources.getString(R.string.cat), resources.getString(R.string.cat_tools))
                    "entertainment" -> String.format(resources.getString(R.string.cat), resources.getString(R.string.cat_entertainment))
                    "sport" -> String.format(resources.getString(R.string.cat), resources.getString(R.string.cat_sport))
                    else -> String.format(resources.getString(R.string.cat), resources.getString(R.string.cat_uncat))
                }

                networkError = ""
            } catch (e: Exception) {
                networkError = e.printStackTrace().toString()
            }
        }

        override fun onErrorResponse(tag: String, message: String) {
            networkError = message

            noInternetLayout?.visibility = View.VISIBLE
            progressBar?.visibility = View.GONE
            content?.visibility = View.GONE
        }
    }

    private val likeListener: RequestNetwork.RequestListener = object : RequestNetwork.RequestListener {
        override fun onResponse(tag: String, message: String) {
            btnLike?.isEnabled = true
            likeState = true

            settings?.edit()?.putBoolean(id, true)?.apply()
            btnLike?.setIconResource(R.drawable.ic_like)

            loadData()
        }

        override fun onErrorResponse(tag: String, message: String) {
            btnLike?.isEnabled = true

            Toast.makeText(this@PromptViewActivity, getString(R.string.label_sorry_action_failed), Toast.LENGTH_SHORT).show()
        }
    }

    private val dislikeListener: RequestNetwork.RequestListener = object : RequestNetwork.RequestListener {
        override fun onResponse(tag: String, message: String) {
            btnLike?.isEnabled = true
            likeState = false

            settings?.edit()?.putBoolean(id, false)?.apply()
            btnLike?.setIconResource(R.drawable.ic_like_outline)

            loadData()
        }

        override fun onErrorResponse(tag: String, message: String) {
            btnLike?.isEnabled = true

            Toast.makeText(this@PromptViewActivity, getString(R.string.label_sorry_action_failed), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        reloadAmoled()

        // Reset preferences singleton
        Preferences.getPreferences(this, "")
    }

    @Suppress("DEPRECATION")
    private fun reloadAmoled() {
        if (isDarkThemeEnabled() &&  Preferences.getPreferences(this, "").getAmoledPitchBlack()) {
            if (android.os.Build.VERSION.SDK_INT <= 34) {
                window.navigationBarColor = ResourcesCompat.getColor(resources, R.color.amoled_window_background, theme)
                window.statusBarColor = ResourcesCompat.getColor(resources, R.color.amoled_accent_50, theme)
            }

            window.setBackgroundDrawableResource(R.color.amoled_window_background)
            activityTitle?.setBackgroundColor(ResourcesCompat.getColor(resources, R.color.amoled_accent_50, theme))
            promptBg?.setBackgroundResource(R.drawable.btn_accent_24_amoled)
            promptActions?.setBackgroundResource(R.drawable.btn_accent_24_amoled)

            btnBack?.background = getDarkAccentDrawable(
                AppCompatResources.getDrawable(
                    this,
                    R.drawable.btn_accent_tonal_v4_amoled
                )!!, this
            )

            btnFlag?.background = getDarkAccentDrawable(
                AppCompatResources.getDrawable(
                    this,
                    R.drawable.btn_accent_tonal_v4_amoled
                )!!, this
            )
        } else {
            if (android.os.Build.VERSION.SDK_INT <= 34) {
                window.navigationBarColor = SurfaceColors.SURFACE_0.getColor(this)
                window.statusBarColor = SurfaceColors.SURFACE_4.getColor(this)
            }

            val colorDrawable = ColorDrawable(SurfaceColors.SURFACE_0.getColor(this))
            window.setBackgroundDrawable(colorDrawable)
            activityTitle?.setBackgroundColor(SurfaceColors.SURFACE_4.getColor(this))

            promptBg?.background = getDarkDrawable(
                AppCompatResources.getDrawable(
                    this,
                    R.drawable.btn_accent_24
                )!!
            )

            promptActions?.background = getDarkDrawable(
                AppCompatResources.getDrawable(
                    this,
                    R.drawable.btn_accent_24
                )!!
            )

            btnBack?.background = getDarkAccentDrawable(
                AppCompatResources.getDrawable(
                    this,
                    R.drawable.btn_accent_tonal_v4
                )!!, this
            )

            btnFlag?.background = getDarkAccentDrawable(
                AppCompatResources.getDrawable(
                    this,
                    R.drawable.btn_accent_tonal_v4
                )!!, this
            )
        }
    }

    private fun getDarkDrawable(drawable: Drawable) : Drawable {
        DrawableCompat.setTint(DrawableCompat.wrap(drawable), SurfaceColors.SURFACE_1.getColor(this))
        return drawable
    }

    private fun isDarkThemeEnabled(): Boolean {
        return when (resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_YES -> true
            Configuration.UI_MODE_NIGHT_NO -> false
            Configuration.UI_MODE_NIGHT_UNDEFINED -> false
            else -> false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val extras: Bundle? = intent.extras

        if (extras == null) {
            checkForURI()
        } else {
            id = extras.getString("id", "")
            title = extras.getString("title", "")

            this@PromptViewActivity.setTitle(extras.getString("title", ""))

            if (id == "" || title == "") {
                checkForURI()
            } else {
                allowLaunch()
            }
        }
    }

    private fun checkForURI() {
        val uri = intent.data
        try {
            val url = URL(uri?.scheme, uri?.host, uri?.path)

            val paths = url.path.split("/")
            id = paths[paths.size - 1]

            allowLaunch()
        } catch (e: MalformedURLException) {
            Toast.makeText(this, "Invalid URL", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    @Suppress("DEPRECATION")
    private fun allowLaunch() {
        setContentView(R.layout.activity_view_prompt)

        if (android.os.Build.VERSION.SDK_INT <= 34) {
            window.statusBarColor = SurfaceColors.SURFACE_4.getColor(this)
        }

        initUI()

        Thread {
            runOnUiThread {
                initLogic()
            }
        }.start()
    }

    private fun initUI() {
        activityTitle = findViewById(R.id.activity_view_title)
        content = findViewById(R.id.view_content)
        progressBar = findViewById(R.id.progress_bar_view)
        noInternetLayout = findViewById(R.id.no_internet)
        btnReconnect = findViewById(R.id.btn_reconnect)
        btnShowDetails = findViewById(R.id.btn_show_details)
        promptBy = findViewById(R.id.prompt_by)
        promptText = findViewById(R.id.prompt_text)
        refreshPage = findViewById(R.id.refresh_page)
        btnFlag = findViewById(R.id.btn_flag)
        btnCopy = findViewById(R.id.btn_copy)
        btnLike = findViewById(R.id.btn_like)
        btnTry = findViewById(R.id.btn_try)
        textCat = findViewById(R.id.text_cat)
        btnBack = findViewById(R.id.btn_back)
        root = findViewById(R.id.root)
        promptBg = findViewById(R.id.prompt_bg)
        promptActions = findViewById(R.id.prompt_actions)

        noInternetLayout?.visibility = View.GONE
        progressBar?.visibility = View.VISIBLE
        content?.visibility = View.GONE

        reloadAmoled()
    }

    private fun initLogic() {
        activityTitle?.isSelected = true

        btnFlag?.background = getDarkAccentDrawable(
            AppCompatResources.getDrawable(
                this,
                R.drawable.btn_accent_tonal_v4
            )!!, this
        )

        btnBack?.background = getDarkAccentDrawable(
            AppCompatResources.getDrawable(
                this,
                R.drawable.btn_accent_tonal_v4
            )!!, this
        )

        btnFlag?.setImageResource(R.drawable.ic_flag)

        btnBack?.setOnClickListener {
            finish()
        }

        settings = getSharedPreferences("likes", MODE_PRIVATE)

        likeState = settings?.getBoolean(id, false) == true

        refreshPage?.setColorSchemeResources(R.color.accent_900)
        refreshPage?.setProgressBackgroundColorSchemeColor(
            SurfaceColors.SURFACE_2.getColor(this)
        )
        refreshPage?.setSize(SwipeRefreshLayout.LARGE)
        refreshPage?.setOnRefreshListener(this)

        if (likeState) {
            btnLike?.setIconResource(R.drawable.ic_like)
        } else {
            btnLike?.setIconResource(R.drawable.ic_like_outline)
        }

        btnCopy?.setOnClickListener {
            val clipboard: ClipboardManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("prompt", promptText?.text.toString())
            clipboard.setPrimaryClip(clip)

            Toast.makeText(this, getString(R.string.label_copy), Toast.LENGTH_SHORT).show()
        }

        btnLike?.setOnClickListener {
            if (likeState) {
                requestNetwork?.startRequestNetwork("GET", "${Config.API_ENDPOINT}/dislike.php?api_key=${Api.TESLASOFT_API_KEY}&id=$id", "A", dislikeListener)
            } else {
                requestNetwork?.startRequestNetwork("GET", "${Config.API_ENDPOINT}/like.php?api_key=${Api.TESLASOFT_API_KEY}&id=$id", "A", likeListener)
            }

            btnLike?.isEnabled = false
        }

        btnTry?.setOnClickListener {
            if (promptFor == "GPT") {
                val i = Intent(
                    this,
                    AssistantActivity::class.java
                ).setAction(Intent.ACTION_VIEW)
                i.putExtra("prompt", promptText?.text.toString())
                startActivity(i)
            } else {
                val i = Intent(
                    this,
                    AssistantActivity::class.java
                ).setAction(Intent.ACTION_VIEW)
                i.putExtra("prompt", "/imagine " + promptText?.text.toString())
                i.putExtra("FORCE_SLASH_COMMANDS_ENABLED", true)
                startActivity(i)
            }
        }

        btnFlag?.setOnClickListener {
            val i = Intent(this, ReportAbuseActivity::class.java).setAction(Intent.ACTION_VIEW)
            i.putExtra("id", id)
            startActivity(i)
        }

        requestNetwork = RequestNetwork(this)

        activityTitle?.text = title

        btnReconnect?.setOnClickListener { loadData() }

        btnShowDetails?.setOnClickListener {
            MaterialAlertDialogBuilder(this, R.style.App_MaterialAlertDialog)
                .setTitle(R.string.label_error_details)
                .setMessage(networkError)
                .setPositiveButton(R.string.btn_close) { _, _ -> }
                .show()
        }

        loadData()
    }

    private fun getDarkAccentDrawable(drawable: Drawable, context: Context) : Drawable {
        DrawableCompat.setTint(DrawableCompat.wrap(drawable), getSurfaceColor(context))
        return drawable
    }

    private fun getSurfaceColor(context: Context) : Int {
        return if (isDarkThemeEnabled() &&  Preferences.getPreferences(context, "").getAmoledPitchBlack()) {
            ResourcesCompat.getColor(context.resources, R.color.amoled_accent_50, context.theme)
        } else {
            SurfaceColors.SURFACE_4.getColor(context)
        }
    }

    override fun onRefresh() {
        refreshPage?.isRefreshing = false

        loadData()
    }

    private fun loadData() {
        noInternetLayout?.visibility = View.GONE
        progressBar?.visibility = View.VISIBLE
        content?.visibility = View.GONE

        requestNetwork?.startRequestNetwork("GET", "${Config.API_ENDPOINT}/prompt.php?api_key=${Api.TESLASOFT_API_KEY}&id=$id", "A", dataListener)
    }
}
