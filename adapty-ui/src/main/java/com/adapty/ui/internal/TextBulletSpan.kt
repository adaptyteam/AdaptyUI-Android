package com.adapty.ui.internal

import android.graphics.Canvas
import android.graphics.Paint
import android.text.Layout
import android.text.Spanned
import android.text.style.CharacterStyle
import android.text.style.LeadingMarginSpan
import android.text.style.LineHeightSpan
import android.text.style.MetricAffectingSpan
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class TextBulletSpan(
    private val bulletText: CharSequence,
    private val spacePx: Int,
    private val verticalSpacingPx: Int,
) : LeadingMarginSpan, LineHeightSpan {

    override fun getLeadingMargin(first: Boolean): Int {
        return spacePx
    }

    override fun drawLeadingMargin(c: Canvas, p: Paint, x: Int, dir: Int, top: Int, baseline: Int, bottom: Int, text: CharSequence, start: Int, end: Int, first: Boolean, layout: Layout?) {
        if (first) {
            if (bulletText.isEmpty()) return
            val layout = layout ?: return
            val paint = layout.paint

            val bulletStart = layout.getPrimaryHorizontal(start).toInt() - dir * spacePx

            val previousTextAlign = paint.textAlign
            paint.textAlign = if (dir < 0) Paint.Align.RIGHT else Paint.Align.LEFT

            if (bulletText !is Spanned) {
                c.drawText(bulletText, 0, bulletText.length, bulletStart.toFloat(), baseline.toFloat(), paint)
            } else {
                val previousTextColor = paint.color
                val previousTypeface = paint.typeface
                val previousTextSize = paint.textSize
                bulletText.getSpans(0, bulletText.length, Any::class.java)?.forEach { span ->
                    when (span) {
                        is MetricAffectingSpan -> span.updateMeasureState(paint)
                        is CharacterStyle -> span.updateDrawState(paint)
                    }
                }
                c.drawText(bulletText, 0, bulletText.length, bulletStart.toFloat(), baseline.toFloat(), paint)
                paint.color = previousTextColor
                paint.typeface = previousTypeface
                paint.textSize = previousTextSize
            }

            paint.textAlign = previousTextAlign
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