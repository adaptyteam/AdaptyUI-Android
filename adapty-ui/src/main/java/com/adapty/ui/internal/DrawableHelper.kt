package com.adapty.ui.internal

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.StateListDrawable
import androidx.annotation.RestrictTo
import com.adapty.models.AdaptyViewConfiguration
import com.adapty.models.AdaptyViewConfiguration.Component.Shape
import com.adapty.models.AdaptyViewConfiguration.Component.Shape.CornerRadius

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class DrawableHelper(
    private val shaderHelper: ShaderHelper,
) {

    fun createDrawable(
        filling: AdaptyViewConfiguration.Asset.Filling,
    ): Drawable {
        return ShapeDrawable(
            ShapeDrawable.Shape.Fill(ShapeDrawable.Shape.Type.Rect(), filling),
            null,
            shaderHelper,
        )
    }

    fun createDrawable(
        shapes: Collection<Pair<Int, Shape?>>,
        templateConfig: TemplateConfig,
        context: Context,
    ): Drawable {
        val shapes = shapes.mapNotNull { (stateRes, shape) -> shape?.let { stateRes to shape } }

        if (shapes.size == 1)
            return createDrawable(shapes[0].second, templateConfig, context)

        return StateListDrawable().apply {
            shapes.forEach { (stateRes, shape) ->
                addState(intArrayOf(stateRes), createDrawable(shape, templateConfig, context))
            }
        }
    }

    fun createDrawable(
        shape: Shape,
        templateConfig: TemplateConfig,
        context: Context,
    ): Drawable {
        val shapeType = when (val type = shape.type) {
            is Shape.Type.Rectangle -> {
                val cornerRadius = type.cornerRadius
                val cornerRadiiPx = when {
                    cornerRadius is CornerRadius.Same && cornerRadius.value > 0f -> {
                        val cornerRadiusPx = cornerRadius.value.dp(context)
                        floatArrayOf(
                            cornerRadiusPx, cornerRadiusPx,
                            cornerRadiusPx, cornerRadiusPx,
                            cornerRadiusPx, cornerRadiusPx,
                            cornerRadiusPx, cornerRadiusPx,
                        )
                    }
                    cornerRadius is CornerRadius.Different && !(cornerRadius.bottomLeft == 0f && cornerRadius.bottomRight == 0f && cornerRadius.topLeft == 0f && cornerRadius.topRight == 0f) -> {
                        val topLeftPx = cornerRadius.topLeft.dp(context)
                        val topRightPx = cornerRadius.topRight.dp(context)
                        val bottomRightPx = cornerRadius.bottomRight.dp(context)
                        val bottomLeftPx = cornerRadius.bottomLeft.dp(context)
                        floatArrayOf(
                            topLeftPx, topLeftPx,
                            topRightPx, topRightPx,
                            bottomRightPx, bottomRightPx,
                            bottomLeftPx, bottomLeftPx,
                        )
                    }
                    else -> null
                }

                ShapeDrawable.Shape.Type.Rect(cornerRadiiPx)
            }

            Shape.Type.Circle -> ShapeDrawable.Shape.Type.Circle

            is Shape.Type.RectWithArc -> ShapeDrawable.Shape.Type.RectWithArc(type.arcHeight.dp(context))
        }

        val fillShape = shape.backgroundAssetId?.let { assetId ->
            val asset = templateConfig.getAsset<AdaptyViewConfiguration.Asset.Filling>(assetId)

            ShapeDrawable.Shape.Fill(shapeType, asset)
        }

        val strokeShape = shape.border?.let { border ->
            val asset = templateConfig.getAsset<AdaptyViewConfiguration.Asset.Filling>(border.assetId)

            ShapeDrawable.Shape.Stroke(shapeType, asset, border.thickness.dp(context))
        }

        return ShapeDrawable(fillShape, strokeShape, shaderHelper)
    }

    fun createForegroundRippleDrawable(
        context: Context,
        resId: Int,
        bgDrawable: Drawable,
    ): Drawable? {
        if (resId == 0) return null

        val mask = bgDrawable

        return context.getDrawable(resId)?.let { ripple ->
            (ripple.mutate() as? RippleDrawable)?.apply {
                setDrawableByLayerId(android.R.id.mask, mask)
            }
        }
    }

    fun createTimelineDrawable(
        timelineEntry: TimelineEntry,
        templateConfig: TemplateConfig,
        context: Context,
    ): Drawable {
        val bgSizePx = TIMELINE_DRAWABLE_BACKGROUND_WIDTH_DP.dp(context)
        val lineWidthPx = TIMELINE_DRAWABLE_LINE_WIDTH_DP.dp(context)

        val bitmap = templateConfig.getAsset<AdaptyViewConfiguration.Asset.Image>(
            timelineEntry.image.assetId,
        ).getBitmap(bgSizePx.toInt(), AdaptyViewConfiguration.Asset.Image.Dimension.WIDTH)

        val circle = createDrawable(timelineEntry.shape, templateConfig, context)

        val icon = BitmapDrawable(context.resources, bitmap)
        timelineEntry.tintColor?.let { tint ->
            val tintColor =
                templateConfig.getAsset<AdaptyViewConfiguration.Asset.Color>(tint.assetId).value
            icon.colorFilter = PorterDuffColorFilter(tintColor, PorterDuff.Mode.SRC_IN)
        }

        val lineGradient = templateConfig.getAsset<AdaptyViewConfiguration.Asset>(
            timelineEntry.gradient.assetId,
        )

        return FeatureTimelineDrawable(circle, icon, bgSizePx, lineWidthPx, lineGradient, shaderHelper)
    }
}