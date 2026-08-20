package com.easonyin.dogplay

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.SoundPool
import java.io.File
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin

/** 所有内置音效。全部由代码合成，不需要任何音频素材文件。 */
enum class SoundId(val labelRes: Int) {
    SQUEAK(R.string.snd_squeak), YIP(R.string.snd_yip), THUMP(R.string.snd_thump), CHITTER(R.string.snd_chitter), MEOW(R.string.snd_meow),
    SNIFF(R.string.snd_sniff), QUACK(R.string.snd_quack), CHEEP(R.string.snd_cheep), BAA(R.string.snd_baa), OINK(R.string.snd_oink),
    CROAK(R.string.snd_croak), HISS(R.string.snd_hiss), CLICK(R.string.snd_click), SPLASH(R.string.snd_splash), SKITTER(R.string.snd_skitter),
    FLUTTER(R.string.snd_flutter), BUZZ(R.string.snd_buzz), WHIRR(R.string.snd_whirr), TWEET(R.string.snd_tweet), SPARKLE(R.string.snd_sparkle),
    BLIP(R.string.snd_blip), BOING(R.string.snd_boing), WHOOSH(R.string.snd_whoosh), CRUNCH(R.string.snd_crunch), SQUEAKTOY(R.string.snd_squeaktoy),
    POP(R.string.snd_pop), MISS(R.string.snd_miss), REWARD(R.string.snd_reward);

    companion object {
        /** 可在设置里手动指定的音效（不含 MISS / REWARD 这两个系统音） */
        val selectable: List<SoundId> by lazy { entries.filter { it != MISS && it != REWARD } }
    }
}

/** 极简波形合成器：够用就好，重点是短促、高频、能勾起狗注意力。 */
private class Buf(val sr: Int, seconds: Float) {
    val d = FloatArray((sr * seconds).toInt().coerceAtLeast(1))

    /** 一段带扫频 / 颤音 / 谐波的音 */
    fun tone(
        startS: Float, durS: Float, f0: Float, f1: Float = f0, amp: Float = 0.7f,
        harm: Float = 0f, vibHz: Float = 0f, vibDepth: Float = 0f,
        attack: Float = 0.006f, curve: Float = 3f
    ) {
        val s = (startS * sr).toInt()
        val n = (durS * sr).toInt()
        if (n <= 0) return
        var phase = 0.0
        for (i in 0 until n) {
            val idx = s + i
            if (idx < 0 || idx >= d.size) continue
            val t = i.toFloat() / n
            // 指数插值扫频，听感更自然
            var f = f0 * (f1 / f0).toDouble().pow(t.toDouble()).toFloat()
            if (vibHz > 0f) f *= (1f + vibDepth * sin(2.0 * PI * vibHz * i / sr).toFloat())
            phase += 2.0 * PI * f / sr
            val base = sin(phase).toFloat()
            val v = if (harm > 0f) base + harm * sin(2 * phase).toFloat() + harm * 0.5f * sin(3 * phase).toFloat() else base
            // 包络：快速起音 + 指数衰减
            val aN = (attack * sr).coerceAtLeast(1f)
            val env = (if (i < aN) i / aN else 1f) * exp(-curve * t)
            d[idx] += v * env * amp
        }
    }

    /** 带一阶高低通整形的噪声段 */
    fun noise(
        startS: Float, durS: Float, amp: Float = 0.5f,
        lpHz: Float = 9000f, hpHz: Float = 0f, curve: Float = 4f, attack: Float = 0.003f
    ) {
        val s = (startS * sr).toInt()
        val n = (durS * sr).toInt()
        if (n <= 0) return
        val aLp = 1f - exp(-2f * PI.toFloat() * lpHz / sr)
        val aHp = if (hpHz > 0f) 1f - exp(-2f * PI.toFloat() * hpHz / sr) else 0f
        var lp = 0f
        var hp = 0f
        var seed = 22222L + s
        for (i in 0 until n) {
            val idx = s + i
            if (idx < 0 || idx >= d.size) continue
            seed = seed * 6364136223846793005L + 1442695040888963407L
            val white = ((seed ushr 40).toFloat() / 8388608f) - 1f
            lp += aLp * (white - lp)
            var v = lp
            if (aHp > 0f) { hp += aHp * (v - hp); v -= hp }
            val t = i.toFloat() / n
            val aN = (attack * sr).coerceAtLeast(1f)
            val env = (if (i < aN) i / aN else 1f) * exp(-curve * t)
            d[idx] += v * env * amp
        }
    }

