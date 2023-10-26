package com.adapty.ui.internal

import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal sealed class TextProperties(
    val text: CharSequence?,
    val horizontalGravity: Int,
    val textSize: Float?,
) {

    val textAlignment
        get() = when (horizontalGravity) {
            Gravity.START -> View.TEXT_ALIGNMENT_VIEW_START
            Gravity.CENTER_HORIZONTAL -> View.TEXT_ALIGNMENT_CENTER
            else -> View.TEXT_ALIGNMENT_VIEW_END
        }

    class Builder {
        var isMultiple = false
        var text: CharSequence? = null
        var horizontalGravity = Gravity.START
        var textSize: Float? = null
        var typeface: Typeface? = null
        @ColorInt var textColor: Int? = null

        fun build(): TextProperties {
            return if (isMultiple) {
                Multiple(text, horizontalGravity, textSize)
            } else {
                Single(textColor, typeface, text, horizontalGravity, textSize)
            }
        }
    }

    class Single(
        @ColorInt val textColor: Int?,
        val typeface: Typeface?,
        text: CharSequence?,
        gravity: Int,
        textSize: Float?,
    ): TextProperties(text, gravity, textSize)
    class Multiple(
        text: CharSequence?,
        gravity: Int,
        textSize: Float?,
    ): TextProperties(text, gravity, textSize)
}