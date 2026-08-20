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

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

/** 一只猎物：自带运动状态机 + 逃跑反应 */
class Prey(
    var type: PreyType,
    private val dp: Float,
    private val rnd: Random,
    private val sizeMul: Float = 1f,
    /** 外部算好的尺寸上限（像素）。见 GameView.sizeCap */
    private val maxSize: Float = Float.MAX_VALUE
) {
    var x = 0f; var y = 0f
    var vx = 0f; var vy = 0f
    var size = type.sizeDp * dp * sizeMul

    private var state = 0            // 0=停顿 1=奔跑 2=逃窜
    private var timer = 0f
    private var tx = 0f; private var ty = 0f
    private var phase = rnd.nextFloat() * 10f
    private var tilt = 0f
    private var facingLeft = false

    var alive = true
    private var respawn = 0f
    private var scale = 0f           // 出场/被抓时的缩放动画

    /** 「大乱斗」模式下，每次重生随机换一个角色 */
    var respawnType: (() -> PreyType)? = null

    /**
     * 实际尺寸：按倍率放大后再封顶。
     * 不封顶的话大倍率下留白会超过屏幕宽度，坐标算出来是负的，猎物会卡在角落。
     */
    private fun effectiveSize(w: Float, h: Float): Float =
        (type.sizeDp * dp * sizeMul).coerceAtMost(minOf(maxSize, minOf(w, h) * 0.30f))

    /** 生成新一轮的位置 */
    fun spawn(w: Float, h: Float) {
        respawnType?.invoke()?.let { type = it }
        size = effectiveSize(w, h)
        val m = size * 0.6f
        x = m + rnd.nextFloat() * (w - 2 * m)
        y = m + rnd.nextFloat() * (h - 2 * m)
        val a = rnd.nextFloat() * 6.2832f
        val base = type.speedDp * dp
        vx = cos(a.toDouble()).toFloat() * base
        vy = sin(a.toDouble()).toFloat() * base
        alive = true
        scale = 0f
        state = 0
        timer = rnd.nextFloat() * 0.5f
        pickTarget(w, h)
    }

    /** 被抓到 */
    fun caught() {
        alive = false
        respawn = 0.45f + rnd.nextFloat() * 0.65f
    }

    fun hitRadius(): Float = size * 0.72f + 16f * dp   // 放宽一点，狗用爪子拍不准

    fun hit(px: Float, py: Float): Boolean =
        alive && scale > 0.35f && hypot(px - x, py - y) <= hitRadius()

    /** 附近落空 → 受惊逃窜，这一下最能激起继续追的兴趣 */
    fun startle(px: Float, py: Float, w: Float, h: Float) {
        if (!alive) return
        val d = hypot(px - x, py - y)
        if (d > hitRadius() * 3.4f) return
        val a = atan2(y - py, x - px)
        val spd = type.speedDp * dp * type.dart * 1.35f
        vx = cos(a.toDouble()).toFloat() * spd
        vy = sin(a.toDouble()).toFloat() * spd
        state = 2
        timer = 0.35f + rnd.nextFloat() * 0.45f
        tx = (x + vx * 0.9f).coerceIn(size, w - size)
        ty = (y + vy * 0.9f).coerceIn(size, h - size)
    }

    private fun pickTarget(w: Float, h: Float) {
        val m = size * 0.7f
        // 老鼠、甲虫这类爱贴边跑；其他的满屏跑
        val edgeLover = type == PreyType.MOUSE || type == PreyType.BEETLE ||
                type == PreyType.SPIDER || type == PreyType.CRAB
        if (edgeLover && rnd.nextFloat() < 0.55f) {
            when (rnd.nextInt(4)) {
                0 -> { tx = m + rnd.nextFloat() * (w - 2 * m); ty = m }
                1 -> { tx = m + rnd.nextFloat() * (w - 2 * m); ty = h - m }
                2 -> { tx = m; ty = m + rnd.nextFloat() * (h - 2 * m) }
                else -> { tx = w - m; ty = m + rnd.nextFloat() * (h - 2 * m) }
            }
        } else {
            tx = m + rnd.nextFloat() * (w - 2 * m)
            ty = m + rnd.nextFloat() * (h - 2 * m)
        }
        // 目标别离得太近，不然看着像在原地抖
        if (hypot(tx - x, ty - y) < size * 3f) {
            tx = m + rnd.nextFloat() * (w - 2 * m)
            ty = m + rnd.nextFloat() * (h - 2 * m)
        }
    }

    fun update(dt: Float, w: Float, h: Float, speedMul: Float) {
        if (!alive) {
            respawn -= dt
            scale = (scale - dt * 4f).coerceAtLeast(0f)
            if (respawn <= 0f) spawn(w, h)
            return
        }
        if (scale < 1f) scale = (scale + dt * 4.5f).coerceAtMost(1f)

        val base = type.speedDp * dp * speedMul
        val m = size * 0.6f

        when (type.motion) {
            Motion.BOUNCE -> {
                // 匀速直线 + 撞墙反弹
                val cur = hypot(vx, vy)
                if (cur < 1f) { vx = base; vy = base * 0.6f }
                else if (abs(cur - base) > base * 0.05f) {
                    val k = base / cur; vx *= k; vy *= k
                }
                x += vx * dt; y += vy * dt
                if (x < m) { x = m; vx = abs(vx) }
                if (x > w - m) { x = w - m; vx = -abs(vx) }
                if (y < m) { y = m; vy = abs(vy) }
                if (y > h - m) { y = h - m; vy = -abs(vy) }
            }

            else -> {
                timer -= dt
                when (state) {
                    0 -> { // 停顿：几乎不动，只有轻微抖动，等一下就突然窜出去
                        vx *= exp(-7f * dt); vy *= exp(-7f * dt)
                        if (type.motion == Motion.FLUTTER || type.motion == Motion.DRIFT) {
                            vx += sin((phase * 2.1f).toDouble()).toFloat() * base * 0.6f * dt
                            vy += cos((phase * 1.7f).toDouble()).toFloat() * base * 0.6f * dt
                        }
                        if (timer <= 0f) {
                            state = 1
                            timer = type.runMin + rnd.nextFloat() * (type.runMax - type.runMin)
                            pickTarget(w, h)
                        }
                    }
                    1, 2 -> { // 奔跑 / 逃窜
                        val burst = if (state == 2) type.dart * 1.3f else type.dart
                        val spd = base * when (type.motion) {
                            Motion.GLIDE -> 1.0f
                            Motion.DRIFT -> 0.5f
                            Motion.FLUTTER -> 0.8f
                            else -> burst
                        }
                        val dx = tx - x
                        val dy = ty - y
                        val d = hypot(dx, dy).coerceAtLeast(0.001f)
                        var wantX = dx / d * spd
                        var wantY = dy / d * spd

                        if (type.motion == Motion.FLUTTER) {
                            // 垂直方向叠一个正弦，飞得忽上忽下
                            val s = sin((phase * 3.2f).toDouble()).toFloat() * spd * 0.75f
                            wantX += -dy / d * s
                            wantY += dx / d * s
                        } else if (type.motion == Motion.SLITHER) {
                            val s = sin((phase * 4.5f).toDouble()).toFloat() * spd * 0.45f
                            wantX += -dy / d * s
                            wantY += dx / d * s
                        } else if (type.motion == Motion.DRIFT) {
                            wantY -= base * 0.25f    // 轻轻上浮
                            wantX += sin((phase * 1.3f).toDouble()).toFloat() * base * 0.35f
                        }

                        // 转向平滑度：陆生急停急转，飞行/光点更顺滑
                        val k = when (type.motion) {
                            Motion.DART, Motion.HOP -> 1f - exp(-16f * dt)
                            Motion.GLIDE -> 1f - exp(-4.5f * dt)
                            else -> 1f - exp(-6f * dt)
                        }
                        vx += (wantX - vx) * k
                        vy += (wantY - vy) * k
                        x += vx * dt; y += vy * dt

                        val arrived = d < size * 0.8f
                        if (timer <= 0f || arrived) {
                            if (type.motion == Motion.SLITHER || type.motion == Motion.GLIDE ||
                                type.motion == Motion.FLUTTER || type.motion == Motion.DRIFT) {
                                // 这几类不爱停，直接换个目标继续走
                                if (rnd.nextFloat() < 0.30f) {
                                    state = 0
                                    timer = type.pauseMin + rnd.nextFloat() * (type.pauseMax - type.pauseMin)
                                } else {
                                    timer = type.runMin + rnd.nextFloat() * (type.runMax - type.runMin)
                                    pickTarget(w, h)
                                }
                            } else {
                                state = 0
                                timer = type.pauseMin + rnd.nextFloat() * (type.pauseMax - type.pauseMin)
                            }
                        }
                        // 撞到边界就贴边并重选目标
                        if (x < m) { x = m; pickTarget(w, h) }
                        if (x > w - m) { x = w - m; pickTarget(w, h) }
                        if (y < m) { y = m; pickTarget(w, h) }
                        if (y > h - m) { y = h - m; pickTarget(w, h) }
                    }
                }
            }
        }

        // 动画相位跟着实际速度走，走路循环才不会打滑
        val v = hypot(vx, vy)
        phase += dt * when (type.motion) {
            Motion.FLUTTER -> 16f
            Motion.BOUNCE -> 2f + v / (size + 1f)
            Motion.DRIFT -> 3f
            else -> 2.5f + v / (size * 0.55f + 1f) * 1.6f
        }
        if (phase > 1e6f) phase = 0f

        if (v > base * 0.12f) facingLeft = vx < 0f
        val targetTilt = (vy / (v + 1f) * 22f).coerceIn(-24f, 24f)
        tilt += (targetTilt - tilt) * (1f - exp(-8f * dt))
    }

    /** 跳跃类猎物的离地高度 */
    private fun hopOffset(): Float =
        if (type.motion == Motion.HOP && state != 0)
            abs(sin(phase.toDouble())).toFloat() * size * 0.30f
        else 0f

    fun draw(c: Canvas, p: Paint, custom: Bitmap?, shadows: Boolean) {
        if (scale <= 0.01f) return
        val hop = hopOffset()

        if (shadows) {   // 地面投影，跳起来时影子变小，立体感来源
            p.shader = null
            p.style = Paint.Style.FILL
            val k = 1f - hop / (size * 0.6f)
            p.color = Color.argb((70 * k * scale).toInt().coerceIn(0, 255), 0, 0, 0)
            c.drawOval(
                x - size * 0.45f * scale, y + size * 0.30f - size * 0.09f,
                x + size * 0.45f * scale, y + size * 0.30f + size * 0.09f, p
            )
        }

        c.save()
        c.translate(x, y - hop)
        c.scale(scale, scale)
        when (type.orient) {
            Orient.SIDE -> {
                if (facingLeft) c.scale(-1f, 1f)
                c.rotate(tilt * 0.5f)
            }
            Orient.TOP -> {
                val a = Math.toDegrees(atan2(vy.toDouble(), vx.toDouble())).toFloat()
                if (hypot(vx, vy) > 1f) c.rotate(a)
            }
            Orient.NONE -> {}
        }
        PreyRenderer.draw(c, p, type, size, phase, custom)
        c.restore()
    }
}
