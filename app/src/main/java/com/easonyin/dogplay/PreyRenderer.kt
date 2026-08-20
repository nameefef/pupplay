package com.easonyin.dogplay

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * 所有猎物的矢量画法。
 * 约定：在“本地坐标系”里画，中心 (0,0)，朝向 +X，整体大致落在 [-0.6s, 0.6s] 之内。
 * 平移 / 旋转 / 左右翻转由调用方负责。
 */
object PreyRenderer {

    private val path = Path()
    private val path2 = Path()
    private val rect = RectF()

    fun draw(c: Canvas, p: Paint, t: PreyType, s: Float, phase: Float, custom: Bitmap?) {
        p.style = Paint.Style.FILL
        p.strokeCap = Paint.Cap.ROUND
        p.strokeJoin = Paint.Join.ROUND
        when (t) {
            PreyType.MOUSE -> mouse(c, p, s, phase, t)
            PreyType.FOX -> fox(c, p, s, phase, t)
            PreyType.RABBIT -> rabbit(c, p, s, phase, t)
            PreyType.SQUIRREL -> squirrel(c, p, s, phase, t)
            PreyType.CAT -> cat(c, p, s, phase, t)
            PreyType.HEDGEHOG -> hedgehog(c, p, s, phase, t)
            PreyType.RACCOON -> raccoon(c, p, s, phase, t)
            PreyType.DUCK -> duck(c, p, s, phase, t)
            PreyType.CHICK -> chick(c, p, s, phase, t)
            PreyType.SHEEP -> sheep(c, p, s, phase, t)
            PreyType.PIG -> pig(c, p, s, phase, t)
            PreyType.FROG -> frog(c, p, s, phase, t)
            PreyType.SNAKE -> snake(c, p, s, phase, t)
            PreyType.CRAB -> crab(c, p, s, phase, t)
            PreyType.FISH -> fish(c, p, s, phase, t)
            PreyType.BEETLE -> beetle(c, p, s, phase, t)
            PreyType.BUTTERFLY -> butterfly(c, p, s, phase, t)
            PreyType.BEE -> bee(c, p, s, phase, t)
            PreyType.SPIDER -> spider(c, p, s, phase, t)
            PreyType.DRAGONFLY -> dragonfly(c, p, s, phase, t)
            PreyType.BIRD -> bird(c, p, s, phase, t)
            PreyType.FIREFLY -> firefly(c, p, s, phase, t)
            PreyType.RED_DOT, PreyType.LASER, PreyType.BLUE_DOT -> dot(c, p, s, phase, t)
            PreyType.TENNIS -> tennis(c, p, s, phase, t)
            PreyType.FRISBEE -> frisbee(c, p, s, phase, t)
            PreyType.BONE -> bone(c, p, s, phase, t)
            PreyType.ROPE -> rope(c, p, s, phase, t)
            PreyType.BUBBLE -> bubble(c, p, s, phase, t)
            PreyType.STAR -> star(c, p, s, phase, t)
            PreyType.FEATHER -> feather(c, p, s, phase, t)
            PreyType.CUSTOM, PreyType.MIXED -> customBmp(c, p, s, custom)
        }
    }

    // ── 通用小工具 ──────────────────────────────────────────
    private fun oval(c: Canvas, p: Paint, cx: Float, cy: Float, rx: Float, ry: Float, col: Int) {
        p.color = col
        p.style = Paint.Style.FILL
        c.drawOval(cx - rx, cy - ry, cx + rx, cy + ry, p)
    }

    private fun dotAt(c: Canvas, p: Paint, cx: Float, cy: Float, r: Float, col: Int) {
        p.color = col
        p.style = Paint.Style.FILL
        c.drawCircle(cx, cy, r, p)
    }

    private fun line(c: Canvas, p: Paint, x1: Float, y1: Float, x2: Float, y2: Float, w: Float, col: Int) {
        p.color = col
        p.style = Paint.Style.STROKE
        p.strokeWidth = w
        c.drawLine(x1, y1, x2, y2, p)
        p.style = Paint.Style.FILL
    }

    private fun tri(c: Canvas, p: Paint, x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float, col: Int) {
        p.color = col
        p.style = Paint.Style.FILL
        path.reset()
        path.moveTo(x1, y1); path.lineTo(x2, y2); path.lineTo(x3, y3); path.close()
        c.drawPath(path, p)
    }

    /** 四条腿的走路循环 */
    private fun walkLegs(c: Canvas, p: Paint, s: Float, phase: Float, col: Int,
                         xs: FloatArray, y: Float, len: Float, w: Float) {
        for (i in xs.indices) {
            val sw = sin((phase + i * 1.7f).toDouble()).toFloat()
            line(c, p, xs[i], y, xs[i] + sw * len * 0.55f, y + len, w, col)
        }
    }

    private fun eye(c: Canvas, p: Paint, x: Float, y: Float, r: Float, col: Int) {
        dotAt(c, p, x, y, r, col)
        dotAt(c, p, x + r * 0.35f, y - r * 0.35f, r * 0.35f, Color.WHITE)
    }

    private fun glow(c: Canvas, p: Paint, r: Float, inner: Int, outer: Int) {
        p.style = Paint.Style.FILL
        p.shader = RadialGradient(0f, 0f, r, intArrayOf(inner, outer, Color.TRANSPARENT),
            floatArrayOf(0f, 0.45f, 1f), Shader.TileMode.CLAMP)
        c.drawCircle(0f, 0f, r, p)
        p.shader = null
    }

