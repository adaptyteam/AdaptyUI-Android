package com.adapty.ui.internal

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable
import androidx.annotation.RestrictTo
import com.adapty.ui.AdaptyUI
import java.lang.Float.min
import kotlin.math.acos

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class ShapeDrawable(
    private val fillShape: Shape.Fill?,
    private val strokeShape: Shape.Stroke?,
    private val shaderHelper: ShaderHelper,
) : Drawable() {

    private val fillPath = Path()
    private val strokePath = Path()
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        (fillShape?.asset as? AdaptyUI.ViewConfiguration.Asset.Color)?.value?.let(::setColor)
    }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        .apply {
            if (strokeShape != null) {
                style = Paint.Style.STROKE
                strokeWidth = strokeShape.strokeThicknessPx
                (strokeShape.asset as? AdaptyUI.ViewConfiguration.Asset.Color)?.value?.let(::setColor)
            }
        }

    override fun onBoundsChange(bounds: Rect?) {
        super.onBoundsChange(bounds)
        fillPath.rewind()
        fillPaint.shader = null
        strokePath.rewind()
        strokePaint.shader = null
    }

    override fun draw(canvas: Canvas) {
        if (fillShape != null) {
            if (fillPath.isEmpty) {
                fillPath(fillPath, bounds, fillShape)
            }
            if (fillPaint.shader == null) {
                setupShader(fillPaint, fillShape.asset)
            }
            canvas.drawPath(fillPath, fillPaint)
        }

        if (strokeShape != null) {
            if (strokePath.isEmpty) {
                val halfStrokeWidth = (strokeShape.strokeThicknessPx / 2).toInt()
                val bounds = Rect(bounds.left + halfStrokeWidth, bounds.top + halfStrokeWidth, bounds.right - halfStrokeWidth, bounds.bottom - halfStrokeWidth)
                fillPath(strokePath, bounds, strokeShape)
            }
            if (strokePaint.shader == null) {
                setupShader(strokePaint, strokeShape.asset)
            }
            canvas.drawPath(strokePath, strokePaint)
        }
    }

    private fun setupShader(paint: Paint, asset: AdaptyUI.ViewConfiguration.Asset.Filling.Local) {
        when (asset) {
            is AdaptyUI.ViewConfiguration.Asset.Gradient -> {
                paint.shader = shaderHelper.createShader(bounds, asset)
            }
            is AdaptyUI.ViewConfiguration.Asset.Image -> {
                shaderHelper.createShader(bounds, asset)?.let(paint::setShader)
            }
            else -> Unit
        }
    }

    private fun fillPath(path: Path, bounds: Rect, shape: Shape) {
        when (val type = shape.type) {
            is Shape.Type.Rect -> {
                fillPathAsRect(path, bounds, type)
            }

            is Shape.Type.Circle -> {
                fillPathAsCircle(path, bounds)
            }

            is Shape.Type.RectWithArc -> {
                fillPathAsRectWithArc(path, bounds, type)
            }
        }
    }

    private fun fillPathAsRectWithArc(path: Path, bounds: Rect, rectWithArc: Shape.Type.RectWithArc) {
        val arcHeight = rectWithArc.arcHeightPx
        val rect = RectF()
        path.moveTo(bounds.left.toFloat(), bounds.bottom.toFloat())
        val r = (bounds.width().let { it * it } + 4 * arcHeight * arcHeight) / (8f * arcHeight)
        val d = r - arcHeight
        val theta = Math.toDegrees((2 * acos(d / r)).toDouble()).toFloat()
        rect.set(
            bounds.exactCenterX() - r,
            bounds.top.toFloat(),
            bounds.exactCenterX() + r,
            bounds.top.toFloat() + r + r,
        )
        if (arcHeight < 0f) {
            rect.sort()
            rect.offset(0f, -arcHeight)
            path.arcTo(rect, 90 + theta / 2f, -theta, false)
        } else if (arcHeight == 0f) {
            path.lineTo(bounds.left.toFloat(), bounds.top.toFloat())
            path.lineTo(bounds.right.toFloat(), bounds.top.toFloat())
        } else {
            path.arcTo(rect, -90 - theta / 2f, theta, false)
        }
        path.lineTo(bounds.right.toFloat(), bounds.bottom.toFloat())
        path.close()
    }

    private fun fillPathAsCircle(path: Path, bounds: Rect) {
        path.addCircle(bounds.exactCenterX(), bounds.exactCenterY(),  min(bounds.width().toFloat(), bounds.height().toFloat()) / 2, Path.Direction.CW)
    }

    private fun fillPathAsRect(path: Path, bounds: Rect, type: Shape.Type.Rect) {
        when {
            type.cornerRadiiPx == null || type.cornerRadiiPx.all { it == 0f } -> {
                path.addRect(RectF(bounds), Path.Direction.CW)
            }
            else -> {
                path.addRoundRect(RectF(bounds), type.cornerRadiiPx, Path.Direction.CW)
            }
        }
    }

    override fun setAlpha(alpha: Int) {}

    override fun setColorFilter(colorFilter: ColorFilter?) {}

    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    sealed class Shape(
        val type: Type,
        val asset: AdaptyUI.ViewConfiguration.Asset.Filling.Local,
    ) {
        class Fill(
            type: Type,
            asset: AdaptyUI.ViewConfiguration.Asset.Filling.Local,
        ) : Shape(type, asset)

        class Stroke(
            type: Type,
            asset: AdaptyUI.ViewConfiguration.Asset.Filling.Local,
            val strokeThicknessPx: Float
        ) : Shape(type, asset)

        sealed class Type {
            class Rect(val cornerRadiiPx: FloatArray? = null): Type()
            object Circle: Type()
            class RectWithArc(val arcHeightPx: Float): Type()
        }
    }
}