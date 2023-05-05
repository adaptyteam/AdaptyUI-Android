package com.adapty.ui.internal

import android.graphics.Typeface
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal object TypefaceHolder {

    private val typefaceCache = mutableMapOf<String, Typeface>()

    fun getOrPut(familyName: String, style: String?): Typeface {
        val familyName = when (familyName) {
            "SFPro" -> "sans-serif"
            else -> familyName
        }

        val textStyle: Int
        val fontName: String
        when (style) {
            "bold", "italic" -> {
                fontName = "${familyName}_$style"
                textStyle = if (style == "italic") Typeface.ITALIC else Typeface.BOLD
            }
            else -> {
                textStyle = Typeface.NORMAL
                fontName = when (style) {
                    null, "normal", "regular" -> familyName
                    else -> "${familyName}-$style"
                }
            }
        }
        return typefaceCache.getOrPut(fontName) { Typeface.create(fontName, textStyle) }
    }
}