    // ── 小动物 ──────────────────────────────────────────────
    private fun mouse(c: Canvas, p: Paint, s: Float, ph: Float, t: PreyType) {
        // 尾巴：跟着跑动左右摆
        p.color = t.c1; p.style = Paint.Style.STROKE; p.strokeWidth = s * 0.045f
        path.reset()
        path.moveTo(-s * 0.38f, 0f)
        path.quadTo(-s * 0.72f, sin(ph.toDouble()).toFloat() * s * 0.22f, -s * 0.98f, sin((ph + 1.2f).toDouble()).toFloat() * s * 0.16f)
        c.drawPath(path, p)
        p.style = Paint.Style.FILL
        walkLegs(c, p, s, ph, t.c3, floatArrayOf(-s * 0.22f, s * 0.06f), s * 0.2f, s * 0.16f, s * 0.05f)
        oval(c, p, -s * 0.04f, 0f, s * 0.38f, s * 0.25f, t.c1)
        oval(c, p, s * 0.30f, s * 0.01f, s * 0.21f, s * 0.18f, t.c1)          // 头
        dotAt(c, p, s * 0.20f, -s * 0.20f, s * 0.15f, t.c1)                    // 耳
        dotAt(c, p, s * 0.20f, -s * 0.20f, s * 0.09f, t.c2)
        eye(c, p, s * 0.38f, -s * 0.04f, s * 0.045f, t.c3)
        dotAt(c, p, s * 0.50f, s * 0.05f, s * 0.04f, t.c2)                     // 鼻
        line(c, p, s * 0.50f, s * 0.05f, s * 0.72f, -s * 0.04f, s * 0.015f, t.c3)
        line(c, p, s * 0.50f, s * 0.05f, s * 0.72f, s * 0.13f, s * 0.015f, t.c3)
    }

    private fun fox(c: Canvas, p: Paint, s: Float, ph: Float, t: PreyType) {
        val sw = sin(ph.toDouble()).toFloat()
        c.save(); c.rotate(-18f + sw * 8f, -s * 0.32f, -s * 0.02f)
        oval(c, p, -s * 0.50f, -s * 0.06f, s * 0.30f, s * 0.16f, t.c1)         // 大尾巴
        dotAt(c, p, -s * 0.74f, -s * 0.10f, s * 0.11f, t.c2)                   // 白尾尖
        c.restore()
        walkLegs(c, p, s, ph, t.c3, floatArrayOf(-s * 0.18f, -s * 0.06f, s * 0.14f, s * 0.24f), s * 0.16f, s * 0.20f, s * 0.055f)
        oval(c, p, -s * 0.02f, 0f, s * 0.36f, s * 0.21f, t.c1)
        oval(c, p, s * 0.30f, -s * 0.08f, s * 0.20f, s * 0.18f, t.c1)          // 头
        tri(c, p, s * 0.16f, -s * 0.20f, s * 0.20f, -s * 0.44f, s * 0.32f, -s * 0.22f, t.c1)  // 耳
        tri(c, p, s * 0.34f, -s * 0.22f, s * 0.42f, -s * 0.44f, s * 0.46f, -s * 0.16f, t.c1)
        tri(c, p, s * 0.36f, -s * 0.06f, s * 0.60f, s * 0.03f, s * 0.36f, s * 0.10f, t.c2)    // 吻部
        dotAt(c, p, s * 0.60f, s * 0.03f, s * 0.035f, t.c3)
        eye(c, p, s * 0.36f, -s * 0.12f, s * 0.042f, t.c3)
        oval(c, p, s * 0.0f, s * 0.12f, s * 0.24f, s * 0.09f, t.c2)            // 白肚皮
    }

    private fun rabbit(c: Canvas, p: Paint, s: Float, ph: Float, t: PreyType) {
        dotAt(c, p, -s * 0.34f, -s * 0.02f, s * 0.12f, t.c1)                   // 短尾巴
        oval(c, p, -s * 0.02f, s * 0.02f, s * 0.33f, s * 0.26f, t.c1)
        val hop = abs(sin(ph.toDouble())).toFloat()
        oval(c, p, -s * 0.10f, s * 0.20f - hop * s * 0.05f, s * 0.16f, s * 0.10f, t.c1)  // 后腿
        line(c, p, s * 0.16f, s * 0.20f, s * 0.20f, s * 0.30f, s * 0.055f, t.c1)
        oval(c, p, s * 0.28f, -s * 0.10f, s * 0.18f, s * 0.16f, t.c1)          // 头
        // 长耳朵
        for (k in 0 until 2) {
            c.save()
            c.rotate(-14f + k * 22f + sin((ph * 0.7f + k).toDouble()).toFloat() * 5f, s * 0.26f, -s * 0.22f)
            oval(c, p, s * 0.26f, -s * 0.44f, s * 0.065f, s * 0.24f, t.c1)
            oval(c, p, s * 0.26f, -s * 0.44f, s * 0.032f, s * 0.17f, t.c2)
            c.restore()
        }
        eye(c, p, s * 0.36f, -s * 0.13f, s * 0.042f, t.c3)
        dotAt(c, p, s * 0.45f, -s * 0.03f, s * 0.032f, t.c2)
    }

