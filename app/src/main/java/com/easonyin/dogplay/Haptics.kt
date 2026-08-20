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
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/** 点击震动反馈。抓到 / 落空 用不同的手感。 */
class Haptics(ctx: Context) {

    private val vib: Vibrator? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    } catch (e: Throwable) { null }

    var enabled = true
    /** 1=轻 2=中 3=强 */
    var strength = 2

    val available: Boolean get() = vib?.hasVibrator() == true

    /** 抓到：短促有力的双击手感 */
    fun catchHit() {
        if (!enabled) return
        val amp = when (strength) { 1 -> 90; 2 -> 170; else -> 255 }
        val d1 = when (strength) { 1 -> 12L; 2 -> 20L; else -> 30L }
        val d2 = when (strength) { 1 -> 18L; 2 -> 30L; else -> 45L }
        play(longArrayOf(0, d1, 35, d2), intArrayOf(0, amp, 0, amp), d1 + d2 + 35)
    }

    /** 落空：很轻的一记「嗒」 */
    fun miss() {
        if (!enabled) return
        val amp = when (strength) { 1 -> 45; 2 -> 80; else -> 120 }
        play(longArrayOf(0, 10), intArrayOf(0, amp), 10)
    }

    /** 连击奖励：长一点的三连震 */
    fun reward() {
        if (!enabled) return
        val amp = when (strength) { 1 -> 100; 2 -> 190; else -> 255 }
        play(longArrayOf(0, 25, 45, 25, 45, 60), intArrayOf(0, amp, 0, amp, 0, amp), 200)
    }

    /** UI 里试震动用 */
    fun tick() {
        if (!enabled) return
        val amp = when (strength) { 1 -> 70; 2 -> 130; else -> 200 }
        play(longArrayOf(0, 14), intArrayOf(0, amp), 14)
    }

    private fun play(timings: LongArray, amps: IntArray, fallbackMs: Long) {
        val v = vib ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // 有振幅控制就用波形，没有就退化成简单时长
                if (v.hasAmplitudeControl()) {
                    v.vibrate(VibrationEffect.createWaveform(timings, amps, -1))
                } else {
                    v.vibrate(VibrationEffect.createOneShot(fallbackMs.coerceAtLeast(1), VibrationEffect.DEFAULT_AMPLITUDE))
                }
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(fallbackMs.coerceAtLeast(1))
            }
        } catch (e: Throwable) { /* 不影响游戏 */ }
    }
}
