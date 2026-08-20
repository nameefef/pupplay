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

/** 运动风格 */
enum class Motion {
    DART,     // 静止—急冲—静止（鼠、猫等陆生猎物，最能勾起狗的捕猎欲）
    HOP,      // 一跳一跳
    SLITHER,  // S 形连续爬行
    BOUNCE,   // 撞墙反弹（球类）
    GLIDE,    // 平滑滑行（光点）
    FLUTTER,  // 忽上忽下地飘（蝶、蜂）
    DRIFT     // 缓慢飘浮（羽毛、泡泡）
}

/** 朝向处理方式 */
enum class Orient {
    SIDE,   // 侧视图：向左走时左右翻转
    TOP,    // 俯视图：整体跟随方向旋转
    NONE    // 不旋转（光点、球）
}

enum class Group(val labelRes: Int) {
    ANIMAL(R.string.group_animal),
    BUG(R.string.group_bug),
    TOY(R.string.group_toy),
    SPECIAL(R.string.group_special)
}

/**
 * 配色说明：狗是二色视觉（蓝—黄轴），红/绿在它眼里都偏暗黄褐。
 * 所以主色尽量取 亮黄 / 亮蓝 / 白，深色背景上对比最强。
 * 红点是用户点名要的，保留原色（狗看到的是暗色小点，但动得快照样追）。
 */