    private fun squirrel(c: Canvas, p: Paint, s: Float, ph: Float, t: PreyType) {
        p.color = t.c2; p.style = Paint.Style.STROKE
        p.strokeWidth = s * 0.26f
        path.reset()
        path.moveTo(-s * 0.24f, s * 0.10f)
        path.cubicTo(-s * 0.66f, s * 0.10f, -s * 0.60f, -s * 0.46f,
            -s * 0.20f + sin(ph.toDouble()).toFloat() * s * 0.05f, -s * 0.44f)
        c.drawPath(path, p)                                                     // 蓬松大尾巴
        p.style = Paint.Style.FILL
        walkLegs(c, p, s, ph, t.c3, floatArrayOf(-s * 0.12f, s * 0.16f), s * 0.18f, s * 0.16f, s * 0.05f)
        oval(c, p, 0f, s * 0.02f, s * 0.30f, s * 0.21f, t.c1)
        oval(c, p, s * 0.28f, -s * 0.14f, s * 0.17f, s * 0.16f, t.c1)
        tri(c, p, s * 0.20f, -s * 0.26f, s * 0.22f, -s * 0.44f, s * 0.34f, -s * 0.28f, t.c1)
        eye(c, p, s * 0.36f, -s * 0.17f, s * 0.042f, t.c3)
        dotAt(c, p, s * 0.44f, -s * 0.08f, s * 0.032f, t.c3)
        oval(c, p, s * 0.10f, s * 0.10f, s * 0.16f, s * 0.10f, t.c2)
    }

    private fun cat(c: Canvas, p: Paint, s: Float, ph: Float, t: PreyType) {
        p.color = t.c1; p.style = Paint.Style.STROKE; p.strokeWidth = s * 0.07f
        path.reset()
        path.moveTo(-s * 0.36f, -s * 0.02f)
        path.quadTo(-s * 0.62f, -s * 0.16f + sin(ph.toDouble()).toFloat() * s * 0.10f, -s * 0.54f, -s * 0.40f)
        c.drawPath(path, p)                                                     // 竖起的尾巴
        p.style = Paint.Style.FILL
        walkLegs(c, p, s, ph, t.c1, floatArrayOf(-s * 0.24f, -s * 0.12f, s * 0.14f, s * 0.24f), s * 0.16f, s * 0.19f, s * 0.055f)
        oval(c, p, -s * 0.02f, 0f, s * 0.38f, s * 0.20f, t.c1)
        for (k in 0 until 3) {                                                  // 虎斑
            line(c, p, -s * 0.14f + k * s * 0.14f, -s * 0.16f, -s * 0.18f + k * s * 0.14f, s * 0.04f, s * 0.045f, t.c3)
        }
        oval(c, p, s * 0.34f, -s * 0.10f, s * 0.19f, s * 0.17f, t.c1)
        tri(c, p, s * 0.20f, -s * 0.22f, s * 0.22f, -s * 0.42f, s * 0.36f, -s * 0.24f, t.c1)
        tri(c, p, s * 0.38f, -s * 0.24f, s * 0.48f, -s * 0.42f, s * 0.50f, -s * 0.18f, t.c1)
        eye(c, p, s * 0.34f, -s * 0.12f, s * 0.045f, t.c3)
        eye(c, p, s * 0.46f, -s * 0.11f, s * 0.038f, t.c3)
        dotAt(c, p, s * 0.44f, -s * 0.01f, s * 0.030f, t.c2)
        line(c, p, s * 0.44f, -s * 0.01f, s * 0.72f, -s * 0.08f, s * 0.014f, t.c3)
        line(c, p, s * 0.44f, -s * 0.01f, s * 0.72f, s * 0.06f, s * 0.014f, t.c3)
    }

    private fun hedgehog(c: Canvas, p: Paint, s: Float, ph: Float, t: PreyType) {
        // 一圈尖刺
        p.color = t.c3
        for (k in 0 until 16) {
            val a = Math.PI * (1.05f + 0.92f * k / 15f)
            val cx = (cos(a) * s * 0.32f).toFloat() - s * 0.06f
            val cy = (sin(a) * s * 0.26f).toFloat()
            tri(c, p, cx, cy, cx + (cos(a) * s * 0.20f).toFloat(), cy + (sin(a) * s * 0.20f).toFloat(),
                cx + s * 0.07f, cy + s * 0.07f, t.c3)
        }
        oval(c, p, -s * 0.06f, 0f, s * 0.34f, s * 0.27f, t.c1)
        oval(c, p, s * 0.28f, s * 0.06f, s * 0.18f, s * 0.15f, t.c2)            // 脸
        tri(c, p, s * 0.34f, -s * 0.02f, s * 0.56f, s * 0.08f, s * 0.34f, s * 0.16f, t.c2)
        dotAt(c, p, s * 0.56f, s * 0.08f, s * 0.038f, t.c3)
        eye(c, p, s * 0.30f, s * 0.0f, s * 0.038f, t.c3)
        walkLegs(c, p, s, ph, t.c3, floatArrayOf(-s * 0.16f, s * 0.14f), s * 0.22f, s * 0.10f, s * 0.05f)
    }

    private fun raccoon(c: Canvas, p: Paint, s: Float, ph: Float, t: PreyType) {
        for (k in 0 until 5) {                                                  // 环纹尾巴
            dotAt(c, p, -s * 0.34f - k * s * 0.12f, -s * 0.04f - k * s * 0.03f,
                s * (0.15f - k * 0.012f), if (k % 2 == 0) t.c3 else t.c1)
        }
        walkLegs(c, p, s, ph, t.c3, floatArrayOf(-s * 0.20f, -s * 0.08f, s * 0.12f, s * 0.22f), s * 0.16f, s * 0.19f, s * 0.055f)
        oval(c, p, -s * 0.02f, 0f, s * 0.35f, s * 0.21f, t.c1)
        oval(c, p, s * 0.32f, -s * 0.08f, s * 0.19f, s * 0.17f, t.c2)
        dotAt(c, p, s * 0.20f, -s * 0.22f, s * 0.08f, t.c1)                     // 耳
        dotAt(c, p, s * 0.44f, -s * 0.22f, s * 0.08f, t.c1)
        oval(c, p, s * 0.34f, -s * 0.10f, s * 0.17f, s * 0.07f, t.c3)           // 黑眼罩
        dotAt(c, p, s * 0.28f, -s * 0.10f, s * 0.028f, Color.WHITE)
        dotAt(c, p, s * 0.42f, -s * 0.10f, s * 0.028f, Color.WHITE)
        dotAt(c, p, s * 0.50f, s * 0.02f, s * 0.032f, t.c3)
    }

