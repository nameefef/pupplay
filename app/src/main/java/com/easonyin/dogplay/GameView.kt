package com.easonyin.dogplay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.view.MotionEvent
import android.view.View
import java.io.File
import kotlin.math.hypot
import kotlin.random.Random

class GameView(ctx: Context, private val prefs: Prefs) : View(ctx) {

    /** 长按角落满时间后回调（分数，秒数） */
    var onExit: ((Int, Int) -> Unit)? = null

    private val dp = resources.displayMetrics.density
    private val rnd = Random(System.nanoTime())
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val arcRect = RectF()

    private val sound = SoundEngine()
    private val haptics = Haptics(ctx)

    private val preyList = ArrayList<Prey>()
    private var bgBitmap: Bitmap? = null
    private var customPrey: Bitmap? = null
    private var customBgSrc: Bitmap? = null

    private var lastNanos = 0L
    private var running = false
    private var elapsed = 0f

    var score = 0
        private set
    private var combo = 0

    // 长按退出
    private var exitPointer = -1
    private var exitHeld = 0f
    private var exitDownX = 0f
    private var exitDownY = 0f
    private var exited = false
    private val cornerR get() = 52f * dp

    private class Particle(var x: Float, var y: Float, var vx: Float, var vy: Float,
                           var life: Float, val max: Float, val size: Float, val color: Int)
    private val particles = ArrayList<Particle>()

    private class Popup(var x: Float, var y: Float, var life: Float, val label: String)
    private val popups = ArrayList<Popup>()

    private class Ring(var x: Float, var y: Float, var life: Float, val color: Int)
    private val rings = ArrayList<Ring>()

    init {
        isFocusable = true
        keepScreenOn = true
        sound.enabled = prefs.soundEnabled
        sound.volume = prefs.volume / 100f
        haptics.enabled = prefs.hapticEnabled
        haptics.strength = prefs.hapticStrength
        loadAssets()
        Thread { prepareSounds() }.start()
    }

    private fun loadAssets() {
        if (prefs.prey == PreyType.CUSTOM && prefs.hasCustomPrey()) {
            customPrey = decodeScaled(prefs.customPreyFile, 512)
        }
        if (prefs.bg == BgType.CUSTOM && prefs.hasCustomBg()) {
            customBgSrc = decodeScaled(prefs.customBgFile, 2048)
        }
        if (prefs.soundChoice == "custom" && prefs.hasCustomSound()) {
            sound.loadCustom(prefs.customSoundFile)
        }
    }

    private fun decodeScaled(f: File, maxPx: Int): Bitmap? = try {
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(f.absolutePath, o)
        var s = 1
        while (o.outWidth / s > maxPx || o.outHeight / s > maxPx) s *= 2
        BitmapFactory.decodeFile(f.absolutePath, BitmapFactory.Options().apply { inSampleSize = s })
    } catch (e: Throwable) { null }

    private fun prepareSounds() {
        val ids = HashSet<SoundId>()
        ids.add(SoundId.MISS)
        ids.add(SoundId.REWARD)
        when (val c = prefs.soundChoice) {
            "custom" -> {}
            "auto" -> {
                if (prefs.prey == PreyType.MIXED) PreyType.playable.forEach { ids.add(it.sound) }
                else ids.add(prefs.prey.sound)
            }
            else -> SoundId.entries.firstOrNull { it.name == c }?.let { ids.add(it) }
        }
        sound.prepare(ids)
    }

