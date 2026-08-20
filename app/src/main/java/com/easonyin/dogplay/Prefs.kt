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

import android.content.Context
import android.net.Uri
import java.io.File

/** 全部设置项 + 自定义素材文件管理 */
class Prefs(private val ctx: Context) {

    private val sp = ctx.getSharedPreferences("dogplay", Context.MODE_PRIVATE)

    // ── 玩法 ──────────────────────────────────────────────
    var preyKey: String
        get() = sp.getString("prey", PreyType.MOUSE.key) ?: PreyType.MOUSE.key
        set(v) = sp.edit().putString("prey", v).apply()

    val prey: PreyType get() = PreyType.fromKey(preyKey)

    /** 1..5 */
    var speed: Int
        get() = sp.getInt("speed", 3).coerceIn(1, 5)
        set(v) = sp.edit().putInt("speed", v.coerceIn(1, 5)).apply()

    /** 1..10 */
    var count: Int
        get() = sp.getInt("count", 3).coerceIn(1, 10)
        set(v) = sp.edit().putInt("count", v.coerceIn(1, 10)).apply()

    /**
     * 猎物大小 1..10。第 5 档是按屏幕自动算出来的「最合适」大小，也是默认值。
     * 又换了一次 key：档位含义从「倍率」变成了「占屏幕短边的比例」，老值不能沿用。
     */
    var size: Int
        get() = sp.getInt("size3", 5).coerceIn(1, 10)
        set(v) = sp.edit().putInt("size3", v.coerceIn(1, 10)).apply()

    /**
     * 档位 -> 猎物占屏幕短边（横屏时就是屏幕高度）的比例。
     * 不再用「角色基准 × 倍率」，因为那个数字跟屏幕无关，
     * 同一个倍率在小屏上占半个屏幕、在平板上小得看不见。
     */
    val sizeFraction: Float get() = SIZE_FRACTIONS[size - 1]

    /** 给界面显示用的百分比 */
    val sizePercent: Int get() = Math.round(sizeFraction * 100f)

    /** 相对中间档的缩放，菜单预览格子用 */
    val sizeRelative: Float get() = sizeFraction / SIZE_FRACTIONS[4]

    /** 速度档位 -> 实际倍率 */
    val speedMul: Float get() = when (speed) {
        1 -> 0.45f; 2 -> 0.7f; 3 -> 1.0f; 4 -> 1.45f; else -> 2.0f
    }

    val speedLabel: String get() = ctx.getString(when (speed) {
        1 -> R.string.speed_1; 2 -> R.string.speed_2; 3 -> R.string.speed_3
        4 -> R.string.speed_4; else -> R.string.speed_5
    })

    // ── 背景 ──────────────────────────────────────────────
    var bgKey: String
        get() = sp.getString("bg", BgType.GRASS.key) ?: BgType.GRASS.key
        set(v) = sp.edit().putString("bg", v).apply()

    val bg: BgType get() = BgType.fromKey(bgKey)

    // ── 音效 ──────────────────────────────────────────────
    var soundEnabled: Boolean
        get() = sp.getBoolean("sound", true)
        set(v) = sp.edit().putBoolean("sound", v).apply()

    /** 0..100 */
    var volume: Int
        get() = sp.getInt("volume", 85).coerceIn(0, 100)
        set(v) = sp.edit().putInt("volume", v.coerceIn(0, 100)).apply()

    /** "auto" = 跟随角色；SoundId.name = 指定内置音效；"custom" = 用户音频文件 */
    var soundChoice: String
        get() = sp.getString("soundChoice", "auto") ?: "auto"
        set(v) = sp.edit().putString("soundChoice", v).apply()

    val soundChoiceLabel: String get() = when (val c = soundChoice) {
        "auto" -> ctx.getString(R.string.sound_auto)
        "custom" -> ctx.getString(
            if (customSoundFile.exists()) R.string.sound_custom else R.string.sound_custom_none)
        else -> SoundId.entries.firstOrNull { it.name == c }
            ?.let { ctx.getString(it.labelRes) } ?: ctx.getString(R.string.sound_auto)
    }

    /** 落空时的音效也要有（用户要求“点击都要有声效”） */
    var missSoundEnabled: Boolean
        get() = sp.getBoolean("missSound", true)
        set(v) = sp.edit().putBoolean("missSound", v).apply()

    // ── 震动 ──────────────────────────────────────────────
    var hapticEnabled: Boolean
        get() = sp.getBoolean("haptic", true)
        set(v) = sp.edit().putBoolean("haptic", v).apply()

    /** 1=轻 2=中 3=强 */
    var hapticStrength: Int
        get() = sp.getInt("hapticStrength", 2).coerceIn(1, 3)
        set(v) = sp.edit().putInt("hapticStrength", v.coerceIn(1, 3)).apply()

    // ── 防误退 ────────────────────────────────────────────
    /** 长按左上角多少秒才退出：2 / 3 / 5 */
    var exitHoldSec: Float
        get() = sp.getFloat("exitHold", 3f)
        set(v) = sp.edit().putFloat("exitHold", v).apply()

    var showHud: Boolean
        get() = sp.getBoolean("hud", true)
        set(v) = sp.edit().putBoolean("hud", v).apply()

    // ── 战绩 ──────────────────────────────────────────────
    var lastScore: Int
        get() = sp.getInt("lastScore", 0)
        set(v) = sp.edit().putInt("lastScore", v).apply()

    var lastSeconds: Int
        get() = sp.getInt("lastSeconds", 0)
        set(v) = sp.edit().putInt("lastSeconds", v).apply()

    var bestScore: Int
        get() = sp.getInt("bestScore", 0)
        set(v) = sp.edit().putInt("bestScore", v).apply()

    var totalCatches: Int
        get() = sp.getInt("totalCatches", 0)
        set(v) = sp.edit().putInt("totalCatches", v).apply()

    // ── 自定义素材（复制进应用私有目录，不依赖 URI 授权，重启也不会失效）──
    val customPreyFile: File get() = File(ctx.filesDir, "custom_prey.png")
    val customBgFile: File get() = File(ctx.filesDir, "custom_bg.png")
    val customSoundFile: File get() = File(ctx.filesDir, "custom_sound.bin")

    fun hasCustomPrey() = customPreyFile.exists()
    fun hasCustomBg() = customBgFile.exists()
    fun hasCustomSound() = customSoundFile.exists()

    /** 把用户挑的文件复制进来，返回是否成功 */
    fun importFile(uri: Uri, target: File): Boolean = try {
        ctx.contentResolver.openInputStream(uri)?.use { input ->
            target.outputStream().use { out -> input.copyTo(out) }
        }
        target.exists() && target.length() > 0
    } catch (e: Throwable) {
        target.delete()
        false
    }

    fun clearFile(f: File) { try { f.delete() } catch (e: Throwable) {} }

    companion object {
        /**
         * 每一档猎物占屏幕短边的比例。第 5 档 0.18 是中间档，
         * 对绝大多数手机来说都是「一眼看得见、爪子够得着、又不占满屏」的甜点。
         */
        private val SIZE_FRACTIONS = floatArrayOf(
            0.07f, 0.09f, 0.115f, 0.145f, 0.18f, 0.22f, 0.26f, 0.30f, 0.35f, 0.40f
        )
    }
}
