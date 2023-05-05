package com.adapty.ui.internal

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import androidx.annotation.RestrictTo
import com.adapty.ui.R

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class DrawableHelper {

    fun getTickDrawable(context: Context, tintColor: Int) =
        context.resources.getDrawable(R.drawable.adapty_tick, null)
            ?.let { drawable ->
                drawable.mutate().apply {
                    setTint(tintColor)
                }
            }

    fun createMainProductBackgroundDrawable(cornerRadiusPx: Float, backgroundColor: Int): Drawable =
        createStrokelessRectangleDrawable(cornerRadiusPx, backgroundColor)

    fun createPurchaseButtonBackgroundDrawable(
        cornerRadiusPx: Float,
        backgroundColor: Int
    ): Drawable =
        createStrokelessRectangleDrawable(cornerRadiusPx, backgroundColor)

    private fun createStrokelessRectangleDrawable(
        cornerRadiusPx: Float,
        backgroundColor: Int
    ): Drawable =
        shape {
            shape = GradientDrawable.RECTANGLE
            color = ColorStateList.valueOf(backgroundColor)
            cornerRadius = cornerRadiusPx
        }

    fun createProductCellBackgroundDrawable(
        strokeWidth: Int,
        strokeColor: Int,
        backgroundColor: Int,
        cornerRadiusPx: Float,
    ): Drawable {
        val drawableShape = GradientDrawable.RECTANGLE
        val backgroundColorStateList = ColorStateList.valueOf(backgroundColor)

        return selector {
            state(android.R.attr.state_selected) {
                shape {
                    shape = drawableShape
                    stroke {
                        width = strokeWidth
                        color = strokeColor
                    }
                    color = backgroundColorStateList
                    cornerRadius = cornerRadiusPx
                }
            }

            state(android.R.attr.state_enabled) {
                shape {
                    shape = drawableShape
                    color = backgroundColorStateList
                    cornerRadius = cornerRadiusPx
                }
            }
        }
    }

    fun createContentBackgroundDrawable(
        backgroundColor: Int,
        cornerRadiusPx: Float,
    ): Drawable {
        return shape {
            shape = GradientDrawable.RECTANGLE
            color = ColorStateList.valueOf(backgroundColor)
            cornerRadii {
                topLeft = cornerRadiusPx
                topRight = cornerRadiusPx
                bottomRight = 0f
                bottomLeft = 0f
            }
        }
    }
}