    private fun duck(c: Canvas, p: Paint, s: Float, ph: Float, t: PreyType) {
        tri(c, p, -s * 0.30f, -s * 0.02f, -s * 0.52f, -s * 0.22f, -s * 0.26f, s * 0.10f, t.c1)  // 尾
        oval(c, p, -s * 0.04f, s * 0.04f, s * 0.34f, s * 0.22f, t.c1)
        oval(c, p, s * 0.04f, s * 0.06f, s * 0.18f, s * 0.13f, t.c2)            // 翅膀
        oval(c, p, s * 0.26f, -s * 0.20f, s * 0.16f, s * 0.15f, t.c1)           // 头
        tri(c, p, s * 0.38f, -s * 0.24f, s * 0.62f, -s * 0.16f, s * 0.38f, -s * 0.10f, t.c2)   // 嘴
        eye(c, p, s * 0.30f, -s * 0.24f, s * 0.038f, t.c3)
        val sw = sin(ph.toDouble()).toFloat()
        line(c, p, -s * 0.02f, s * 0.24f, -s * 0.02f + sw * s * 0.08f, s * 0.38f, s * 0.05f, t.c2)
        line(c, p, s * 0.14f, s * 0.24f, s * 0.14f - sw * s * 0.08f, s * 0.38f, s * 0.05f, t.c2)
    }

    private fun chick(c: Canvas, p: Paint, s: Float, ph: Float, t: PreyType) {
        oval(c, p, -s * 0.04f, s * 0.06f, s * 0.28f, s * 0.24f, t.c1)
        oval(c, p, s * 0.04f, s * 0.08f, s * 0.14f, s * 0.11f, t.c2)
        dotAt(c, p, s * 0.24f, -s * 0.18f, s * 0.20f, t.c1)                     // 头
        tri(c, p, s * 0.40f, -s * 0.22f, s * 0.58f, -s * 0.14f, s * 0.40f, -s * 0.08f, t.c2)
        eye(c, p, s * 0.28f, -s * 0.22f, s * 0.040f, t.c3)
        val sw = sin(ph.toDouble()).toFloat()
        line(c, p, -s * 0.02f, s * 0.28f, -s * 0.02f + sw * s * 0.09f, s * 0.44f, s * 0.045f, t.c2)
        line(c, p, s * 0.14f, s * 0.28f, s * 0.14f - sw * s * 0.09f, s * 0.44f, s * 0.045f, t.c2)
    }

    private fun sheep(c: Canvas, p: Paint, s: Float, ph: Float, t: PreyType) {
        walkLegs(c, p, s, ph, t.c3, floatArrayOf(-s * 0.18f, s * 0.14f), s * 0.18f, s * 0.20f, s * 0.05f)
        for (k in 0 until 6) {                                                  // 一团羊毛
            val a = Math.PI * 2 * k / 6
            dotAt(c, p, -s * 0.04f + (cos(a) * s * 0.20f).toFloat(), (sin(a) * s * 0.14f).toFloat(), s * 0.17f, t.c1)
        }
        oval(c, p, -s * 0.04f, 0f, s * 0.24f, s * 0.18f, t.c1)
        oval(c, p, s * 0.30f, -s * 0.08f, s * 0.15f, s * 0.14f, t.c2)           // 深色的脸
        oval(c, p, s * 0.22f, -s * 0.18f, s * 0.08f, s * 0.05f, t.c2)           // 耳
        eye(c, p, s * 0.34f, -s * 0.12f, s * 0.036f, t.c3)
        dotAt(c, p, s * 0.43f, -s * 0.02f, s * 0.028f, t.c3)
    }

    private fun pig(c: Canvas, p: Paint, s: Float, ph: Float, t: PreyType) {
        p.color = t.c1; p.style = Paint.Style.STROKE; p.strokeWidth = s * 0.045f
        path.reset()
        path.moveTo(-s * 0.32f, -s * 0.04f)
        path.cubicTo(-s * 0.50f, -s * 0.18f, -s * 0.38f, -s * 0.24f, -s * 0.44f, -s * 0.10f)
        c.drawPath(path, p)                                                     // 卷尾巴
        p.style = Paint.Style.FILL
        walkLegs(c, p, s, ph, t.c3, floatArrayOf(-s * 0.18f, -s * 0.06f, s * 0.12f, s * 0.22f), s * 0.18f, s * 0.16f, s * 0.06f)
        oval(c, p, -s * 0.02f, 0f, s * 0.36f, s * 0.24f, t.c1)
        oval(c, p, s * 0.32f, -s * 0.04f, s * 0.19f, s * 0.18f, t.c1)
        tri(c, p, s * 0.22f, -s * 0.18f, s * 0.26f, -s * 0.38f, s * 0.38f, -s * 0.20f, t.c2)
        oval(c, p, s * 0.50f, s * 0.02f, s * 0.10f, s * 0.08f, t.c2)            // 猪鼻
        dotAt(c, p, s * 0.47f, s * 0.02f, s * 0.020f, t.c3)
        dotAt(c, p, s * 0.54f, s * 0.02f, s * 0.020f, t.c3)
        eye(c, p, s * 0.34f, -s * 0.10f, s * 0.036f, t.c3)
    }

