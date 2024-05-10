package com.adapty.ui.internal

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.Shader
import android.graphics.SweepGradient
import androidx.annotation.RestrictTo
import com.adapty.ui.AdaptyUI
import java.lang.Float.min
import kotlin.math.roundToInt

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class ShaderHelper(
    private val bitmapHelper: BitmapHelper,
) {

    fun createShader(bounds: Rect, gradient: AdaptyUI.ViewConfiguration.Asset.Gradient): Shader {
        val colors = gradient.colors
        val positions = gradient.positions

        return when (gradient.type) {
            AdaptyUI.ViewConfiguration.Asset.Gradient.Type.LINEAR -> {
                val (x0, y0, x1, y1) = gradient.points

                val w = bounds.width()
                val h = bounds.height()

                LinearGradient(
                    bounds.left + w * x0,
                    bounds.top + h * y0,
                    bounds.left + w * x1,
                    bounds.top + h * y1,
                    colors, positions, Shader.TileMode.CLAMP,
                )
            }

            AdaptyUI.ViewConfiguration.Asset.Gradient.Type.RADIAL -> {
                RadialGradient(
                    bounds.exactCenterX(), bounds.exactCenterY(),
                    min(bounds.width().toFloat(), bounds.height().toFloat()),
                    colors, positions, Shader.TileMode.CLAMP,
                )
            }

            AdaptyUI.ViewConfiguration.Asset.Gradient.Type.CONIC -> {
                SweepGradient(
                    bounds.exactCenterX(), bounds.exactCenterY(),
                    colors, positions,
                )
            }
        }
    }

    fun createShader(bounds: Rect, image: AdaptyUI.ViewConfiguration.Asset.Image): Shader? {
        val reqDimValue: Int
        val dim: AdaptyUI.ViewConfiguration.Asset.Image.Dimension
        val boundsWidth = bounds.width()
        val boundsHeight = bounds.height()
        if (boundsWidth > boundsHeight) {
            reqDimValue = boundsWidth
            dim = AdaptyUI.ViewConfiguration.Asset.Image.Dimension.WIDTH
        } else {
            reqDimValue = boundsHeight
            dim = AdaptyUI.ViewConfiguration.Asset.Image.Dimension.HEIGHT
        }
        return bitmapHelper.getBitmap(image, reqDimValue, dim)?.let { bitmap ->
            createShader(bounds, bitmap)
        }
    }

    fun createShader(bounds: Rect, bitmap: Bitmap): Shader {
        return BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            .also { shader ->
                shader.setLocalMatrix(
                    Matrix().apply {
                        val bitmapWidth = bitmap.width
                        val bitmapHeight = bitmap.height
                        val boundsWidth = bounds.width()
                        val boundsHeight = bounds.height()

                        val scale: Float; val dx: Float; val dy: Float

                        if (bitmapWidth * boundsHeight > boundsWidth * bitmapHeight) {
                            scale = boundsHeight.toFloat() / bitmapHeight
                            dx = (boundsWidth - bitmapWidth * scale) * 0.5f
                            dy = 0f
                        } else {
                            scale = boundsWidth.toFloat() / bitmapWidth
                            dx = 0f
                            dy = (boundsHeight - bitmapHeight * scale) * 0.5f
                        }

                        setScale(scale, scale)
                        postTranslate(dx.roundToInt().toFloat(), dy.roundToInt().toFloat())
                    }
                )
            }
    }
}