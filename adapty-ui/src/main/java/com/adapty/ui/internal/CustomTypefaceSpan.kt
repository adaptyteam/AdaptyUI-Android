package com.adapty.ui.internal

import android.graphics.Typeface
import android.text.TextPaint
import android.text.style.MetricAffectingSpan
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class CustomTypefaceSpan(private val typeface: Typeface): MetricAffectingSpan() {
    override fun updateDrawState(tp: TextPaint?) {
        tp?.typeface = typeface
    }

    override fun updateMeasureState(tp: TextPaint) {
        tp.typeface = typeface
    }
}