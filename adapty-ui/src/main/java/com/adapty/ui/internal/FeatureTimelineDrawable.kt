package com.adapty.ui.internal

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.Drawable
import androidx.annotation.RestrictTo
import com.adapty.ui.AdaptyUI

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class FeatureTimelineDrawable(
    private val background: Drawable,
    private val icon: Drawable,
    private val backgroundDrawableSizePx: Float,
    private val lineWidthPx: Float,
    private val lineColorOrGradient: AdaptyUI.ViewConfiguration.Asset,
    private val shaderHelper: ShaderHelper,
) : Drawable() {

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        if (lineColorOrGradient is AdaptyUI.ViewConfiguration.Asset.Color) {
            color = lineColorOrGradient.value
        }
    }

    private val lineHalfWidthPx = lineWidthPx / 2
    private val backgroundDrawableHalfSizePx = backgroundDrawableSizePx / 2

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        linePaint.shader = null

        val bgHalfSize = backgroundDrawableHalfSizePx.toInt()
        val maxIconSize = (backgroundDrawableSizePx * TIMELINE_DRAWABLE_ICON_RELATIVE_COEF).toInt()

        val iconHalfWidth = icon.intrinsicWidth.coerceAtMost(maxIconSize) / 2
        val iconHalfHeight = icon.intrinsicHeight.coerceAtMost(maxIconSize) / 2
        icon.setBounds(
            bounds.centerX() - iconHalfWidth, bounds.top + bgHalfSize - iconHalfHeight,
            bounds.centerX() + iconHalfWidth, bounds.top + bgHalfSize + iconHalfHeight,
        )

        background.setBounds(
            bounds.centerX() - bgHalfSize, bounds.top,
            bounds.centerX() + bgHalfSize, bounds.top + bgHalfSize + bgHalfSize,
        )
    }

    override fun draw(canvas: Canvas) {
        if (linePaint.shader == null && lineColorOrGradient is AdaptyUI.ViewConfiguration.Asset.Gradient) {
            val gradient = shaderHelper.createShader(bounds, lineColorOrGradient)

            linePaint.shader = gradient
        }

        background.draw(canvas)

        canvas.drawRect(
            bounds.centerX() - lineHalfWidthPx,
            bounds.top + backgroundDrawableSizePx,
            bounds.centerX() + lineHalfWidthPx,
            bounds.bottom.toFloat(),
            linePaint
        )
        icon.draw(canvas)
    }

    override fun setAlpha(alpha: Int) {
        linePaint.alpha = alpha
    }

    override fun getAlpha(): Int =
        linePaint.alpha

    override fun setColorFilter(colorFilter: ColorFilter?) {
        linePaint.colorFilter = colorFilter
    }

    override fun getColorFilter(): ColorFilter? =
        linePaint.colorFilter

    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    override fun getConstantState(): ConstantState = object : ConstantState() {
        override fun newDrawable(): Drawable =
            FeatureTimelineDrawable(
                background.constantState!!.newDrawable(),
                icon.constantState!!.newDrawable(),
                backgroundDrawableSizePx,
                lineWidthPx,
                lineColorOrGradient,
                shaderHelper,
            )

        override fun getChangingConfigurations(): Int =
            icon.changingConfigurations or background.changingConfigurations
    }
}