    private fun frog(c: Canvas, p: Paint, s: Float, ph: Float, t: PreyType) {
        val hop = abs(sin(ph.toDouble())).toFloat()
        oval(c, p, -s * 0.16f, s * 0.12f, s * 0.18f, s * 0.11f, t.c1)           // 后腿
        line(c, p, -s * 0.16f, s * 0.16f, -s * 0.34f, s * 0.24f - hop * s * 0.10f, s * 0.06f, t.c1)
        oval(c, p, 0f, s * 0.02f, s * 0.36f, s * 0.25f, t.c1)
        oval(c, p, 0f, s * 0.10f, s * 0.22f, s * 0.13f, t.c2)                   // 浅色肚皮
        dotAt(c, p, s * 0.12f, -s * 0.26f, s * 0.13f, t.c1)                     // 两只鼓眼
        dotAt(c, p, s * 0.32f, -s * 0.26f, s * 0.13f, t.c1)
        dotAt(c, p, s * 0.13f, -s * 0.27f, s * 0.07f, Color.WHITE)
        dotAt(c, p, s * 0.33f, -s * 0.27f, s * 0.07f, Color.WHITE)
        dotAt(c, p, s * 0.15f, -s * 0.27f, s * 0.038f, t.c3)
        dotAt(c, p, s * 0.35f, -s * 0.27f, s * 0.038f, t.c3)
        line(c, p, s * 0.14f, s * 0.02f, s * 0.36f, s * 0.02f, s * 0.028f, t.c3)  // 大嘴
        line(c, p, s * 0.24f, s * 0.14f, s * 0.36f, s * 0.24f, s * 0.055f, t.c1)  // 前腿
    }

    private fun snake(c: Canvas, p: Paint, s: Float, ph: Float, t: PreyType) {
        p.style = Paint.Style.STROKE
        p.strokeWidth = s * 0.17f
        p.color = t.c1
        path.reset()
        var first = true
        var x = -s * 0.55f
        while (x <= s * 0.42f) {
            val y = sin((x / s * 9f + ph * 2f).toDouble()).toFloat() * s * 0.14f * ((x + s * 0.6f) / s)
            if (first) { path.moveTo(x, y); first = false } else path.lineTo(x, y)
            x += s * 0.04f
        }
        c.drawPath(path, p)
        p.strokeWidth = s * 0.06f
        p.color = t.c2
        c.drawPath(path, p)                                                     // 背脊纹
        p.style = Paint.Style.FILL
        val hy = sin((0.42f / 1f * 9f + ph * 2f).toDouble()).toFloat() * s * 0.14f
        oval(c, p, s * 0.46f, hy, s * 0.14f, s * 0.11f, t.c1)                   // 头
        dotAt(c, p, s * 0.48f, hy - s * 0.05f, s * 0.030f, t.c3)
        line(c, p, s * 0.58f, hy, s * 0.74f, hy - s * 0.05f, s * 0.018f, 0xFFFF6B6B.toInt())  // 信子
        line(c, p, s * 0.58f, hy, s * 0.74f, hy + s * 0.05f, s * 0.018f, 0xFFFF6B6B.toInt())
    }

    private fun crab(c: Canvas, p: Paint, s: Float, ph: Float, t: PreyType) {
        val sw = sin(ph.toDouble()).toFloat()
        for (k in 0 until 3) {                                                  // 三对步足
            val yy = -s * 0.10f + k * s * 0.14f
            line(c, p, -s * 0.20f, yy, -s * 0.44f, yy - s * 0.12f + sw * s * 0.06f, s * 0.045f, t.c1)
            line(c, p, s * 0.02f, yy, s * 0.26f, yy - s * 0.12f - sw * s * 0.06f, s * 0.045f, t.c1)
        }
        oval(c, p, -s * 0.06f, 0f, s * 0.30f, s * 0.22f, t.c1)                  // 蟹壳
        oval(c, p, -s * 0.06f, s * 0.02f, s * 0.20f, s * 0.13f, t.c2)
        // 两只大钳
        for (k in 0 until 2) {
            val yy = if (k == 0) -s * 0.22f else s * 0.22f
            line(c, p, s * 0.16f, yy * 0.6f, s * 0.34f, yy, s * 0.055f, t.c1)
            oval(c, p, s * 0.44f, yy, s * 0.13f, s * 0.09f, t.c1)
            line(c, p, s * 0.44f, yy, s * 0.58f, yy - s * 0.05f, s * 0.03f, t.c3)
        }
        dotAt(c, p, s * 0.06f, -s * 0.14f, s * 0.045f, t.c3)                    // 眼柄
        dotAt(c, p, s * 0.06f, s * 0.14f, s * 0.045f, t.c3)
    }

    private fun fish(c: Canvas, p: Paint, s: Float, ph: Float, t: PreyType) {
        val sw = sin((ph * 2f).toDouble()).toFloat()
        tri(c, p, -s * 0.24f, 0f, -s * 0.52f, -s * 0.20f + sw * s * 0.08f, -s * 0.52f, s * 0.20f + sw * s * 0.08f, t.c1)
        oval(c, p, s * 0.04f, 0f, s * 0.36f, s * 0.20f, t.c1)
        tri(c, p, -s * 0.06f, -s * 0.16f, s * 0.02f, -s * 0.36f, s * 0.14f, -s * 0.14f, t.c2)  // 背鳍
        tri(c, p, s * 0.0f, s * 0.16f, s * 0.06f, s * 0.32f, s * 0.16f, s * 0.14f, t.c2)
        oval(c, p, s * 0.10f, s * 0.05f, s * 0.20f, s * 0.10f, t.c2)
        eye(c, p, s * 0.28f, -s * 0.05f, s * 0.046f, t.c3)
        line(c, p, s * 0.16f, -s * 0.14f, s * 0.16f, s * 0.14f, s * 0.022f, t.c3)  // 鳃线
    }