    fun toShorts(): ShortArray {
        var peak = 0f
        for (v in d) { val a = abs(v); if (a > peak) peak = a }
        val g = if (peak > 0.001f) 0.85f / peak else 1f
        val out = ShortArray(d.size)
        // 首尾做几毫秒淡入淡出，避免爆音
        val fade = (0.004f * sr).toInt().coerceAtLeast(1)
        for (i in d.indices) {
            var v = d[i] * g
            if (i < fade) v *= i.toFloat() / fade
            val tail = d.size - 1 - i
            if (tail < fade) v *= tail.toFloat() / fade
            out[i] = (v * 32000f).toInt().coerceIn(-32767, 32767).toShort()
        }
        return out
    }
}

private object Synth {
    fun render(id: SoundId, sr: Int): ShortArray = when (id) {
        SoundId.SQUEAK -> Buf(sr, 0.20f).apply {
            tone(0f, .07f, 2800f, 1900f, .8f, harm = .3f)
            tone(.09f, .06f, 3100f, 2100f, .7f, harm = .3f)
        }.toShorts()

        SoundId.YIP -> Buf(sr, 0.22f).apply {
            tone(0f, .17f, 950f, 520f, .8f, harm = .35f, vibHz = 28f, vibDepth = .05f)
        }.toShorts()

        SoundId.THUMP -> Buf(sr, 0.18f).apply {
            tone(0f, .12f, 140f, 75f, .8f, harm = .2f, curve = 5f)
            noise(0f, .07f, .45f, lpHz = 500f, curve = 8f)
        }.toShorts()

        SoundId.CHITTER -> Buf(sr, 0.26f).apply {
            for (k in 0 until 6) tone(k * .038f, .022f, 3300f, 2700f, .55f, curve = 5f)
        }.toShorts()

        SoundId.MEOW -> Buf(sr, 0.42f).apply {
            tone(0f, .16f, 640f, 980f, .7f, harm = .4f, vibHz = 7f, vibDepth = .03f, curve = 0.6f)
            tone(.15f, .24f, 980f, 540f, .65f, harm = .4f, vibHz = 7f, vibDepth = .03f, curve = 2.2f)
        }.toShorts()

        SoundId.SNIFF -> Buf(sr, 0.22f).apply {
            noise(0f, .08f, .5f, hpHz = 2200f, curve = 6f)
            noise(.11f, .07f, .45f, hpHz = 2400f, curve = 6f)
        }.toShorts()

        SoundId.QUACK -> Buf(sr, 0.20f).apply {
            tone(0f, .14f, 500f, 360f, .8f, harm = .9f, vibHz = 45f, vibDepth = .13f, curve = 2f)
        }.toShorts()

        SoundId.CHEEP -> Buf(sr, 0.20f).apply {
            tone(0f, .07f, 3400f, 4300f, .7f)
            tone(.10f, .07f, 3800f, 3000f, .6f)
        }.toShorts()

        SoundId.BAA -> Buf(sr, 0.50f).apply {
            tone(0f, .45f, 430f, 370f, .7f, harm = .6f, vibHz = 15f, vibDepth = .10f, curve = 1.6f)
        }.toShorts()

        SoundId.OINK -> Buf(sr, 0.26f).apply {
            tone(0f, .20f, 310f, 175f, .8f, harm = .8f, vibHz = 30f, vibDepth = .16f, curve = 2.5f)
            noise(0f, .08f, .2f, hpHz = 1500f, curve = 6f)
        }.toShorts()

        SoundId.CROAK -> Buf(sr, 0.28f).apply {
            tone(0f, .24f, 195f, 150f, .8f, harm = 1f, vibHz = 55f, vibDepth = .30f, curve = 2f)
        }.toShorts()

        SoundId.HISS -> Buf(sr, 0.38f).apply {
            noise(0f, .36f, .5f, hpHz = 3800f, curve = 1.8f, attack = .03f)
        }.toShorts()

        SoundId.CLICK -> Buf(sr, 0.16f).apply {
            for (k in 0 until 3) noise(k * .05f, .018f, .7f, hpHz = 4200f, curve = 10f)
        }.toShorts()

        SoundId.SPLASH -> Buf(sr, 0.30f).apply {
            noise(0f, .26f, .6f, hpHz = 1200f, curve = 5f)
            tone(0f, .10f, 900f, 300f, .3f, curve = 6f)
        }.toShorts()

        SoundId.SKITTER -> Buf(sr, 0.22f).apply {
            for (k in 0 until 7) noise(k * .029f, .013f, .55f, hpHz = 5200f, curve = 12f)
        }.toShorts()

        SoundId.FLUTTER -> Buf(sr, 0.34f).apply {
            for (k in 0 until 5) noise(k * .065f, .045f, .35f, lpHz = 1400f, hpHz = 250f, curve = 5f, attack = .012f)
        }.toShorts()

        SoundId.BUZZ -> Buf(sr, 0.34f).apply {
            tone(0f, .32f, 230f, 205f, .5f, harm = 1f, vibHz = 12f, vibDepth = .06f, curve = 1.2f, attack = .02f)
        }.toShorts()

        SoundId.WHIRR -> Buf(sr, 0.30f).apply {
            tone(0f, .28f, 900f, 820f, .4f, harm = .5f, vibHz = 60f, vibDepth = .35f, curve = 1.5f, attack = .02f)
        }.toShorts()

        SoundId.TWEET -> Buf(sr, 0.24f).apply {
            tone(0f, .09f, 3000f, 4500f, .7f)
            tone(.11f, .09f, 4300f, 3200f, .6f)
        }.toShorts()

        SoundId.SPARKLE -> Buf(sr, 0.34f).apply {
            tone(0f, .14f, 1760f, 1760f, .5f, curve = 5f)
            tone(.06f, .14f, 2349f, 2349f, .5f, curve = 5f)
            tone(.12f, .18f, 3136f, 3136f, .5f, curve = 4f)
        }.toShorts()

        SoundId.BLIP -> Buf(sr, 0.10f).apply {
            tone(0f, .06f, 1200f, 2500f, .7f, curve = 4f)
        }.toShorts()

        SoundId.BOING -> Buf(sr, 0.36f).apply {
            tone(0f, .32f, 720f, 180f, .8f, harm = .45f, vibHz = 18f, vibDepth = .22f, curve = 2.2f)
        }.toShorts()

        SoundId.WHOOSH -> Buf(sr, 0.32f).apply {
            noise(0f, .30f, .55f, lpHz = 6000f, hpHz = 1200f, curve = 2.2f, attack = .05f)
        }.toShorts()

        SoundId.CRUNCH -> Buf(sr, 0.26f).apply {
            noise(0f, .12f, .7f, hpHz = 1600f, curve = 7f)
            noise(.13f, .09f, .5f, hpHz = 1800f, curve = 7f)
        }.toShorts()

        SoundId.SQUEAKTOY -> Buf(sr, 0.34f).apply {
            tone(0f, .18f, 1600f, 2700f, .75f, harm = .5f, curve = 1.2f)
            tone(.19f, .13f, 2500f, 1500f, .6f, harm = .5f, curve = 2f)
        }.toShorts()

        SoundId.POP -> Buf(sr, 0.10f).apply {
            tone(0f, .05f, 900f, 200f, .9f, harm = .4f, curve = 8f)
            noise(0f, .025f, .5f, hpHz = 2000f, curve = 12f)
        }.toShorts()

        SoundId.MISS -> Buf(sr, 0.14f).apply {
            noise(0f, .10f, .35f, lpHz = 2600f, hpHz = 400f, curve = 7f)
        }.toShorts()

        SoundId.REWARD -> Buf(sr, 0.50f).apply {
            tone(0f, .16f, 1046f, 1046f, .5f, curve = 4f)
            tone(.10f, .16f, 1318f, 1318f, .5f, curve = 4f)
            tone(.20f, .28f, 1568f, 1568f, .55f, curve = 3f)
        }.toShorts()
    }
}

