package com.adapty.ui.internal

import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewTreeObserver.OnPreDrawListener
import android.widget.TextView
import androidx.annotation.RestrictTo
import com.adapty.utils.AdaptyLogLevel
import kotlin.math.ceil

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class TextHelper(
    private val flowKey: String,
) {

    fun resizeTextOnPreDrawIfNeeded(textView: TextView, retainOriginalHeight: Boolean, onHeightChanged: (() -> Unit)? = null) {
        textView.addOnPreDrawListener(object: OnPreDrawListener {
            private var lastWidth = 0
            private var lastHeight = 0
            private var lastTextLength = 0
            private var retryLastCalculations = false
            private var retryCounter = 0

            override fun onPreDraw(): Boolean {
                val layout = textView.layout ?: return true

                if (lastHeight != textView.height) {
                    lastHeight = textView.height
                    if (retainOriginalHeight)
                        textView.minHeight = lastHeight
                }

                if (lastWidth != textView.width || retryLastCalculations || lastTextLength != textView.text.length) {
                    lastWidth = textView.width
                    lastTextLength = textView.text.length
                    try {
                        if (!isTruncated(textView, layout))
                            return true

                        textView.setTextSize(
                            TypedValue.COMPLEX_UNIT_PX,
                            findBestScaledTextSize(textView, layout),
                        )
                        if (!retainOriginalHeight)
                            onHeightChanged?.invoke()
                        else {
                            textView.setVerticalGravity(Gravity.CENTER_VERTICAL)
                        }

                        retryLastCalculations = false
                        retryCounter = 0
                    } catch (e: Exception) {
                        log(AdaptyLogLevel.WARN) { "$LOG_PREFIX $flowKey couldn't scale text: ${e.localizedMessage ?: e.message}" }
                        if (++retryCounter <= 3) {
                            retryLastCalculations = true
                        } else {
                            retryLastCalculations = false
                            retryCounter = 0
                        }
                        return true
                    }
                    return false
                }
                return true
            }
        })
    }

    private fun findBestScaledTextSize(textView: TextView, layout: Layout): Float {
        val text = textView.text
        val paint = textView.paint
        val lastLine = textView.maxLines.coerceAtMost(textView.lineCount).coerceAtLeast(1) - 1
        val textSize = textView.textSize.toInt()
        val halfTextSize = (ceil(textSize / 2f)).toInt()
        val textSizes = (halfTextSize..textSize).toList()
        val bestTextSizeIndex = textSizes.binarySearch { value ->
            paint.textSize = value.toFloat()
            val lastInLastLine = getLastCharIndex(lastLine, textView, layout)

            val nextValue = value + 1
            paint.textSize = nextValue.toFloat()
            val lastInLastLineNext = getLastCharIndex(lastLine, textView, layout)

            when {
                lastInLastLine == text.lastIndex && lastInLastLineNext < text.lastIndex -> 0
                lastInLastLine < text.lastIndex -> 1
                else -> -1
            }
        }.coerceAtLeast(0)

        return textSizes[bestTextSizeIndex].toFloat()
    }

    private fun isTruncated(textView: TextView, layout: Layout): Boolean {
        val line = textView.maxLines.coerceAtMost(textView.lineCount).coerceAtLeast(1) - 1
        return getLastCharIndex(line, textView, layout) != textView.text.lastIndex
    }

    private fun getLastCharIndex(
        line: Int,
        textView: TextView,
        layout: Layout,
    ): Int {
        return configureStaticLayout(textView, layout).getLineEnd(line) - 1
    }

    private fun configureStaticLayout(textView: TextView, layout: Layout) =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StaticLayout.Builder.obtain(
                textView.text,
                0,
                textView.text.length,
                textView.paint,
                textView.width,
            )
                .setAlignment(layout.alignment)
                .build()
        } else {
            StaticLayout(
                textView.text,
                textView.paint,
                textView.width,
                layout.alignment,
                1.0f,
                0.0f,
                false,
            )
        }
}