    // ── 虫 · 鸟 ────────────────────────────────────────────
    private fun beetle(c: Canvas, p: Paint, s: Float, ph: Float, t: PreyType) {
        val sw = sin((ph * 2.2f).toDouble()).toFloat()
        for (k in 0 until 3) {                                                  // 六条腿
            val xx = -s * 0.16f + k * s * 0.16f
            line(c, p, xx, -s * 0.14f, xx - s * 0.06f, -s * 0.34f + sw * s * 0.05f, s * 0.04f, t.c3)
            line(c, p, xx, s * 0.14f, xx - s * 0.06f, s * 0.34f - sw * s * 0.05f, s * 0.04f, t.c3)
        }
        oval(c, p, -s * 0.04f, 0f, s * 0.34f, s * 0.24f, t.c1)                  // 鞘翅
        line(c, p, -s * 0.34f, 0f, s * 0.22f, 0f, s * 0.035f, t.c2)
        oval(c, p, s * 0.16f, 0f, s * 0.14f, s * 0.20f, t.c2)                   // 前胸
        dotAt(c, p, s * 0.34f, 0f, s * 0.11f, t.c3)                             // 头
        line(c, p, s * 0.40f, -s * 0.05f, s * 0.60f, -s * 0.18f, s * 0.025f, t.c3)
        line(c, p, s * 0.40f, s * 0.05f, s * 0.60f, s * 0.18f, s * 0.025f, t.c3)
    }

    private fun butterfly(c: Canvas, p: Paint, s: Float, ph: Float, t: PreyType) {
        val flap = 0.30f + 0.70f * abs(cos(ph.toDouble())).toFloat()            // 拍翅：横向压扁
        for (side in intArrayOf(-1, 1)) {
            val y = side * s * 0.06f
            c.save()
            c.translate(0f, y)
            c.scale(1f, flap * side.toFloat(), 0f, 0f)
            oval(c, p, -s * 0.02f, s * 0.22f, s * 0.24f, s * 0.24f, t.c1)       // 上翅
            oval(c, p, -s * 0.20f, s * 0.34f, s * 0.16f, s * 0.17f, t.c2)       // 下翅
            dotAt(c, p, s * 0.02f, s * 0.22f, s * 0.07f, t.c2)                  // 翅斑
            dotAt(c, p, -s * 0.20f, s * 0.34f, s * 0.05f, t.c1)
            c.restore()
        }
        oval(c, p, 0f, 0f, s * 0.26f, s * 0.055f, t.c3)                         // 身体
        line(c, p, s * 0.22f, 0f, s * 0.40f, -s * 0.12f, s * 0.022f, t.c3)
        line(c, p, s * 0.22f, 0f, s * 0.40f, s * 0.12f, s * 0.022f, t.c3)
    }

    private fun bee(c: Canvas, p: Paint, s: Float, ph: Float, t: PreyType) {
        val flap = 0.25f + 0.75f * abs(cos((ph * 3f).toDouble())).toFloat()
        p.color = Color.argb(150, 235, 248, 255)
        for (side in intArrayOf(-1, 1)) {
            c.save(); c.translate(0f, side * s * 0.10f); c.scale(1f, flap * side.toFloat(), 0f, 0f)
            oval(c, p, -s * 0.02f, s * 0.20f, s * 0.22f, s * 0.13f, Color.argb(160, 235, 248, 255))
            c.restore()
        }
        oval(c, p, -s * 0.04f, 0f, s * 0.30f, s * 0.19f, t.c1)
        for (k in 0 until 3) {                                                  // 黑黄条纹
            oval(c, p, -s * 0.20f + k * s * 0.15f, 0f, s * 0.045f, s * 0.18f, t.c2)
        }
        dotAt(c, p, s * 0.28f, 0f, s * 0.13f, t.c2)                             // 头
        dotAt(c, p, s * 0.32f, -s * 0.05f, s * 0.030f, t.c3)
        tri(c, p, -s * 0.34f, -s * 0.05f, -s * 0.50f, 0f, -s * 0.34f, s * 0.05f, t.c2)  // 尾刺
    }

    private fun spider(c: Canvas, p: Paint, s: Float, ph: Float, t: PreyType) {
        p.color = t.c2; p.style = Paint.Style.STROKE; p.strokeWidth = s * 0.038f
        for (k in 0 until 4) {                                                  // 八条带膝盖的腿
            val sw = sin((ph * 2.4f + k * 1.1f).toDouble()).toFloat() * s * 0.07f
            for (side in intArrayOf(-1, 1)) {
                val bx = -s * 0.10f + k * s * 0.11f
                val by = side * s * 0.10f
                path.reset()
                path.moveTo(bx, by)
                path.lineTo(bx - s * 0.10f + sw, by + side * s * 0.24f)
                path.lineTo(bx - s * 0.24f + sw, by + side * s * 0.14f)
                c.drawPath(path, p)
            }
        }
        p.style = Paint.Style.FILL
        oval(c, p, -s * 0.18f, 0f, s * 0.24f, s * 0.20f, t.c1)                  // 腹部
        oval(c, p, s * 0.16f, 0f, s * 0.15f, s * 0.13f, t.c2)                   // 头胸
        dotAt(c, p, s * 0.24f, -s * 0.06f, s * 0.030f, t.c3)
        dotAt(c, p, s * 0.24f, s * 0.06f, s * 0.030f, t.c3)
    }

