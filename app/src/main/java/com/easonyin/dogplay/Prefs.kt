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
     * 猎物大小档位 1..10。具体多少毫米由屏幕算出来，见 [sizeMm]。
     * 默认第 8 档，偏大但仍留得下跑动空间。
     */
    var size: Int
        get() = sp.getInt("size5", 8).coerceIn(1, 10)
        set(v) = sp.edit().putInt("size5", v.coerceIn(1, 10)).apply()

    /**
     * 档位 -> 猎物画出来的高度（毫米），区间两端都是有依据的硬约束：
     *
     * **下限 18mm —— 狗要看得清。** 犬类视敏度约 20/75，最小可分辨视角 3.75 弧分。
     * 狗趴着看地上的手机视距 30~60cm（近于 33cm 根本对不上焦），
     * 要分辨出形状大约需要 30 个可分辨单元，60cm 处即 20mm、55cm 处即 18mm。
     * 再小狗就只看得见「有东西在动」，认不出是什么。
     *
     * **上限 屏幕短边的 45% —— 狗要玩得起来。** 猎物占满场地就没地方跑，
     * 而奔跑本身才是勾起捕猎欲的关键，不是大小。另外绝对不超过 70mm，
     * 免得在大平板上大到荒谬。
     *
     * 中间等比展开。所以同一档在不同设备上毫米数不同 —— 这是有意的：
     * 手机屏幕短边只有 65~71mm，物理上放不下「爪垫大小 + 还有地方跑」，
     * 必须让位给可玩性；平板放得下，区间就自动拉宽。
     */
    fun sizeMm(shortEdgeMm: Float): Float {
        val lo = MIN_VISIBLE_MM
        val hi = minOf(shortEdgeMm * PLAYABLE_FRACTION, MAX_MM).coerceAtLeast(lo * 1.15f)
        val t = (size - 1) / 9f
        return lo * Math.pow((hi / lo).toDouble(), t.toDouble()).toFloat()
    }

    fun sizeMmLabel(shortEdgeMm: Float): Int = Math.round(sizeMm(shortEdgeMm))

    /** 档位归一到 0~1，菜单预览格子用；改档位区间也不会失配 */
    val sizeNorm: Float get() = (size - 1) / 9f

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
        /** 狗能看清形状的下限（毫米），依据见 [sizeMm] */
        private const val MIN_VISIBLE_MM = 18f

        /** 猎物最多占屏幕短边的多少，超过就没地方跑了 */
        private const val PLAYABLE_FRACTION = 0.45f

        /** 绝对上限，防止大平板上大到荒谬 */
        private const val MAX_MM = 70f
    }
}
