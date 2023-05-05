package com.adapty.ui.internal

import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.widget.TextView
import androidx.annotation.RestrictTo
import kotlin.math.ceil

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class TextHelper {

    fun findBestScaledTextSize(text: CharSequence, textView: TextView, layout: Layout): Float {
        val paint = textView.paint
        val lastLine = textView.maxLines.coerceAtMost(textView.lineCount).coerceAtLeast(1) - 1
        val textSize = textView.textSize.toInt()
        val halfTextSize = (ceil(textSize / 2f)).toInt()
        val textSizes = (halfTextSize..textSize).toList()
        val bestTextSizeIndex = textSizes.binarySearch { value ->
            paint.textSize = value.toFloat()
            val lastInLastLine = getLastCharIndex(text, lastLine, textView, layout)

            val nextValue = value + 1
            paint.textSize = nextValue.toFloat()
            val lastInLastLineNext = getLastCharIndex(text, lastLine, textView, layout)

            when {
                lastInLastLine == text.lastIndex && lastInLastLineNext < text.lastIndex -> 0
                lastInLastLine < text.lastIndex -> 1
                else -> -1
            }
        }.coerceAtLeast(0)

        return textSizes[bestTextSizeIndex].toFloat()
    }

    fun isTruncated(text: CharSequence, textView: TextView, layout: Layout): Boolean {
        val line = textView.maxLines.coerceAtMost(textView.lineCount).coerceAtLeast(1) - 1
        return getLastCharIndex(text, line, textView, layout) != text.lastIndex
    }

    private fun getLastCharIndex(
        text: CharSequence,
        line: Int,
        textView: TextView,
        layout: Layout,
    ): Int {
        return configureStaticLayout(text, textView, layout).getLineEnd(line) - 1
    }

    private fun configureStaticLayout(text: CharSequence, textView: TextView, layout: Layout) =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StaticLayout.Builder.obtain(
                text,
                0,
                textView.text.length,
                textView.paint,
                textView.width,
            )
                .setAlignment(layout.alignment)
                .build()
        } else {
            StaticLayout(
                text,
                textView.paint,
                textView.width,
                layout.alignment,
                1.0f,
                0.0f,
                false,
            )
        }
}