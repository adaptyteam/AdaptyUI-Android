package com.adapty.ui.internal.ui.attributes

import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale

internal enum class AspectRatio {
    FIT, FILL, STRETCH
}

internal fun AspectRatio.toComposeContentScale() =
    when (this) {
        AspectRatio.STRETCH -> ContentScale.FillBounds
        AspectRatio.FILL -> ContentScale.Crop
        else -> ContentScale.Fit
    }

internal fun AspectRatio.evaluateComposeImageAlignment() =
    when (this) {
        AspectRatio.FILL -> Alignment.TopCenter
        else -> Alignment.Center
    }