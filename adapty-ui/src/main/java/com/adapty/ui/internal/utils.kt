package com.adapty.ui.internal

import android.content.Context
import android.graphics.Paint
import android.graphics.Point
import android.graphics.Rect
import android.os.Build
import android.util.TypedValue
import android.view.View
import android.view.WindowManager
import androidx.annotation.RequiresApi
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.utils.AdaptyLogLevel
import java.util.concurrent.Executors

internal fun Float.dp(context: Context): Float {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        this,
        context.resources.displayMetrics
    )
}

internal fun WindowManager.getScreenSize(): Pair<Int, Int> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        maximumWindowMetrics.bounds.width() to maximumWindowMetrics.bounds.height()
    } else {
        Point().let { outSize ->
            defaultDisplay.getRealSize(outSize)
            outSize.x to outSize.y
        }
    }
}

internal val View.locationOnScreen: IntArray get() = intArrayOf(0,0).also(::getLocationOnScreen)

internal val View.topCoord: Int get() = locationOnScreen[1]

internal val View.bottomCoord: Int get() = topCoord + height

internal fun View.setBackgroundFromAttr(attrRes: Int) {
    val typedValue = resolveAttr(context, attrRes)

    if (typedValue.resourceId != 0) {
        setBackgroundResource(typedValue.resourceId)
    }
}

@RequiresApi(Build.VERSION_CODES.M)
internal fun View.setForegroundFromAttr(attrRes: Int) {
    val typedValue = resolveAttr(context, attrRes)

    if (typedValue.resourceId != 0) {
        foreground = context.getDrawable(typedValue.resourceId)
    }
}

private fun resolveAttr(context: Context, attrRes: Int): TypedValue {
    val typedValue = TypedValue()

    context.theme.resolveAttribute(attrRes, typedValue, true)
    return typedValue
}

internal fun Paint.hasGlyphCompat(string: String): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        return hasGlyph(string)
    }
    val missingSymbol = "\uD809\uDCAB".toCharArray()
    val missingSymbolBounds = Rect()
        .also { bounds -> getTextBounds(missingSymbol, 0, missingSymbol.size, bounds) }

    val testSymbol = string.toCharArray()
    val testSymbolBounds = Rect()
        .also { bounds -> getTextBounds(testSymbol, 0, testSymbol.size, bounds) }
    return testSymbolBounds != missingSymbolBounds
}

private val logExecutor = Executors.newSingleThreadExecutor()

@OptIn(InternalAdaptyApi::class)
internal fun log(messageLogLevel: AdaptyLogLevel, msg: () -> String) {
    logExecutor.execute { com.adapty.internal.utils.log(messageLogLevel, msg) }
}