package com.adapty.ui.internal

import android.content.Context
import android.graphics.Typeface
import androidx.annotation.RestrictTo
import androidx.core.graphics.TypefaceCompat

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal object TypefaceHolder {

    private val typefaceCache = mutableMapOf<String, Typeface>()

    fun getOrPut(context: Context, familyName: String, style: String?): Typeface {
        val fontKey = "$familyName-$style"

        return typefaceCache.getOrPut(fontKey) {
            val fontFamily = Typeface.create(familyName, Typeface.NORMAL)
            val weight = style?.takeIf { it.isNotEmpty() }?.substring(1)?.toIntOrNull() ?: 400

            TypefaceCompat.create(context, fontFamily, weight, false)
        }
    }
}