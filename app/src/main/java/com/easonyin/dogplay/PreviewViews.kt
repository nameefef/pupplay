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
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.view.View

/** 角色选择格子：直接用游戏里的同一套画法渲染预览 */
class PreyTile(
    ctx: Context,
    val type: PreyType,
    private val custom: () -> Bitmap?,
    private val sizeRel: () -> Float = { 1f }
) : View(ctx) {

    private val dp = resources.displayMetrics.density
    private val p = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val t = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val r = RectF()

    var picked = false
        set(v) { field = v; invalidate() }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        r.set(2f * dp, 2f * dp, w - 2f * dp, h - 2f * dp)

        p.style = Paint.Style.FILL
        p.color = if (picked) 0xFF1E2B45.toInt() else 0xFF151C2C.toInt()
        c.drawRoundRect(r, 14f * dp, 14f * dp, p)

        p.style = Paint.Style.STROKE
        p.strokeWidth = if (picked) 2.5f * dp else 1f * dp
        p.color = if (picked) 0xFFFFE066.toInt() else 0x22FFFFFF
        c.drawRoundRect(r, 14f * dp, 14f * dp, p)
        p.style = Paint.Style.FILL

        // 预览跟着「大小」档位一起缩放，滑动时能直接看到效果
        // 预览跟着「大小」档位缩放；倍率压缩一下并封顶，免得画到格子外面
        val vis = sizeRel().coerceIn(0.5f, 1.75f)
        val s = minOf(w, h - 22f * dp) * 0.42f * vis
        c.save()
        c.translate(w / 2f, (h - 20f * dp) / 2f)
        PreyRenderer.draw(c, p, type, s, 1.1f, custom())
        c.restore()

        t.textSize = 11f * dp
        t.color = if (picked) 0xFFFFE066.toInt() else Color.argb(190, 255, 255, 255)
        c.drawText(context.getString(type.labelRes), w / 2f, h - 7f * dp, t)
    }
}

/** 背景选择格子：调用真正的背景渲染器出一张缩略图 */
class BgTile(ctx: Context, val type: BgType, private val custom: () -> Bitmap?) : View(ctx) {

    private val dp = resources.displayMetrics.density
    private val p = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val t = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val r = RectF()
    private var thumb: Bitmap? = null
    private var thumbW = 0
    private var thumbH = 0

    var picked = false
        set(v) { field = v; invalidate() }

    fun invalidateThumb() { thumb?.recycle(); thumb = null; invalidate() }

    override fun onDraw(c: Canvas) {
        val w = width; val h = height
        if (w <= 0 || h <= 0) return
        val bh = (h - 20 * dp).toInt().coerceAtLeast(1)
        if (thumb == null || thumbW != w || thumbH != bh) {
            thumb?.recycle()
            thumb = try { BackgroundRenderer.render(type, w, bh, custom()) } catch (e: Throwable) { null }
            thumbW = w; thumbH = bh
        }

        val fw = w.toFloat(); val fh = h.toFloat()
        r.set(2f * dp, 2f * dp, fw - 2f * dp, fh - 20f * dp)
        c.save()
        path.reset()
        path.addRoundRect(r, 12f * dp, 12f * dp, android.graphics.Path.Direction.CW)
        c.clipPath(path)
        thumb?.let { if (!it.isRecycled) c.drawBitmap(it, 2f * dp, 2f * dp, null) }
        c.restore()

        p.style = Paint.Style.STROKE
        p.strokeWidth = if (picked) 2.5f * dp else 1f * dp
        p.color = if (picked) 0xFFFFE066.toInt() else 0x22FFFFFF
        c.drawRoundRect(r, 12f * dp, 12f * dp, p)
        p.style = Paint.Style.FILL

        t.textSize = 11f * dp
        t.color = if (picked) 0xFFFFE066.toInt() else Color.argb(190, 255, 255, 255)
        c.drawText(context.getString(type.labelRes), fw / 2f, fh - 5f * dp, t)
    }

    companion object { private val path = android.graphics.Path() }
}
