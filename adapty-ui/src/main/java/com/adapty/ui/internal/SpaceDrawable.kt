package com.adapty.ui.internal

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class SpaceDrawable(spaceWidthPx: Int): Drawable() {

    init {
        setBounds(0, 0, spaceWidthPx, 1)
    }

    override fun draw(canvas: Canvas)  = Unit

    override fun setAlpha(alpha: Int)  = Unit

    override fun setColorFilter(colorFilter: ColorFilter?) = Unit

    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}