    private fun dragonfly(c: Canvas, p: Paint, s: Float, ph: Float, t: PreyType) {
        val flap = 0.20f + 0.80f * abs(cos((ph * 4f).toDouble())).toFloat()
        for (side in intArrayOf(-1, 1)) {
            c.save(); c.scale(1f, flap * side.toFloat(), 0f, 0f)
            oval(c, p, s * 0.10f, s * 0.22f, s * 0.22f, s * 0.075f, Color.argb(155, 220, 245, 255))
            oval(c, p, -s * 0.10f, s * 0.20f, s * 0.20f, s * 0.070f, Color.argb(130, 200, 235, 255))
            c.restore()
        }
        oval(c, p, -s * 0.10f, 0f, s * 0.40f, s * 0.045f, t.c1)                 // 细长身体
        dotAt(c, p, s * 0.34f, 0f, s * 0.11f, t.c2)                             // 大复眼
        dotAt(c, p, s * 0.32f, -s * 0.06f, s * 0.045f, t.c3)
        dotAt(c, p, s * 0.32f, s * 0.06f, s * 0.045f, t.c3)
    }

    private fun bird(c: Canvas, p: Paint, s: Float, ph: Float, t: PreyType) {
        tri(c, p, -s * 0.26f, 0f, -s * 0.52f, -s * 0.14f, -s * 0.50f, s * 0.12f, t.c2)  // 尾羽
        oval(c, p, -s * 0.02f, s * 0.02f, s * 0.30f, s * 0.22f, t.c1)
        dotAt(c, p, s * 0.26f, -s * 0.16f, s * 0.17f, t.c1)                     // 头
        tri(c, p, s * 0.40f, -s * 0.20f, s * 0.60f, -s * 0.13f, s * 0.40f, -s * 0.08f, t.c2)  // 喙
        eye(c, p, s * 0.30f, -s * 0.20f, s * 0.038f, t.c3)
        c.save()                                                                // 扇动的翅膀
        c.rotate(sin((ph * 3f).toDouble()).toFloat() * 32f, 0f, -s * 0.02f)
        oval(c, p, -s * 0.04f, -s * 0.06f, s * 0.22f, s * 0.11f, t.c2)
        c.restore()
        line(c, p, s * 0.02f, s * 0.22f, s * 0.02f, s * 0.36f, s * 0.035f, t.c2)
        line(c, p, s * 0.14f, s * 0.22f, s * 0.14f, s * 0.36f, s * 0.035f, t.c2)
    }

    private fun firefly(c: Canvas, p: Paint, s: Float, ph: Float, t: PreyType) {
        val pulse = 0.55f + 0.45f * sin((ph * 2.2f).toDouble()).toFloat()
        c.save(); c.translate(-s * 0.16f, 0f)
        glow(c, p, s * 0.85f * pulse, Color.argb((200 * pulse).toInt(), 233, 255, 122),
            Color.argb((70 * pulse).toInt(), 200, 255, 90))
        c.restore()
        oval(c, p, s * 0.06f, 0f, s * 0.20f, s * 0.11f, t.c3)                   // 身体
        dotAt(c, p, -s * 0.16f, 0f, s * 0.13f, Color.argb(255, 245, 255, 190))  // 发光的尾部
        dotAt(c, p, s * 0.26f, 0f, s * 0.09f, t.c3)
        oval(c, p, s * 0.02f, -s * 0.10f, s * 0.16f, s * 0.05f, Color.argb(120, 255, 255, 230))
    }

    // ── 玩具 · 光点 ────────────────────────────────────────
    private fun dot(c: Canvas, p: Paint, s: Float, ph: Float, t: PreyType) {
        val pulse = 0.88f + 0.12f * sin((ph * 3f).toDouble()).toFloat()
        glow(c, p, s * 1.7f * pulse, Color.argb(170, Color.red(t.c1), Color.green(t.c1), Color.blue(t.c1)),
            Color.argb(45, Color.red(t.c1), Color.green(t.c1), Color.blue(t.c1)))
        dotAt(c, p, 0f, 0f, s * 0.42f * pulse, t.c1)
        dotAt(c, p, 0f, 0f, s * 0.20f * pulse, t.c2)
        dotAt(c, p, -s * 0.05f, -s * 0.05f, s * 0.08f, t.c3)
    }

    private fun tennis(c: Canvas, p: Paint, s: Float, ph: Float, t: PreyType) {
        c.save(); c.rotate(ph * 40f)
        dotAt(c, p, 0f, 0f, s * 0.5f, t.c1)
        dotAt(c, p, -s * 0.13f, -s * 0.13f, s * 0.18f, Color.argb(60, 255, 255, 255))
        p.color = t.c2; p.style = Paint.Style.STROKE; p.strokeWidth = s * 0.07f
        rect.set(-s * 0.86f, -s * 0.5f, -s * 0.14f, s * 0.5f)
        c.drawArc(rect, -60f, 120f, false, p)                                   // 网球缝线
        rect.set(s * 0.14f, -s * 0.5f, s * 0.86f, s * 0.5f)
        c.drawArc(rect, 120f, 120f, false, p)
        p.style = Paint.Style.FILL
        c.restore()
    }

    private fun frisbee(c: Canvas, p: Paint, s: Float, ph: Float, t: PreyType) {
        c.save(); c.rotate(ph * 60f)
        oval(c, p, 0f, s * 0.05f, s * 0.5f, s * 0.22f, t.c3)                    // 厚度阴影
        oval(c, p, 0f, 0f, s * 0.5f, s * 0.22f, t.c1)
        oval(c, p, 0f, 0f, s * 0.34f, s * 0.14f, t.c2)
        oval(c, p, 0f, 0f, s * 0.20f, s * 0.08f, t.c1)
        c.restore()
    }