    private fun playCatchSound(t: PreyType) {
        when (val c = prefs.soundChoice) {
            "custom" -> if (!sound.playCustom()) sound.play(t.sound)
            "auto" -> sound.play(t.sound)
            else -> sound.play(SoundId.entries.firstOrNull { it.name == c } ?: t.sound)
        }
    }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        super.onSizeChanged(w, h, ow, oh)
        if (w <= 0 || h <= 0) return
        bgBitmap?.recycle()
        bgBitmap = BackgroundRenderer.render(prefs.bg, w, h, customBgSrc)
        rebuildPrey(w.toFloat(), h.toFloat())
    }

    private fun rebuildPrey(w: Float, h: Float) {
        preyList.clear()
        val mixed = prefs.prey == PreyType.MIXED
        repeat(prefs.count) {
            val t = if (mixed) PreyType.playable.random(rnd) else prefs.prey
            val p = Prey(t, dp, rnd)
            if (mixed) p.respawnType = { PreyType.playable.random(rnd) }
            p.spawn(w, h)
            preyList.add(p)
        }
    }

    fun resume() {
        running = true
        lastNanos = 0L
        postInvalidateOnAnimation()
    }

    fun pause() { running = false }

    fun release() { sound.release(); bgBitmap?.recycle(); bgBitmap = null }

    val seconds: Int get() = elapsed.toInt()

    // ── 输入 ────────────────────────────────────────────────
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (exited) return true
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val i = e.actionIndex
                val x = e.getX(i); val y = e.getY(i)
                if (exitPointer == -1 && inCorner(x, y)) {
                    exitPointer = e.getPointerId(i)
                    exitHeld = 0f
                    exitDownX = x; exitDownY = y
                } else {
                    tap(x, y)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (exitPointer != -1) {
                    val i = e.findPointerIndex(exitPointer)
                    // 手指挪开超过 40dp 就算取消，避免狗爪蹭过去正好压住
                    if (i < 0 || hypot(e.getX(i) - exitDownX, e.getY(i) - exitDownY) > 40f * dp) {
                        cancelExitHold()
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                if (e.getPointerId(e.actionIndex) == exitPointer) cancelExitHold()
            }
            MotionEvent.ACTION_CANCEL -> cancelExitHold()
        }
        return true
    }

    private fun inCorner(x: Float, y: Float) = x < cornerR * 1.6f && y < cornerR * 1.6f

    private fun cancelExitHold() { exitPointer = -1; exitHeld = 0f }

    private fun tap(x: Float, y: Float) {
        // 从最后画的往前找，视觉上「最上面那只」优先被抓到
        for (i in preyList.indices.reversed()) {
            val p = preyList[i]
            if (p.hit(x, y)) {
                p.caught()
                score++
                combo++
                prefs.totalCatches = prefs.totalCatches + 1
                playCatchSound(p.type)
                haptics.catchHit()
                burst(p.x, p.y, p.type.c1, 16)
                rings.add(Ring(p.x, p.y, 0f, p.type.c1))
                popups.add(Popup(p.x, p.y - p.size * 0.6f, 0f, context.getString(R.string.popup_plus_one)))
                if (combo % 10 == 0) {
                    sound.play(SoundId.REWARD)
                    haptics.reward()
                    burst(p.x, p.y, 0xFFFFE066.toInt(), 30)
                    popups.add(Popup(p.x, p.y - p.size * 1.2f, 0f,
                        context.getString(R.string.combo_fmt, combo)))
                }
                return
            }
        }
        // 没抓到：也要有反馈，并且惊动附近的猎物（这一下最能激起继续追）
        if (prefs.missSoundEnabled) sound.play(SoundId.MISS, 0.8f)
        haptics.miss()
        rings.add(Ring(x, y, 0f, 0x88FFFFFF.toInt()))
        val w = width.toFloat(); val h = height.toFloat()
        for (p in preyList) p.startle(x, y, w, h)
    }

    private fun burst(x: Float, y: Float, color: Int, n: Int) {
        repeat(n) {
            val a = rnd.nextFloat() * 6.2832f
            val sp = (60f + rnd.nextFloat() * 300f) * dp
            val life = 0.35f + rnd.nextFloat() * 0.45f
            particles.add(
                Particle(
                    x, y,
                    kotlin.math.cos(a.toDouble()).toFloat() * sp,
                    kotlin.math.sin(a.toDouble()).toFloat() * sp,
                    life, life, (2f + rnd.nextFloat() * 4f) * dp, color
                )
            )
        }
    }

    // ── 循环 ────────────────────────────────────────────────
    override fun onDraw(c: Canvas) {
        val now = System.nanoTime()
        var dt = if (lastNanos == 0L) 0f else (now - lastNanos) / 1_000_000_000f
        lastNanos = now
        if (dt > 0.05f) dt = 0.05f    // 卡顿时不要让猎物瞬移

        val w = width.toFloat(); val h = height.toFloat()
        if (running && !exited) {
            elapsed += dt
            val mul = prefs.speedMul
            for (p in preyList) p.update(dt, w, h, mul)
            stepEffects(dt)
            stepExitHold(dt)
        }

        // 背景
        val bg = bgBitmap
        if (bg != null && !bg.isRecycled) c.drawBitmap(bg, 0f, 0f, null)
        else c.drawColor(0xFF0B1220.toInt())

        // 猎物：按 y 排序，靠下的画在上面，有点前后层次
        preyList.sortBy { it.y }
        for (p in preyList) p.draw(c, paint, customPrey, prefs.bg != BgType.CUSTOM)

        drawEffects(c)
        if (prefs.showHud) drawHud(c, w)
        drawExitCorner(c)

        if (running) postInvalidateOnAnimation()
    }

    private fun stepEffects(dt: Float) {
        var i = particles.size - 1
        while (i >= 0) {
            val p = particles[i]
            p.life -= dt
            if (p.life <= 0f) particles.removeAt(i)
            else {
                p.x += p.vx * dt; p.y += p.vy * dt
                p.vy += 900f * dp * dt          // 一点重力，碎屑会落下来
                p.vx *= 0.96f
            }
            i--
        }
        i = popups.size - 1
        while (i >= 0) {
            val p = popups[i]
            p.life += dt
            p.y -= 60f * dp * dt
            if (p.life > 0.9f) popups.removeAt(i)
            i--
        }
        i = rings.size - 1
        while (i >= 0) {
            val r = rings[i]
            r.life += dt
            if (r.life > 0.45f) rings.removeAt(i)
            i--
        }
    }

    private fun stepExitHold(dt: Float) {
        if (exitPointer == -1) return
        exitHeld += dt
        if (exitHeld >= prefs.exitHoldSec) {
            exited = true
            running = false
            sound.play(SoundId.SPARKLE)
            haptics.reward()
            prefs.lastScore = score
            prefs.lastSeconds = seconds
            if (score > prefs.bestScore) prefs.bestScore = score
            onExit?.invoke(score, seconds)
        }
    }

    private fun drawEffects(c: Canvas) {
        paint.shader = null
        paint.style = Paint.Style.FILL
        for (p in particles) {
            val a = (p.life / p.max).coerceIn(0f, 1f)
            paint.color = Color.argb((255 * a).toInt().coerceIn(0, 255), Color.red(p.color), Color.green(p.color), Color.blue(p.color))
            c.drawCircle(p.x, p.y, p.size * a, paint)
        }
        paint.style = Paint.Style.STROKE
        for (r in rings) {
            val t = r.life / 0.45f
            paint.strokeWidth = (5f - 3.5f * t) * dp
            paint.color = Color.argb(((1f - t) * 200).toInt().coerceIn(0, 255), Color.red(r.color), Color.green(r.color), Color.blue(r.color))
            c.drawCircle(r.x, r.y, (14f + 60f * t) * dp, paint)
        }
        paint.style = Paint.Style.FILL
        text.textAlign = Paint.Align.CENTER
        for (p in popups) {
            val a = (1f - (p.life / 0.9f)).coerceIn(0f, 1f)
            text.textSize = 22f * dp
            text.color = Color.argb((255 * a).toInt(), 255, 235, 130)
            c.drawText(p.label, p.x, p.y, text)
        }
    }

    private fun drawHud(c: Canvas, w: Float) {
        text.textAlign = Paint.Align.RIGHT
        text.textSize = 15f * dp
        text.color = Color.argb(150, 255, 255, 255)
        val m = (elapsed / 60).toInt()
        val s = (elapsed % 60).toInt()
        c.drawText(context.getString(R.string.hud_fmt, score, m, s), w - 16f * dp, 26f * dp, text)
    }

    /** 左上角的退出圈：平时很淡，按住时进度环走满才退出 */
    private fun drawExitCorner(c: Canvas) {
        val cx = cornerR * 0.82f
        val cy = cornerR * 0.82f
        val r = cornerR * 0.5f
        paint.shader = null
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f * dp
        paint.color = if (exitPointer >= 0) Color.argb(120, 255, 255, 255) else Color.argb(45, 255, 255, 255)
        c.drawCircle(cx, cy, r, paint)

        text.textAlign = Paint.Align.CENTER
        if (exitPointer >= 0) {
            val prog = (exitHeld / prefs.exitHoldSec).coerceIn(0f, 1f)
            paint.strokeWidth = 5f * dp
            paint.color = 0xFFFFE066.toInt()
            arcRect.set(cx - r, cy - r, cx + r, cy + r)
            c.drawArc(arcRect, -90f, 360f * prog, false, paint)
            text.textSize = 13f * dp
            text.color = Color.argb(230, 255, 235, 130)
            c.drawText(context.getString(R.string.exit_hold_fmt, prefs.exitHoldSec - exitHeld),
                cx + r * 1.9f, cy + 5f * dp, text)
        } else {
            paint.style = Paint.Style.FILL
            paint.color = Color.argb(60, 255, 255, 255)
            c.drawCircle(cx, cy, r * 0.22f, paint)
        }
        paint.style = Paint.Style.FILL
    }
}
