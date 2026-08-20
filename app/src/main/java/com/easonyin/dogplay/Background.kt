package com.easonyin.dogplay

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import kotlin.math.sin
import kotlin.random.Random

enum class BgType(val key: String, val labelRes: Int, val hintRes: Int) {
    DARK("dark", R.string.bg_dark, R.string.bghint_dark),
    GRASS("grass", R.string.bg_grass, R.string.bghint_grass),
    NIGHT("night", R.string.bg_night, R.string.bghint_night),
    SNOW("snow", R.string.bg_snow, R.string.bghint_snow),
    WOOD("wood", R.string.bg_wood, R.string.bghint_wood),
    CARPET("carpet", R.string.bg_carpet, R.string.bghint_carpet),
    BEACH("beach", R.string.bg_beach, R.string.bghint_beach),
    SKY("sky", R.string.bg_sky, R.string.bghint_sky),
    FOREST("forest", R.string.bg_forest, R.string.bghint_forest),
    CUSTOM("custom", R.string.bg_custom, R.string.bghint_custom);

    companion object {
        val builtin: List<BgType> by lazy { entries.filter { it != CUSTOM } }
        fun fromKey(k: String?): BgType = entries.firstOrNull { it.key == k } ?: GRASS
    }
}

object BackgroundRenderer {

    /** 生成一张铺满全屏的背景位图；只在尺寸变化时调用一次 */
    fun render(type: BgType, w: Int, h: Int, custom: Bitmap?): Bitmap {
        val bmp = Bitmap.createBitmap(w.coerceAtLeast(1), h.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val rnd = Random(type.ordinal * 7919 + 13)

        if (type == BgType.CUSTOM && custom != null) {
            drawCoverCrop(c, custom, w, h, p)
            // 压暗一点，保证猎物比背景亮，狗才看得清
            p.shader = null
            p.color = Color.argb(90, 6, 10, 20)
            c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), p)
            vignette(c, w, h)
            return bmp
        }

