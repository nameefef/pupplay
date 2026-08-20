package com.easonyin.dogplay

import android.app.Activity
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class GameActivity : AppCompatActivity() {

    private lateinit var prefs: Prefs
    private lateinit var view: GameView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        view = GameView(this, prefs)
        setContentView(view)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // 防误退核心之一：返回键 / 返回手势直接吞掉
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { /* 只能靠长按左上角退出 */ }
        })

        view.onExit = { score, sec ->
            val i = intent
            i.putExtra("score", score)
            i.putExtra("seconds", sec)
            setResult(Activity.RESULT_OK, i)
            finish()
        }

        // 防误退核心之二：把左右边缘划成「手势排除区」，尽量挡掉侧滑返回
        view.post { applyGestureExclusion() }
    }

    private fun applyGestureExclusion() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        val h = view.height
        val w = view.width
        if (h <= 0) return
        val edge = (48 * resources.displayMetrics.density).toInt()
        view.systemGestureExclusionRects = listOf(
            Rect(0, 0, edge, h),
            Rect(w - edge, 0, w, h)
        )
    }

    private fun hideBars() {
        val c = WindowInsetsControllerCompat(window, view)
        c.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        c.hide(WindowInsetsCompat.Type.systemBars())
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideBars()
            applyGestureExclusion()
        }
    }

    override fun onResume() {
        super.onResume()
        hideBars()
        view.resume()
    }

    override fun onPause() {
        super.onPause()
        view.pause()
        // 被系统打断（来电等）也把成绩存下来，不至于白玩
        prefs.lastScore = view.score
        prefs.lastSeconds = view.seconds
        if (view.score > prefs.bestScore) prefs.bestScore = view.score
    }

    override fun onDestroy() {
        super.onDestroy()
        view.release()
    }
}