enum class PreyType(
    val key: String,
    val labelRes: Int,
    val group: Group,
    val c1: Int,            // 主体色
    val c2: Int,            // 辅助色
    val c3: Int,            // 细节色
    val sizeDp: Float,
    val speedDp: Float,     // 巡航速度 dp/秒
    val dart: Float,        // 冲刺倍率
    val pauseMin: Float, val pauseMax: Float,
    val runMin: Float, val runMax: Float,
    val motion: Motion,
    val orient: Orient,
    val sound: SoundId
) {
    // ── 小动物 ────────────────────────────────────────────────
    MOUSE("mouse", R.string.prey_mouse, Group.ANIMAL, 0xFFD8DEE9.toInt(), 0xFFF2B8C6.toInt(), 0xFF2B3242.toInt(),
        34f, 330f, 2.6f, 0.20f, 0.85f, 0.30f, 0.85f, Motion.DART, Orient.SIDE, SoundId.SQUEAK),
    FOX("fox", R.string.prey_fox, Group.ANIMAL, 0xFFFFB43F.toInt(), 0xFFFFF3DC.toInt(), 0xFF3A2A18.toInt(),
        58f, 300f, 2.0f, 0.45f, 1.30f, 0.70f, 1.50f, Motion.DART, Orient.SIDE, SoundId.YIP),
    RABBIT("rabbit", R.string.prey_rabbit, Group.ANIMAL, 0xFFFFF3D6.toInt(), 0xFFF5B9C8.toInt(), 0xFF3A3427.toInt(),
        46f, 340f, 2.3f, 0.35f, 1.10f, 0.45f, 1.00f, Motion.HOP, Orient.SIDE, SoundId.THUMP),
    SQUIRREL("squirrel", R.string.prey_squirrel, Group.ANIMAL, 0xFFE0A95C.toInt(), 0xFFFFF0D4.toInt(), 0xFF3A2A18.toInt(),
        44f, 350f, 2.5f, 0.25f, 0.90f, 0.35f, 0.85f, Motion.HOP, Orient.SIDE, SoundId.CHITTER),
    CAT("cat", R.string.prey_cat, Group.ANIMAL, 0xFFB9C6DA.toInt(), 0xFFFFF3DC.toInt(), 0xFF2B3242.toInt(),
        56f, 320f, 2.2f, 0.50f, 1.40f, 0.60f, 1.30f, Motion.DART, Orient.SIDE, SoundId.MEOW),
    HEDGEHOG("hedgehog", R.string.prey_hedgehog, Group.ANIMAL, 0xFFC9B27A.toInt(), 0xFFFFE9BE.toInt(), 0xFF33291A.toInt(),
        40f, 210f, 1.8f, 0.55f, 1.50f, 0.50f, 1.10f, Motion.DART, Orient.SIDE, SoundId.SNIFF),
    RACCOON("raccoon", R.string.prey_raccoon, Group.ANIMAL, 0xFFAFB9C9.toInt(), 0xFFFFF3DC.toInt(), 0xFF232937.toInt(),
        54f, 290f, 2.0f, 0.45f, 1.25f, 0.60f, 1.25f, Motion.DART, Orient.SIDE, SoundId.CHITTER),
    DUCK("duck", R.string.prey_duck, Group.ANIMAL, 0xFFFFF0C4.toInt(), 0xFFFFA828.toInt(), 0xFF2B3242.toInt(),
        46f, 260f, 1.9f, 0.40f, 1.20f, 0.50f, 1.10f, Motion.DART, Orient.SIDE, SoundId.QUACK),
    CHICK("chick", R.string.prey_chick, Group.ANIMAL, 0xFFFFE45C.toInt(), 0xFFFFA828.toInt(), 0xFF2B3242.toInt(),
        32f, 320f, 2.4f, 0.20f, 0.70f, 0.30f, 0.70f, Motion.HOP, Orient.SIDE, SoundId.CHEEP),
    SHEEP("sheep", R.string.prey_sheep, Group.ANIMAL, 0xFFFFF7E8.toInt(), 0xFF9AA7BC.toInt(), 0xFF2B3242.toInt(),
        52f, 240f, 1.8f, 0.55f, 1.40f, 0.55f, 1.20f, Motion.DART, Orient.SIDE, SoundId.BAA),
    PIG("pig", R.string.prey_pig, Group.ANIMAL, 0xFFF7C3CF.toInt(), 0xFFFFE0E7.toInt(), 0xFF3A2A2E.toInt(),
        50f, 250f, 1.9f, 0.50f, 1.30f, 0.55f, 1.20f, Motion.DART, Orient.SIDE, SoundId.OINK),
    FROG("frog", R.string.prey_frog, Group.ANIMAL, 0xFFC8DE5A.toInt(), 0xFFEFF7C8.toInt(), 0xFF2B3242.toInt(),
        40f, 300f, 2.6f, 0.45f, 1.30f, 0.35f, 0.75f, Motion.HOP, Orient.SIDE, SoundId.CROAK),
    SNAKE("snake", R.string.prey_snake, Group.ANIMAL, 0xFFD6E05A.toInt(), 0xFF8FA33A.toInt(), 0xFF2B3242.toInt(),
        60f, 260f, 1.9f, 0.30f, 0.90f, 0.90f, 1.80f, Motion.SLITHER, Orient.TOP, SoundId.HISS),
    CRAB("crab", R.string.prey_crab, Group.ANIMAL, 0xFFFF9A5B.toInt(), 0xFFFFD9B0.toInt(), 0xFF2B3242.toInt(),
        44f, 300f, 2.2f, 0.30f, 0.90f, 0.40f, 0.90f, Motion.DART, Orient.TOP, SoundId.CLICK),
    FISH("fish", R.string.prey_fish, Group.ANIMAL, 0xFF6FD4FF.toInt(), 0xFFDDF4FF.toInt(), 0xFF20313F.toInt(),
        42f, 320f, 2.1f, 0.15f, 0.55f, 0.60f, 1.40f, Motion.GLIDE, Orient.TOP, SoundId.SPLASH),

    // ── 虫 · 鸟 ──────────────────────────────────────────────
    BEETLE("beetle", R.string.prey_beetle, Group.BUG, 0xFFE9DC4E.toInt(), 0xFF8C8420.toInt(), 0xFF23281A.toInt(),
        24f, 380f, 2.8f, 0.08f, 0.45f, 0.25f, 0.65f, Motion.DART, Orient.TOP, SoundId.SKITTER),
    BUTTERFLY("butterfly", R.string.prey_butterfly, Group.BUG, 0xFFFFE066.toInt(), 0xFF6FC2FF.toInt(), 0xFF2B3242.toInt(),
        42f, 200f, 1.5f, 0.00f, 0.20f, 1.00f, 2.20f, Motion.FLUTTER, Orient.TOP, SoundId.FLUTTER),
    BEE("bee", R.string.prey_bee, Group.BUG, 0xFFFFD23F.toInt(), 0xFF2B3242.toInt(), 0xFFE8F4FF.toInt(),
        30f, 300f, 1.9f, 0.00f, 0.15f, 0.60f, 1.40f, Motion.FLUTTER, Orient.TOP, SoundId.BUZZ),
    SPIDER("spider", R.string.prey_spider, Group.BUG, 0xFF9FB0C8.toInt(), 0xFF2B3242.toInt(), 0xFFE8F4FF.toInt(),
        34f, 400f, 3.0f, 0.15f, 0.80f, 0.20f, 0.55f, Motion.DART, Orient.TOP, SoundId.SKITTER),
    DRAGONFLY("dragonfly", R.string.prey_dragonfly, Group.BUG, 0xFF7FE3FF.toInt(), 0xFFE8F9FF.toInt(), 0xFF2B3242.toInt(),
        46f, 380f, 2.2f, 0.05f, 0.40f, 0.40f, 1.10f, Motion.FLUTTER, Orient.TOP, SoundId.WHIRR),
    BIRD("bird", R.string.prey_bird, Group.BUG, 0xFF8FD0FF.toInt(), 0xFFFFE066.toInt(), 0xFF2B3242.toInt(),
        44f, 340f, 2.2f, 0.20f, 0.90f, 0.60f, 1.40f, Motion.HOP, Orient.SIDE, SoundId.TWEET),
    FIREFLY("firefly", R.string.prey_firefly, Group.BUG, 0xFFE9FF7A.toInt(), 0xFFFFF7B0.toInt(), 0xFF2B3242.toInt(),
        22f, 180f, 1.6f, 0.10f, 0.60f, 0.50f, 1.30f, Motion.DRIFT, Orient.NONE, SoundId.SPARKLE),

    // ── 玩具 · 光点 ──────────────────────────────────────────
    RED_DOT("red_dot", R.string.prey_red_dot, Group.TOY, 0xFFFF3B30.toInt(), 0xFFFF9A90.toInt(), 0xFFFFFFFF.toInt(),
        20f, 430f, 1.7f, 0.10f, 0.45f, 0.55f, 1.40f, Motion.GLIDE, Orient.NONE, SoundId.BLIP),
    LASER("laser", R.string.prey_laser, Group.TOY, 0xFF7CFF6B.toInt(), 0xFFD6FFCF.toInt(), 0xFFFFFFFF.toInt(),
        20f, 450f, 1.7f, 0.08f, 0.40f, 0.55f, 1.40f, Motion.GLIDE, Orient.NONE, SoundId.BLIP),
    BLUE_DOT("blue_dot", R.string.prey_blue_dot, Group.TOY, 0xFF6FD4FF.toInt(), 0xFFDDF4FF.toInt(), 0xFFFFFFFF.toInt(),
        22f, 420f, 1.7f, 0.08f, 0.40f, 0.55f, 1.40f, Motion.GLIDE, Orient.NONE, SoundId.BLIP),
    TENNIS("tennis", R.string.prey_tennis, Group.TOY, 0xFFE4E84A.toInt(), 0xFFFFFFFF.toInt(), 0xFF6E7420.toInt(),
        46f, 360f, 1.0f, 0f, 0f, 99f, 99f, Motion.BOUNCE, Orient.NONE, SoundId.BOING),
    FRISBEE("frisbee", R.string.prey_frisbee, Group.TOY, 0xFF6FC2FF.toInt(), 0xFFE8F4FF.toInt(), 0xFF1F3A4D.toInt(),
        56f, 400f, 1.0f, 0f, 0f, 99f, 99f, Motion.BOUNCE, Orient.NONE, SoundId.WHOOSH),
    BONE("bone", R.string.prey_bone, Group.TOY, 0xFFFFF3DC.toInt(), 0xFFE0D2B4.toInt(), 0xFF3A3427.toInt(),
        50f, 280f, 1.0f, 0f, 0f, 99f, 99f, Motion.BOUNCE, Orient.NONE, SoundId.CRUNCH),
    ROPE("rope", R.string.prey_rope, Group.TOY, 0xFFFFD9A0.toInt(), 0xFF6FC2FF.toInt(), 0xFF3A3427.toInt(),
        52f, 300f, 2.0f, 0.25f, 0.80f, 0.45f, 1.00f, Motion.DART, Orient.SIDE, SoundId.SQUEAKTOY),
    BUBBLE("bubble", R.string.prey_bubble, Group.TOY, 0xFFCDEBFF.toInt(), 0xFFFFFFFF.toInt(), 0xFF9FD8FF.toInt(),
        44f, 140f, 1.4f, 0.10f, 0.50f, 1.00f, 2.40f, Motion.DRIFT, Orient.NONE, SoundId.POP),
    STAR("star", R.string.prey_star, Group.TOY, 0xFFFFE066.toInt(), 0xFFFFF7C8.toInt(), 0xFF6E5A16.toInt(),
        40f, 330f, 1.9f, 0.15f, 0.60f, 0.50f, 1.20f, Motion.GLIDE, Orient.NONE, SoundId.SPARKLE),
    FEATHER("feather", R.string.prey_feather, Group.TOY, 0xFFFFF3DC.toInt(), 0xFF9FD8FF.toInt(), 0xFF3A3427.toInt(),
        46f, 160f, 1.5f, 0.05f, 0.40f, 1.00f, 2.20f, Motion.DRIFT, Orient.TOP, SoundId.FLUTTER),

    // ── 特别 ────────────────────────────────────────────────
    CUSTOM("custom", R.string.prey_custom, Group.SPECIAL, 0xFFFFE066.toInt(), 0xFF6FC2FF.toInt(), 0xFF2B3242.toInt(),
        56f, 320f, 2.2f, 0.30f, 1.00f, 0.45f, 1.10f, Motion.DART, Orient.SIDE, SoundId.SQUEAK),
    MIXED("mixed", R.string.prey_mixed, Group.SPECIAL, 0xFFFFE066.toInt(), 0xFF6FC2FF.toInt(), 0xFF2B3242.toInt(),
        46f, 320f, 2.2f, 0.30f, 1.00f, 0.45f, 1.10f, Motion.DART, Orient.SIDE, SoundId.SQUEAK);

    companion object {
        /** 可被「大乱斗」随机抽到的真实角色（排除 CUSTOM / MIXED） */
        val playable: List<PreyType> by lazy {
            entries.filter { it != CUSTOM && it != MIXED }
        }

        fun fromKey(key: String?): PreyType =
            entries.firstOrNull { it.key == key } ?: MOUSE
    }
}
