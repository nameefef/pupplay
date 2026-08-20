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

import android.content.res.Resources
import kotlin.math.abs
import kotlin.math.min

/** 屏幕的物理尺寸。猎物大小完全由这里推导，所以单独抽出来，菜单和游戏用同一套数。 */
object Screen {

    /**
     * 每毫米多少像素。
     *
     * 优先用 xdpi/ydpi（真实物理密度），但有些机型只有一个轴报真实值、
     * 另一个轴报分桶后的 densityDpi（比如 xdpi=160、ydpi=440）。
     * 这种情况下取平均会得到一个两边都不对的值，不如整体退回 densityDpi。
     */
    fun mmPx(res: Resources): Float {
        val m = res.displayMetrics
        val x = m.xdpi
        val y = m.ydpi
        val sane = x in 100f..900f && y in 100f..900f
        val agree = sane && abs(x - y) <= 0.25f * maxOf(x, y)
        val dpi = if (agree) (x + y) / 2f else m.densityDpi.toFloat()
        return dpi / 25.4f
    }

    /** 屏幕短边的物理长度（毫米）。横屏时这就是场地的高度。 */
    fun shortEdgeMm(res: Resources): Float {
        val m = res.displayMetrics
        val shortPx = min(m.widthPixels, m.heightPixels).toFloat()
        return shortPx / mmPx(res)
    }
}
