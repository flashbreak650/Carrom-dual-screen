package com.carromdual.screen.utils

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator

object UiAnimHelper {

    fun animateStaggeredEntrance(vararg views: View) {
        val interpolator = DecelerateInterpolator(1.6f)
        views.forEachIndexed { index, view ->
            view.alpha = 0f
            view.translationY = 28f
            view.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay((index * 45L).coerceAtMost(360L))
                .setDuration(320L)
                .setInterpolator(interpolator)
                .start()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    fun applyPressScale(view: View) {
        view.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    v.animate().scaleX(0.97f).scaleY(0.97f).setDuration(90L).start()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.animate().scaleX(1f).scaleY(1f).setDuration(120L).start()
                }
            }
            false
        }
    }
}
