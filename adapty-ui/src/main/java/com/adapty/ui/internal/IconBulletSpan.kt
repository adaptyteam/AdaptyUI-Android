package com.adapty.ui.internal

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.text.Layout
import android.text.Spanned
import android.text.style.LeadingMarginSpan
import android.text.style.LineHeightSpan
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class IconBulletSpan(
    private val bullet: Drawable,
    private val spacePx: Int,
    private val verticalSpacingPx: Int,
) : LeadingMarginSpan, LineHeightSpan {

    override fun getLeadingMargin(first: Boolean): Int {
        return spacePx
    }

    override fun drawLeadingMargin(c: Canvas, p: Paint, x: Int, dir: Int, top: Int, baseline: Int, bottom: Int, text: CharSequence, start: Int, end: Int, first: Boolean, layout: Layout?) {
        if (first) {
            val layout = layout ?: return

            val verticalSpacingPx = if (start == 0) 0 else verticalSpacingPx

            val widthPx = bullet.bounds.width()
            val heightPx = bullet.bounds.height()
            val bulletStart = layout.getPrimaryHorizontal(start).toInt() - dir * spacePx
            val lineHeight = bottom - top
            val centerY = top + (lineHeight.toFloat() + verticalSpacingPx) / 2
            val halfHeight = heightPx / 2
            val newTop = (centerY - halfHeight).toInt()
            val newBottom = (centerY + halfHeight).toInt()
            val bulletEnd = bulletStart + dir * widthPx
            val bulletLeft: Int
            val bulletRight: Int
            if (bulletEnd < bulletStart) {
                bulletLeft = bulletEnd
                bulletRight = bulletStart
            } else {
                bulletLeft = bulletStart
                bulletRight = bulletEnd
            }
            bullet.setBounds(bulletLeft, newTop, bulletRight, newBottom)
            bullet.draw(c)
        }
    }

    private var prevAscDiff = 0
    private var prevDescDiff = 0

    override fun chooseHeight(text: CharSequence, start: Int, end: Int, spanstartv: Int, lineHeight: Int, fm: Paint.FontMetricsInt) {
        val spanned = text as? Spanned ?: return
        val heightPx = bullet.bounds.height()

        if (start == spanned.getSpanStart(this)) {
            val currentLineHeight = fm.descent - fm.ascent
            if (currentLineHeight < heightPx) {
                val diff = (heightPx - currentLineHeight) / 2
                fm.ascent -= diff
                fm.descent += diff
                prevAscDiff += diff
                prevDescDiff = diff
            }
            if (start != 0) {
                fm.ascent -= verticalSpacingPx
                prevAscDiff += verticalSpacingPx
            }
        } else {
            fm.ascent += prevAscDiff
            fm.descent -= prevDescDiff
            prevAscDiff = 0
            prevDescDiff = 0
        }
    }
}