        when (type) {
            BgType.DARK -> {
                vertGradient(c, p, w, h, 0xFF111A28.toInt(), 0xFF060A12.toInt())
            }

            BgType.GRASS -> {
                vertGradient(c, p, w, h, 0xFF1E4636.toInt(), 0xFF0C2119.toInt())
                // 一丛丛草叶剪影
                p.shader = null
                p.style = Paint.Style.FILL
                for (i in 0 until (w * h / 5200).coerceIn(120, 900)) {
                    val x = rnd.nextFloat() * w
                    val y = h * (0.25f + 0.75f * rnd.nextFloat())
                    val len = h * (0.03f + 0.09f * rnd.nextFloat()) * (y / h)
                    val lean = (rnd.nextFloat() - 0.5f) * len * 0.7f
                    val shade = 0.25f + 0.5f * rnd.nextFloat()
                    p.color = blend(0xFF2E6B4E.toInt(), 0xFF0C2119.toInt(), 1f - shade)
                    p.strokeWidth = 1f + 2.5f * rnd.nextFloat()
                    p.style = Paint.Style.STROKE
                    val path = Path()
                    path.moveTo(x, y)
                    path.quadTo(x + lean * 0.4f, y - len * 0.6f, x + lean, y - len)
                    c.drawPath(path, p)
                }
                p.style = Paint.Style.FILL
            }

            BgType.NIGHT -> {
                vertGradient(c, p, w, h, 0xFF101B3A.toInt(), 0xFF03060F.toInt())
                p.shader = null
                for (i in 0 until 220) {
                    val x = rnd.nextFloat() * w
                    val y = rnd.nextFloat() * h * 0.9f
                    val r = 0.6f + 1.8f * rnd.nextFloat()
                    p.color = Color.argb((90 + rnd.nextInt(150)), 255, 255, 235)
                    c.drawCircle(x, y, r, p)
                }
                // 月亮
                p.color = 0xFFF3EBC8.toInt()
                c.drawCircle(w * 0.84f, h * 0.16f, minOf(w, h) * 0.07f, p)
                p.color = 0xFF101B3A.toInt()
                c.drawCircle(w * 0.80f, h * 0.13f, minOf(w, h) * 0.066f, p)
            }

            BgType.SNOW -> {
                vertGradient(c, p, w, h, 0xFFDCE9F5.toInt(), 0xFF9FB6CC.toInt())
                p.shader = null
                // 起伏的雪堆
                for (k in 0 until 4) {
                    val base = h * (0.55f + 0.13f * k)
                    val path = Path()
                    path.moveTo(0f, h.toFloat())
                    path.lineTo(0f, base)
                    var x = 0f
                    while (x < w) {
                        path.quadTo(x + w * 0.08f, base - h * 0.05f * sin((x / w * 6f + k).toDouble()).toFloat(), x + w * 0.16f, base)
                        x += w * 0.16f
                    }
                    path.lineTo(w.toFloat(), h.toFloat())
                    path.close()
                    p.color = blend(0xFFFFFFFF.toInt(), 0xFFB9CCDD.toInt(), k / 3f)
                    c.drawPath(path, p)
                }
            }

            BgType.WOOD -> {
                vertGradient(c, p, w, h, 0xFF6B4A2C.toInt(), 0xFF3A2716.toInt())
                p.shader = null
                val planks = 7
                val ph = h.toFloat() / planks
                for (i in 0 until planks) {
                    val y = i * ph
                    p.color = blend(0xFF7A552F.toInt(), 0xFF4A3220.toInt(), rnd.nextFloat() * 0.8f)
                    c.drawRect(0f, y, w.toFloat(), y + ph - 2f, p)
                    // 木纹
                    p.style = Paint.Style.STROKE
                    p.strokeWidth = 1.2f
                    for (g in 0 until 5) {
                        p.color = Color.argb(38, 30, 18, 8)
                        val gy = y + ph * (0.15f + 0.7f * rnd.nextFloat())
                        val path = Path()
                        path.moveTo(0f, gy)
                        var x = 0f
                        while (x < w) {
                            path.quadTo(x + w * 0.1f, gy + (rnd.nextFloat() - 0.5f) * ph * 0.18f, x + w * 0.2f, gy)
                            x += w * 0.2f
                        }
                        c.drawPath(path, p)
                    }
                    p.style = Paint.Style.FILL
                    p.color = Color.argb(70, 20, 12, 5)
                    c.drawRect(0f, y + ph - 2f, w.toFloat(), y + ph, p)
                }
            }

            BgType.CARPET -> {
                vertGradient(c, p, w, h, 0xFF2A3350.toInt(), 0xFF161C2E.toInt())
                p.shader = null
                // 绒毛质感：大量短线
                p.style = Paint.Style.STROKE
                p.strokeWidth = 1.4f
                for (i in 0 until (w * h / 2600).coerceIn(300, 2600)) {
                    val x = rnd.nextFloat() * w
                    val y = rnd.nextFloat() * h
                    p.color = Color.argb(26 + rnd.nextInt(30), 190, 205, 235)
                    c.drawLine(x, y, x + (rnd.nextFloat() - 0.5f) * 7f, y - 3f - rnd.nextFloat() * 5f, p)
                }
                p.style = Paint.Style.FILL
            }

            BgType.BEACH -> {
                vertGradient(c, p, w, h, 0xFFF2DFA8.toInt(), 0xFFCBAF74.toInt())
                p.shader = null
                // 上方一条海
                p.color = 0xFF3E8FB0.toInt()
                c.drawRect(0f, 0f, w.toFloat(), h * 0.26f, p)
                p.color = 0xFF6FC2D8.toInt()
                c.drawRect(0f, h * 0.22f, w.toFloat(), h * 0.27f, p)
                p.color = 0xFFFFFFFF.toInt()
                for (i in 0 until 40) {
                    val x = rnd.nextFloat() * w
                    val y = h * (0.20f + 0.07f * rnd.nextFloat())
                    c.drawRect(x, y, x + 10f + rnd.nextFloat() * 30f, y + 2.5f, p)
                }
                // 沙粒
                for (i in 0 until 900) {
                    val x = rnd.nextFloat() * w
                    val y = h * 0.28f + rnd.nextFloat() * h * 0.72f
                    p.color = Color.argb(40 + rnd.nextInt(50), 120, 95, 55)
                    c.drawCircle(x, y, 0.8f + rnd.nextFloat(), p)
                }
            }

            BgType.SKY -> {
                vertGradient(c, p, w, h, 0xFF4FA8D8.toInt(), 0xFFBFE4F2.toInt())
                p.shader = null
                p.color = Color.argb(210, 255, 255, 255)
                for (i in 0 until 9) {
                    val cx = rnd.nextFloat() * w
                    val cy = h * (0.1f + 0.7f * rnd.nextFloat())
                    val s = minOf(w, h) * (0.08f + 0.10f * rnd.nextFloat())
                    for (k in 0 until 5) {
                        c.drawCircle(
                            cx + (k - 2) * s * 0.45f,
                            cy + sin(k.toDouble()).toFloat() * s * 0.12f,
                            s * (0.45f + 0.25f * rnd.nextFloat()), p
                        )
                    }
                }
            }

            BgType.FOREST -> {
                vertGradient(c, p, w, h, 0xFF20364A.toInt(), 0xFF0B1420.toInt())
                p.shader = null
                // 三层树影，越近越深
                for (layer in 0 until 3) {
                    val col = blend(0xFF2E5B47.toInt(), 0xFF0A1118.toInt(), layer / 2.6f)
                    p.color = col
                    val baseY = h * (0.55f + 0.18f * layer)
                    var x = -w * 0.05f
                    while (x < w * 1.05f) {
                        val tw = w * (0.07f + 0.05f * rnd.nextFloat())
                        val th = h * (0.30f + 0.28f * rnd.nextFloat()) * (1f - layer * 0.15f)
                        val path = Path()
                        path.moveTo(x, baseY)
                        path.lineTo(x + tw / 2f, baseY - th)
                        path.lineTo(x + tw, baseY)
                        path.close()
                        c.drawPath(path, p)
                        x += tw * 0.75f
                    }
                    c.drawRect(0f, baseY, w.toFloat(), h.toFloat(), p)
                }
            }

            else -> vertGradient(c, p, w, h, 0xFF111A28.toInt(), 0xFF060A12.toInt())
        }