    private fun bone(c: Canvas, p: Paint, s: Float, ph: Float, t: PreyType) {
        c.save(); c.rotate(sin(ph.toDouble()).toFloat() * 14f)
        p.color = t.c1; p.style = Paint.Style.FILL
        rect.set(-s * 0.34f, -s * 0.11f, s * 0.34f, s * 0.11f)
        c.drawRoundRect(rect, s * 0.11f, s * 0.11f, p)
        for (sx in intArrayOf(-1, 1)) for (sy in intArrayOf(-1, 1)) {
            dotAt(c, p, sx * s * 0.36f, sy * s * 0.15f, s * 0.15f, t.c1)
        }
        for (sx in intArrayOf(-1, 1)) for (sy in intArrayOf(-1, 1)) {
            dotAt(c, p, sx * s * 0.38f, sy * s * 0.17f, s * 0.06f, t.c2)
        }
        c.restore()
    }

    private fun rope(c: Canvas, p: Paint, s: Float, ph: Float, t: PreyType) {
        val wag = sin(ph.toDouble()).toFloat() * 10f
        c.save(); c.rotate(wag)
        p.color = t.c1; p.style = Paint.Style.STROKE; p.strokeWidth = s * 0.16f
        path.reset(); path2.reset()                                             // 两股拧在一起
        var x = -s * 0.34f
        var first = true
        while (x <= s * 0.34f) {
            val y = sin((x / s * 14f).toDouble()).toFloat() * s * 0.09f
            if (first) { path.moveTo(x, y); path2.moveTo(x, -y); first = false }
            else { path.lineTo(x, y); path2.lineTo(x, -y) }
            x += s * 0.03f
        }
        c.drawPath(path, p)
        p.color = t.c2
        c.drawPath(path2, p)
        p.style = Paint.Style.FILL
        for (sx in intArrayOf(-1, 1)) {                                         // 两端的结
            dotAt(c, p, sx * s * 0.40f, 0f, s * 0.15f, t.c1)
            for (k in 0 until 4) {
                line(c, p, sx * s * 0.48f, 0f, sx * s * 0.62f, (k - 1.5f) * s * 0.09f, s * 0.035f, t.c2)
            }
        }
        c.restore()
    }

    private fun bubble(c: Canvas, p: Paint, s: Float, ph: Float, t: PreyType) {
        val wob = 1f + 0.06f * sin((ph * 2f).toDouble()).toFloat()
        oval(c, p, 0f, 0f, s * 0.5f * wob, s * 0.5f / wob, Color.argb(70, 205, 235, 255))
        p.color = Color.argb(190, 235, 248, 255)
        p.style = Paint.Style.STROKE
        p.strokeWidth = s * 0.045f
        c.drawOval(-s * 0.5f * wob, -s * 0.5f / wob, s * 0.5f * wob, s * 0.5f / wob, p)
        p.strokeWidth = s * 0.06f
        p.color = Color.argb(220, 255, 255, 255)
        rect.set(-s * 0.34f, -s * 0.34f, s * 0.10f, s * 0.10f)
        c.drawArc(rect, 150f, 80f, false, p)                                    // 高光
        p.style = Paint.Style.FILL
        dotAt(c, p, s * 0.16f, s * 0.18f, s * 0.05f, Color.argb(140, 255, 255, 255))
    }

    private fun star(c: Canvas, p: Paint, s: Float, ph: Float, t: PreyType) {
        glow(c, p, s * 1.1f, Color.argb(90, 255, 235, 120), Color.argb(25, 255, 220, 90))
        c.save(); c.rotate(ph * 22f)
        path.reset()
        for (k in 0 until 10) {
            val r = if (k % 2 == 0) s * 0.52f else s * 0.22f
            val a = Math.PI * 2 * k / 10 - Math.PI / 2
            val x = (cos(a) * r).toFloat()
            val y = (sin(a) * r).toFloat()
            if (k == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        p.color = t.c1; p.style = Paint.Style.FILL
        c.drawPath(path, p)
        p.color = t.c3; p.style = Paint.Style.STROKE; p.strokeWidth = s * 0.035f
        c.drawPath(path, p)
        p.style = Paint.Style.FILL
        dotAt(c, p, -s * 0.10f, -s * 0.10f, s * 0.09f, t.c2)
        c.restore()
    }

    private fun feather(c: Canvas, p: Paint, s: Float, ph: Float, t: PreyType) {
        c.save(); c.rotate(sin(ph.toDouble()).toFloat() * 16f)
        for (side in intArrayOf(-1, 1)) {                                       // 两侧羽片
            path.reset()
            path.moveTo(-s * 0.42f, 0f)
            path.quadTo(-s * 0.10f, side * s * 0.30f, s * 0.34f, side * s * 0.06f)
            path.quadTo(s * 0.05f, side * s * 0.05f, -s * 0.42f, 0f)
            path.close()
            p.color = if (side < 0) t.c1 else t.c2
            p.style = Paint.Style.FILL
            c.drawPath(path, p)
        }
        line(c, p, -s * 0.48f, 0f, s * 0.40f, s * 0.03f, s * 0.035f, t.c3)      // 羽轴
        c.restore()
    }

    private fun customBmp(c: Canvas, p: Paint, s: Float, bmp: Bitmap?) {
        if (bmp == null) {
            // 没设置图片时给个占位：黄色圆点 + 问号感的缺口
            dotAt(c, p, 0f, 0f, s * 0.5f, 0xFFFFE066.toInt())
            dotAt(c, p, s * 0.12f, -s * 0.12f, s * 0.16f, 0xFF2B3242.toInt())
            return
        }
        val bw = bmp.width.toFloat()
        val bh = bmp.height.toFloat()
        if (bw <= 0f || bh <= 0f) return
        val scale = (s * 1.25f) / maxOf(bw, bh)
        val dw = bw * scale
        val dh = bh * scale
        rect.set(-dw / 2f, -dh / 2f, dw / 2f, dh / 2f)
        p.color = Color.WHITE
        c.drawBitmap(bmp, Rect(0, 0, bmp.width, bmp.height), rect, p)
    }
}