/**
 * 播放层：每个音效预生成 PCM，装进 2 条 MODE_STATIC 的 AudioTrack 轮流播，
 * 延迟低、可叠音。自定义音效走 SoundPool 读本地文件。
 */
class SoundEngine {

    private val sr = 22050
    private val tracks = java.util.concurrent.ConcurrentHashMap<SoundId, Array<AudioTrack>>()
    private var soundPool: SoundPool? = null
    private var customId = 0
    private var customLoaded = false

    var enabled = true
    var volume = 1f

    private val attrs = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    /** 在后台线程提前生成需要的音效，避免第一次点击卡顿 */
    fun prepare(ids: Collection<SoundId>) {
        for (id in ids) {
            if (tracks.containsKey(id)) continue
            val pcm = try { Synth.render(id, sr) } catch (e: Throwable) { continue }
            val arr = ArrayList<AudioTrack>(2)
            repeat(2) {
                val t = buildTrack(pcm) ?: return@repeat
                arr.add(t)
            }
            if (arr.isNotEmpty()) tracks[id] = arr.toTypedArray()
        }
    }

    private fun buildTrack(pcm: ShortArray): AudioTrack? = try {
        val bytes = pcm.size * 2
        val t = AudioTrack.Builder()
            .setAudioAttributes(attrs)
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sr)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bytes)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        t.write(pcm, 0, pcm.size)
        t
    } catch (e: Throwable) { null }

    private val cursor = HashMap<SoundId, Int>()

    fun play(id: SoundId, vol: Float = 1f) {
        if (!enabled) return
        val arr = tracks[id] ?: run { prepare(listOf(id)); tracks[id] } ?: return
        val i = (cursor[id] ?: 0) % arr.size
        cursor[id] = i + 1
        val t = arr[i]
        try {
            if (t.playState != AudioTrack.PLAYSTATE_STOPPED) t.stop()
            t.reloadStaticData()
            val v = (volume * vol).coerceIn(0f, 1f)
            t.setVolume(v)
            t.play()
        } catch (e: Throwable) { /* 播放失败不影响游戏 */ }
    }

    /** 载入用户自选的音频文件 */
    fun loadCustom(file: File?) {
        releasePool()
        customLoaded = false
        if (file == null || !file.exists()) return
        val sp = SoundPool.Builder().setMaxStreams(4).setAudioAttributes(attrs).build()
        sp.setOnLoadCompleteListener { _, _, status -> customLoaded = status == 0 }
        customId = try { sp.load(file.absolutePath, 1) } catch (e: Throwable) { 0 }
        soundPool = sp
    }

    fun hasCustom(): Boolean = customLoaded

    fun playCustom(vol: Float = 1f): Boolean {
        if (!enabled || !customLoaded) return false
        val v = (volume * vol).coerceIn(0f, 1f)
        return try {
            soundPool?.play(customId, v, v, 1, 0, 1f) != null
        } catch (e: Throwable) { false }
    }

    private fun releasePool() {
        try { soundPool?.release() } catch (e: Throwable) {}
        soundPool = null
    }

    fun release() {
        for (arr in tracks.values) for (t in arr) {
            try { t.stop() } catch (e: Throwable) {}
            try { t.release() } catch (e: Throwable) {}
        }
        tracks.clear()
        releasePool()
    }
}
