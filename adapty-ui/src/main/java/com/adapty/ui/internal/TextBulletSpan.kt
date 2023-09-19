package com.adapty.ui.internal

import android.graphics.Canvas
import android.graphics.Paint
import android.text.Layout
import android.text.Spanned
import android.text.style.LeadingMarginSpan
import android.text.style.LineHeightSpan
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class TextBulletSpan(
    private val bullet: String,
    private val spacePx: Int,
    private val verticalSpacingPx: Int,
) : LeadingMarginSpan, LineHeightSpan {

    override fun getLeadingMargin(first: Boolean): Int {
        return spacePx
    }

    override fun drawLeadingMargin(c: Canvas, p: Paint, x: Int, dir: Int, top: Int, baseline: Int, bottom: Int, text: CharSequence, start: Int, end: Int, first: Boolean, layout: Layout?) {
        if (first) {
            val layout = layout ?: return
            val bulletStart = x + layout.getPrimaryHorizontal(start).toInt() - spacePx
            c.drawText(bullet, bulletStart.toFloat(), baseline.toFloat(), layout.paint)
        }
    }

    private var prevAscDiff = 0

    override fun chooseHeight(text: CharSequence, start: Int, end: Int, spanstartv: Int, lineHeight: Int, fm: Paint.FontMetricsInt) {
        val spanned = text as? Spanned ?: return
        if (start == spanned.getSpanStart(this)) {
            if (start != 0) {
                fm.ascent -= verticalSpacingPx
                prevAscDiff += verticalSpacingPx
            }
        } else {
            fm.ascent += prevAscDiff
            prevAscDiff = 0
        }
    }
}