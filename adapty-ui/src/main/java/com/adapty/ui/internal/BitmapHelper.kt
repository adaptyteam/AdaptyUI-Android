package com.adapty.ui.internal

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.annotation.RestrictTo
import com.adapty.ui.AdaptyUI.ViewConfiguration.Asset
import com.adapty.ui.AdaptyUI.ViewConfiguration.Asset.Image.Dimension
import com.adapty.ui.AdaptyUI.ViewConfiguration.Asset.Image.ScaleType

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class BitmapHelper {

    fun getBitmap(image: Asset.Image, boundsW: Int, boundsH: Int, scaleType: ScaleType): Bitmap? {
        val dim: Dimension
        val reqDim: Int

        val coef = when (scaleType) {
            ScaleType.FIT_MAX -> 1
            ScaleType.FIT_MIN -> -1
        }

        if ((boundsW - boundsH) * coef > 0) {
            dim = Dimension.WIDTH
            reqDim = boundsW
        } else {
            dim = Dimension.HEIGHT
            reqDim = boundsH
        }

        return getBitmap(image, reqDim, dim)
    }

    fun getBitmap(image: Asset.Image, reqDim: Int = 0, dim: Dimension = Dimension.WIDTH) : Bitmap? {
        return when (val source = image.source) {
            is Asset.Image.Source.Base64Str -> getBitmap(source, reqDim, dim)
            is Asset.Image.Source.File -> getBitmap(source, reqDim, dim)
        }
    }

    private fun getBitmap(source: Asset.Image.Source.Base64Str, reqDim: Int, dim: Dimension) : Bitmap? {
        if (source.imageBase64 == null) return null

        val byteArray = Base64.decode(source.imageBase64, Base64.DEFAULT)

        if (reqDim <= 0) {
            return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
        }

        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size, options)

        options.updateInSampleSize(reqDim, dim)
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size, options)
    }

    private fun getBitmap(source: Asset.Image.Source.File, reqDim: Int, dim: Dimension) : Bitmap? {
        if (reqDim <= 0) {
            return BitmapFactory.decodeFile(source.file.absolutePath)
        }

        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(source.file.absolutePath, options)

        options.updateInSampleSize(reqDim, dim)
        return BitmapFactory.decodeFile(source.file.absolutePath, options)
    }

    private fun BitmapFactory.Options.updateInSampleSize(reqDim: Int, dim: Dimension) {
        inSampleSize = calculateInSampleSize(
            when (dim) {
                Dimension.WIDTH -> this.outWidth
                Dimension.HEIGHT -> this.outHeight
            },
            reqDim,
        )
        inJustDecodeBounds = false
    }

    private fun calculateInSampleSize(initialDimValue: Int, reqDimValue: Int): Int {
        var inSampleSize = 1

        if (initialDimValue > reqDimValue) {
            val half: Int = initialDimValue / 2

            while (half / inSampleSize >= reqDimValue) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }
}