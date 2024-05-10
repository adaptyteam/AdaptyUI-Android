@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal

import android.view.View
import androidx.annotation.RestrictTo
import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.internal.utils.adaptyError
import com.adapty.ui.AdaptyUI

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal sealed class Features {
    class List(val textComponent: AdaptyUI.ViewConfiguration.Component.Text): Features()
    class TimeLine(val timelineEntries: kotlin.collections.List<TimelineEntry>): Features()
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class TimelineEntry(
    val text: AdaptyUI.ViewConfiguration.Component.Text,
    val image: AdaptyUI.ViewConfiguration.Component.Reference,
    val shape: AdaptyUI.ViewConfiguration.Component.Shape,
    val tintColor: AdaptyUI.ViewConfiguration.Component.Reference?,
    val gradient: AdaptyUI.ViewConfiguration.Component.Reference,
) {
    companion object {
        fun from(map: Map<String, AdaptyUI.ViewConfiguration.Component>) =
            TimelineEntry(
                map["text"] as? AdaptyUI.ViewConfiguration.Component.Text ?: throw adaptyError(
                    null,
                    "AdaptyUIError: text property not found for timeline entry",
                    AdaptyErrorCode.DECODING_FAILED,
                ),
                (map["image"] as? AdaptyUI.ViewConfiguration.Component.Reference) ?: throw adaptyError(
                    null,
                    "AdaptyUIError: image property not found for timeline entry",
                    AdaptyErrorCode.DECODING_FAILED,
                ),
                (map["shape"] as? AdaptyUI.ViewConfiguration.Component.Shape) ?: throw adaptyError(
                    null,
                    "AdaptyUIError: shape property not found for timeline entry",
                    AdaptyErrorCode.DECODING_FAILED,
                ),
                map["image_color"] as? AdaptyUI.ViewConfiguration.Component.Reference,
                (map["gradient"] as? AdaptyUI.ViewConfiguration.Component.Reference) ?: throw adaptyError(
                    null,
                    "AdaptyUIError: gradient property not found for timeline entry",
                    AdaptyErrorCode.DECODING_FAILED,
                ),
            )
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal sealed class FeatureUIBlock {
    class List(val textView: View): FeatureUIBlock()
    class TimeLine(val entries: kotlin.collections.List<Cell>): FeatureUIBlock() {
        class Cell(
            val imageView: View,
            val textView: View,
            val drawableWidthPx: Int,
            val textStartMarginPx: Int,
        )
    }
}