        vignette(c, w, h)
        return bmp
    }

    /** 四周压暗：把注意力收到画面中间，也让边缘的猎物不至于糊在一起 */
    private fun vignette(c: Canvas, w: Int, h: Int) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val r = maxOf(w, h) * 0.78f
        p.shader = RadialGradient(
            w / 2f, h / 2f, r,
            intArrayOf(Color.TRANSPARENT, Color.TRANSPARENT, Color.argb(115, 0, 0, 0)),
            floatArrayOf(0f, 0.62f, 1f), Shader.TileMode.CLAMP
        )
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), p)
    }

    private fun vertGradient(c: Canvas, p: Paint, w: Int, h: Int, top: Int, bottom: Int) {
        p.shader = LinearGradient(0f, 0f, 0f, h.toFloat(), top, bottom, Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), p)
        p.shader = null
    }

    /** 等比裁剪铺满（centerCrop） */
    fun drawCoverCrop(c: Canvas, bmp: Bitmap, w: Int, h: Int, p: Paint) {
        p.isFilterBitmap = true
        p.isDither = true
        val bw = bmp.width.toFloat()
        val bh = bmp.height.toFloat()
        if (bw <= 0f || bh <= 0f) return
        val scale = maxOf(w / bw, h / bh)
        val dw = bw * scale
        val dh = bh * scale
        val left = (w - dw) / 2f
        val top = (h - dh) / 2f
        c.drawBitmap(bmp, Rect(0, 0, bmp.width, bmp.height), RectF(left, top, left + dw, top + dh), p)
    }

    private fun blend(a: Int, b: Int, t: Float): Int {
        val tt = t.coerceIn(0f, 1f)
        fun mix(x: Int, y: Int) = (x + (y - x) * tt).toInt().coerceIn(0, 255)
        return Color.argb(
            255,
            mix(Color.red(a), Color.red(b)),
            mix(Color.green(a), Color.green(b)),
            mix(Color.blue(a), Color.blue(b))
        )
    }
}
