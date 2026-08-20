/*
 * PupPlay — a touchscreen hunting game for dogs
 * Copyright (C) 2026 nameefef
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.easonyin.dogplay

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.appcompat.widget.SwitchCompat
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: Prefs
    private lateinit var sound: SoundEngine
    private lateinit var haptics: Haptics

    private val dp get() = resources.displayMetrics.density

    private val preyTiles = ArrayList<PreyTile>()
    private val bgTiles = ArrayList<BgTile>()

    private var speedLabel: TextView? = null
    private var countLabel: TextView? = null
    private var sizeLabel: TextView? = null
    private var soundLabel: TextView? = null
    private var scoreLabel: TextView? = null
    private var preyStatus: TextView? = null
    private var bgStatus: TextView? = null
    private var soundStatus: TextView? = null

    private var customPreyBmp: Bitmap? = null
    private var customBgBmp: Bitmap? = null

    // ── 颜色 ──────────────────────────────────────────────
    private val cBg = 0xFF0B1220.toInt()
    private val cCard = 0xFF131A29.toInt()
    private val cText = 0xFFE8EEF8.toInt()
    private val cDim = 0xFF93A0B8.toInt()
    private val cAccent = 0xFFFFE066.toInt()

    // ── 文件选择 ──────────────────────────────────────────
    private val pickPreyImg = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) importTo(uri, prefs.customPreyFile, getString(R.string.asset_name_prey)) {
            reloadCustomBitmaps()
            prefs.preyKey = PreyType.CUSTOM.key
            refreshPreySelection()
            refreshStatus()
        }
    }
    private val pickBgImg = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) importTo(uri, prefs.customBgFile, getString(R.string.asset_name_bg)) {
            reloadCustomBitmaps()
            prefs.bgKey = BgType.CUSTOM.key
            bgTiles.forEach { it.invalidateThumb() }
            refreshBgSelection()
            refreshStatus()
        }
    }
    private val pickSound = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) importTo(uri, prefs.customSoundFile, getString(R.string.asset_name_sound)) {
            prefs.soundChoice = "custom"
            sound.loadCustom(prefs.customSoundFile)
            soundLabel?.text = prefs.soundChoiceLabel
            refreshStatus()
        }
    }

    private val playGame = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        if (res.resultCode == Activity.RESULT_OK) {
            val s = res.data?.getIntExtra("score", 0) ?: 0
            val sec = res.data?.getIntExtra("seconds", 0) ?: 0
            Toast.makeText(this, getString(R.string.toast_result, s, sec / 60, sec % 60),
                Toast.LENGTH_LONG).show()
        }
        refreshScore()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        sound = SoundEngine().apply {
            enabled = prefs.soundEnabled
            volume = prefs.volume / 100f
        }
        haptics = Haptics(this).apply {
            enabled = prefs.hapticEnabled
            strength = prefs.hapticStrength
        }
        if (prefs.hasCustomSound()) sound.loadCustom(prefs.customSoundFile)
        reloadCustomBitmaps()
        setContentView(buildUi())
    }

    override fun onResume() {
        super.onResume()
        refreshScore()
    }

    override fun onDestroy() {
        super.onDestroy()
        sound.release()
    }

    // ── 界面搭建 ──────────────────────────────────────────
    private fun buildUi(): View {
        val scroll = ScrollView(this).apply { setBackgroundColor(cBg); isFillViewport = true }
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad(16), pad(20), pad(16), pad(28))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        scroll.addView(col)

        col.addView(TextView(this).apply {
            text = getString(R.string.main_title)
            setTextColor(cText)
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        })
        col.addView(TextView(this).apply {
            text = getString(R.string.main_intro)
            setTextColor(cDim)
            textSize = 12f
            setPadding(0, pad(6), 0, pad(14))
        })

        col.addView(sectionTitle(getString(R.string.sec_lang)))
        col.addView(langCard())

        col.addView(sectionTitle(getString(R.string.sec_prey)))
        col.addView(preySection())

        col.addView(sectionTitle(getString(R.string.sec_bg)))
        col.addView(bgSection())

        col.addView(sectionTitle(getString(R.string.sec_speed)))
        col.addView(speedCountCard())

        col.addView(sectionTitle(getString(R.string.sec_sound)))
        col.addView(soundCard())

        col.addView(sectionTitle(getString(R.string.sec_haptic)))
        col.addView(hapticCard())

        col.addView(sectionTitle(getString(R.string.sec_custom)))
        col.addView(customCard())

        col.addView(sectionTitle(getString(R.string.sec_exit)))
        col.addView(exitCard())

        col.addView(startButton())
        col.addView(scoreCard())

        // 分发 APK 时要让使用者知道许可证和源码在哪；LICENSE 全文也打进了 assets
        col.addView(TextView(this).apply {
            text = getString(R.string.footer_license)
            setTextColor(cDim)
            textSize = 10f
            gravity = Gravity.CENTER
            setPadding(0, pad(22), 0, 0)
        })

        return scroll
    }

    /** 应用内语言切换：不用改系统设置也能切成英文 */
    private fun langCard(): View {
        val wrap = card()
        val tags = listOf("", "en", "zh")
        val current = AppCompatDelegate.getApplicationLocales()
            .toLanguageTags().substringBefore('-').lowercase()
        val selected = tags.indexOfFirst { it.isNotEmpty() && it == current }.let { if (it < 0) 0 else it }
        wrap.addView(segmented(
            listOf(getString(R.string.lang_system), getString(R.string.lang_en), getString(R.string.lang_zh)),
            selected
        ) { i ->
            AppCompatDelegate.setApplicationLocales(
                if (tags[i].isEmpty()) LocaleListCompat.getEmptyLocaleList()
                else LocaleListCompat.forLanguageTags(tags[i])
            )
            // 切换后系统会重建 Activity，界面立刻变成新语言
        })
        return wrap
    }

    private fun preySection(): View {
        val wrap = card()
        for (g in Group.entries) {
            val list = PreyType.entries.filter { it.group == g }
            if (list.isEmpty()) continue
            wrap.addView(TextView(this).apply {
                text = getString(g.labelRes)
                setTextColor(cDim)
                textSize = 12f
                setPadding(pad(2), pad(8), 0, pad(6))
            })
            val cols = 4
            val tileW = (resources.displayMetrics.widthPixels - pad(32) - pad(24)) / cols
            val grid = GridLayout(this).apply { columnCount = cols }
            for (t in list) {
                val tile = PreyTile(this, t, { customPreyBmp }, { prefs.sizeRelative })
                tile.picked = prefs.preyKey == t.key
                tile.layoutParams = GridLayout.LayoutParams().apply {
                    width = tileW
                    height = pad(86)
                }
                tile.setOnClickListener {
                    if (t == PreyType.CUSTOM && !prefs.hasCustomPrey()) {
                        Toast.makeText(this, R.string.toast_pick_prey_first, Toast.LENGTH_SHORT).show()
                    }
                    prefs.preyKey = t.key
                    refreshPreySelection()
                    if (t != PreyType.MIXED && t != PreyType.CUSTOM) sound.play(t.sound)
                    haptics.tick()
                }
                preyTiles.add(tile)
                grid.addView(tile)
            }
            wrap.addView(grid)
        }
        wrap.addView(TextView(this).apply {
            text = getString(R.string.prey_hint)
            setTextColor(cDim)
            textSize = 11f
            setPadding(pad(2), pad(8), 0, 0)
        })
        return wrap
    }

    private fun bgSection(): View {
        val wrap = card()
        val cols = 3
        val tileW = (resources.displayMetrics.widthPixels - pad(32) - pad(24)) / cols
        val grid = GridLayout(this).apply { columnCount = cols }
        for (b in BgType.entries) {
            val tile = BgTile(this, b) { customBgBmp }
            tile.picked = prefs.bgKey == b.key
            tile.layoutParams = GridLayout.LayoutParams().apply {
                width = tileW
                height = pad(84)
            }
            tile.setOnClickListener {
                if (b == BgType.CUSTOM && !prefs.hasCustomBg()) {
                    Toast.makeText(this, R.string.toast_pick_bg_first, Toast.LENGTH_SHORT).show()
                }
                prefs.bgKey = b.key
                refreshBgSelection()
                haptics.tick()
            }
            bgTiles.add(tile)
            grid.addView(tile)
        }
        wrap.addView(grid)
        wrap.addView(TextView(this).apply {
            text = getString(R.string.bg_hint)
            setTextColor(cDim)
            textSize = 11f
            setPadding(pad(2), pad(8), 0, 0)
        })
        return wrap
    }

    private fun speedCountCard(): View {
        val wrap = card()
        speedLabel = TextView(this).apply {
            setTextColor(cText); textSize = 14f
            text = getString(R.string.speed_fmt, prefs.speed, prefs.speedLabel)
        }
        wrap.addView(speedLabel)
        wrap.addView(seek(0, 4, prefs.speed - 1) { v ->
            prefs.speed = v + 1
            speedLabel?.text = getString(R.string.speed_fmt, prefs.speed, prefs.speedLabel)
        })

        countLabel = TextView(this).apply {
            setTextColor(cText); textSize = 14f
            text = getString(R.string.count_fmt, prefs.count)
            setPadding(0, pad(10), 0, 0)
        }
        wrap.addView(countLabel)
        wrap.addView(seek(0, 9, prefs.count - 1) { v ->
            prefs.count = v + 1
            countLabel?.text = getString(R.string.count_fmt, prefs.count)
        })

        sizeLabel = TextView(this).apply {
            setTextColor(cText); textSize = 14f
            text = getString(R.string.size_fmt, prefs.size, prefs.sizePercent)
            setPadding(0, pad(10), 0, 0)
        }
        wrap.addView(sizeLabel)
        wrap.addView(seek(0, 9, prefs.size - 1) { v ->
            prefs.size = v + 1
            sizeLabel?.text = getString(R.string.size_fmt, prefs.size, prefs.sizePercent)
            preyTiles.forEach { it.invalidate() }
        })
        wrap.addView(TextView(this).apply {
            text = getString(R.string.size_cap_hint)
            setTextColor(cDim); textSize = 11f
            setPadding(pad(2), pad(6), 0, 0)
        })

        wrap.addView(TextView(this).apply {
            text = getString(R.string.speed_hint)
            setTextColor(cDim); textSize = 11f
            setPadding(pad(2), pad(8), 0, 0)
        })
        return wrap
    }

    private fun soundCard(): View {
        val wrap = card()
        wrap.addView(switchRow(getString(R.string.sound_enable), prefs.soundEnabled) { on ->
            prefs.soundEnabled = on
            sound.enabled = on
        })
        wrap.addView(switchRow(getString(R.string.sound_miss_enable), prefs.missSoundEnabled) { on ->
            prefs.missSoundEnabled = on
        })

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, pad(8), 0, 0)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        soundLabel = TextView(this).apply {
            setTextColor(cText); textSize = 14f
            text = prefs.soundChoiceLabel
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        row.addView(TextView(this).apply {
            text = getString(R.string.sound_catch_label)
            setTextColor(cDim); textSize = 13f
        })
        row.addView(soundLabel)
        row.addView(smallButton(getString(R.string.btn_change)) { showSoundPicker() })
        wrap.addView(row)

        val volLabel = TextView(this).apply {
            setTextColor(cText); textSize = 14f
            text = getString(R.string.volume_fmt, prefs.volume)
            setPadding(0, pad(10), 0, 0)
        }
        wrap.addView(volLabel)
        wrap.addView(seek(0, 100, prefs.volume) { v ->
            prefs.volume = v
            sound.volume = v / 100f
            volLabel.text = getString(R.string.volume_fmt, v)
        })
        wrap.addView(smallButton(getString(R.string.btn_preview)) {
            sound.enabled = true
            when (val c = prefs.soundChoice) {
                "custom" -> if (!sound.playCustom()) Toast.makeText(this, R.string.sound_custom_not_ready, Toast.LENGTH_SHORT).show()
                "auto" -> sound.play(if (prefs.prey == PreyType.MIXED) SoundId.SQUEAK else prefs.prey.sound)
                else -> sound.play(SoundId.entries.firstOrNull { it.name == c } ?: SoundId.SQUEAK)
            }
            sound.enabled = prefs.soundEnabled
        })
        return wrap
    }

    private fun showSoundPicker() {
        val labels = ArrayList<String>()
        val keys = ArrayList<String>()
        labels.add(getString(R.string.sound_auto_recommended)); keys.add("auto")
        for (s in SoundId.selectable) { labels.add(getString(s.labelRes)); keys.add(s.name) }
        labels.add(getString(
            if (prefs.hasCustomSound()) R.string.sound_custom_set else R.string.sound_custom_pick
        )); keys.add("custom")

        AlertDialog.Builder(this)
            .setTitle(R.string.dlg_sound_title)
            .setItems(labels.toTypedArray()) { _, i ->
                val k = keys[i]
                if (k == "custom" && !prefs.hasCustomSound()) {
                    pickSound.launch("audio/*")
                    return@setItems
                }
                prefs.soundChoice = k
                soundLabel?.text = prefs.soundChoiceLabel
                sound.enabled = true
                if (k == "custom") sound.playCustom()
                else if (k != "auto") sound.play(SoundId.entries.first { it.name == k })
                sound.enabled = prefs.soundEnabled
            }
            .show()
    }

    private fun hapticCard(): View {
        val wrap = card()
        wrap.addView(switchRow(getString(R.string.haptic_enable), prefs.hapticEnabled) { on ->
            prefs.hapticEnabled = on
            haptics.enabled = on
            if (on) haptics.tick()
        })
        wrap.addView(TextView(this).apply {
            text = getString(R.string.haptic_strength)
            setTextColor(cText); textSize = 14f
            setPadding(0, pad(8), 0, pad(6))
        })
        wrap.addView(segmented(listOf(getString(R.string.strength_light), getString(R.string.strength_medium),
            getString(R.string.strength_strong)), prefs.hapticStrength - 1) { i ->
            prefs.hapticStrength = i + 1
            haptics.strength = prefs.hapticStrength
            haptics.tick()
        })
        wrap.addView(TextView(this).apply {
            text = getString(if (haptics.available) R.string.haptic_hint else R.string.haptic_none)
            setTextColor(cDim); textSize = 11f
            setPadding(pad(2), pad(8), 0, 0)
        })
        return wrap
    }

    private fun customCard(): View {
        val wrap = card()
        preyStatus = TextView(this).apply { setTextColor(cDim); textSize = 11f }
        bgStatus = TextView(this).apply { setTextColor(cDim); textSize = 11f }
        soundStatus = TextView(this).apply { setTextColor(cDim); textSize = 11f }

        wrap.addView(assetRow(getString(R.string.custom_prey_title), preyStatus!!,
            pick = { pickPreyImg.launch("image/*") },
            clear = {
                prefs.clearFile(prefs.customPreyFile)
                if (prefs.preyKey == PreyType.CUSTOM.key) prefs.preyKey = PreyType.MOUSE.key
                reloadCustomBitmaps(); refreshPreySelection(); refreshStatus()
            }))
        wrap.addView(assetRow(getString(R.string.custom_bg_title), bgStatus!!,
            pick = { pickBgImg.launch("image/*") },
            clear = {
                prefs.clearFile(prefs.customBgFile)
                if (prefs.bgKey == BgType.CUSTOM.key) prefs.bgKey = BgType.GRASS.key
                reloadCustomBitmaps(); bgTiles.forEach { it.invalidateThumb() }
                refreshBgSelection(); refreshStatus()
            }))
        wrap.addView(assetRow(getString(R.string.custom_sound_title), soundStatus!!,
            pick = { pickSound.launch("audio/*") },
            clear = {
                prefs.clearFile(prefs.customSoundFile)
                if (prefs.soundChoice == "custom") prefs.soundChoice = "auto"
                sound.loadCustom(null)
                soundLabel?.text = prefs.soundChoiceLabel
                refreshStatus()
            }))

        wrap.addView(TextView(this).apply {
            text = getString(R.string.custom_hint)
            setTextColor(cDim); textSize = 11f
            setPadding(pad(2), pad(8), 0, 0)
        })
        refreshStatus()
        return wrap
    }

    private fun assetRow(title: String, status: TextView, pick: () -> Unit, clear: () -> Unit): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, pad(6), 0, pad(6))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        val left = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        left.addView(TextView(this).apply {
            text = title; setTextColor(cText); textSize = 14f
        })
        left.addView(status)
        row.addView(left)
        row.addView(smallButton(getString(R.string.btn_pick)) { pick() })
        row.addView(smallButton(getString(R.string.btn_clear)) { clear() })
        return row
    }

    private fun exitCard(): View {
        val wrap = card()
        wrap.addView(TextView(this).apply {
            text = getString(R.string.exit_desc)
            setTextColor(cText); textSize = 13f
        })
        wrap.addView(TextView(this).apply {
            text = getString(R.string.exit_hold_q)
            setTextColor(cText); textSize = 14f
            setPadding(0, pad(10), 0, pad(6))
        })
        val opts = listOf(2f, 3f, 5f)
        wrap.addView(segmented(listOf(getString(R.string.sec_2), getString(R.string.sec_3), getString(R.string.sec_5)), opts.indexOfFirst { it == prefs.exitHoldSec }.coerceAtLeast(1)) { i ->
            prefs.exitHoldSec = opts[i]
            haptics.tick()
        })
        wrap.addView(switchRow(getString(R.string.show_hud), prefs.showHud) { on -> prefs.showHud = on })
        wrap.addView(TextView(this).apply {
            text = getString(R.string.exit_pin_hint)
            setTextColor(cDim); textSize = 11f
            setPadding(pad(2), pad(8), 0, 0)
        })
        return wrap
    }

    private fun startButton(): View {
        val b = TextView(this).apply {
            text = getString(R.string.btn_start)
            setTextColor(0xFF1B2233.toInt())
            textSize = 19f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                setColor(cAccent)
                cornerRadius = 18f * dp
            }
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, pad(58)
            ).apply { topMargin = pad(20) }
            setOnClickListener {
                haptics.tick()
                playGame.launch(Intent(this@MainActivity, GameActivity::class.java))
            }
        }
        return b
    }

    private fun scoreCard(): View {
        val wrap = card()
        scoreLabel = TextView(this).apply { setTextColor(cText); textSize = 13f }
        wrap.addView(scoreLabel)
        refreshScore()
        return wrap
    }

    // ── 通用控件 ──────────────────────────────────────────
    private fun pad(v: Int) = (v * dp).toInt()

    private fun sectionTitle(s: String) = TextView(this).apply {
        text = s
        setTextColor(cAccent)
        textSize = 14f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        setPadding(pad(4), pad(18), 0, pad(8))
    }

    private fun card(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(pad(12), pad(12), pad(12), pad(12))
        background = GradientDrawable().apply {
            setColor(cCard)
            cornerRadius = 16f * dp
        }
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun seek(min: Int, max: Int, value: Int, onChange: (Int) -> Unit): SeekBar =
        SeekBar(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            this.max = max - min
            progress = (value - min).coerceIn(0, max - min)
            progressDrawable?.setTint(cAccent)
            thumb?.setTint(cAccent)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) = onChange(p + min)
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) { haptics.tick() }
            })
        }

    private fun switchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, pad(6), 0, pad(6))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        row.addView(TextView(this).apply {
            text = label
            setTextColor(cText); textSize = 14f
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        row.addView(SwitchCompat(this).apply {
            isChecked = checked
            setOnCheckedChangeListener { _, v -> onChange(v) }
        })
        return row
    }

    private fun smallButton(label: String, onClick: () -> Unit): TextView = TextView(this).apply {
        text = label
        setTextColor(cAccent)
        textSize = 13f
        gravity = Gravity.CENTER
        setPadding(pad(14), pad(8), pad(14), pad(8))
        background = GradientDrawable().apply {
            setColor(0x22FFE066)
            cornerRadius = 10f * dp
        }
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { leftMargin = pad(8) }
        setOnClickListener { onClick() }
    }

    private fun segmented(labels: List<String>, selected: Int, onSelect: (Int) -> Unit): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        val views = ArrayList<TextView>()
        labels.forEachIndexed { i, s ->
            val tv = TextView(this).apply {
                text = s
                textSize = 13f
                gravity = Gravity.CENTER
                setPadding(pad(10), pad(10), pad(10), pad(10))
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    .apply { if (i > 0) leftMargin = pad(8) }
            }
            tv.setOnClickListener {
                views.forEachIndexed { k, v -> styleSeg(v, k == i) }
                onSelect(i)
            }
            views.add(tv)
            row.addView(tv)
        }
        views.forEachIndexed { k, v -> styleSeg(v, k == selected) }
        return row
    }

    private fun styleSeg(tv: TextView, on: Boolean) {
        tv.setTextColor(if (on) 0xFF1B2233.toInt() else cText)
        tv.background = GradientDrawable().apply {
            setColor(if (on) cAccent else 0x1AFFFFFF)
            cornerRadius = 12f * tv.resources.displayMetrics.density
        }
    }

    // ── 状态刷新 ──────────────────────────────────────────
    private fun refreshPreySelection() {
        preyTiles.forEach { it.picked = it.type.key == prefs.preyKey }
    }

    private fun refreshBgSelection() {
        bgTiles.forEach { it.picked = it.type.key == prefs.bgKey }
    }

    private fun refreshStatus() {
        preyStatus?.text = getString(
            if (prefs.hasCustomPrey()) R.string.status_prey_set else R.string.status_unset)
        bgStatus?.text = getString(
            if (prefs.hasCustomBg()) R.string.status_bg_set else R.string.status_unset)
        soundStatus?.text = getString(
            if (prefs.hasCustomSound()) R.string.status_sound_set else R.string.status_unset)
    }

    private fun refreshScore() {
        scoreLabel?.text = getString(R.string.score_fmt, prefs.lastScore,
            prefs.lastSeconds / 60, prefs.lastSeconds % 60, prefs.bestScore, prefs.totalCatches)
    }

    private fun reloadCustomBitmaps() {
        customPreyBmp?.recycle(); customPreyBmp = null
        customBgBmp?.recycle(); customBgBmp = null
        if (prefs.hasCustomPrey()) customPreyBmp = decode(prefs.customPreyFile, 512)
        if (prefs.hasCustomBg()) customBgBmp = decode(prefs.customBgFile, 1024)
        preyTiles.forEach { it.invalidate() }
    }

    private fun decode(f: File, maxPx: Int): Bitmap? = try {
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(f.absolutePath, o)
        var s = 1
        while (o.outWidth / s > maxPx || o.outHeight / s > maxPx) s *= 2
        BitmapFactory.decodeFile(f.absolutePath, BitmapFactory.Options().apply { inSampleSize = s })
    } catch (e: Throwable) { null }

    private fun importTo(uri: Uri, target: File, what: String, onOk: () -> Unit) {
        if (prefs.importFile(uri, target)) {
            onOk()
            Toast.makeText(this, getString(R.string.toast_asset_set, what), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, getString(R.string.toast_asset_fail, what), Toast.LENGTH_SHORT).show()
        }
    }
}
