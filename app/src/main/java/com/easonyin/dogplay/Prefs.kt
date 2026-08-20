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
}
