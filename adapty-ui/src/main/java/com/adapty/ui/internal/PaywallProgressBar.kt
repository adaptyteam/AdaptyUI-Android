package com.adapty.ui.internal

import android.content.Context
import android.widget.ProgressBar
import androidx.annotation.RestrictTo
import kotlin.math.min

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class PaywallProgressBar(context: Context) : ProgressBar(context) {

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        val padding = ((min(w, h) - LOADING_INDICATOR_WIDTH_DP.dp(context)) / 2).toInt()
        setPadding(padding, padding, padding, padding)
        super.onSizeChanged(w, h, oldw, oldh)
    }
}