@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal

import android.view.View
import androidx.annotation.RestrictTo
import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.internal.utils.adaptyError
import com.adapty.models.AdaptyViewConfiguration

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal sealed class Features {
    class List(val textComponent: AdaptyViewConfiguration.Component.Text): Features()
    class TimeLine(val timelineEntries: kotlin.collections.List<TimelineEntry>): Features()
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class TimelineEntry(
    val text: AdaptyViewConfiguration.Component.Text,
    val image: AdaptyViewConfiguration.Component.Reference,
    val shape: AdaptyViewConfiguration.Component.Shape,
    val tintColor: AdaptyViewConfiguration.Component.Reference?,
    val gradient: AdaptyViewConfiguration.Component.Reference,
) {
    companion object {
        fun from(map: Map<String, AdaptyViewConfiguration.Component>) =
            TimelineEntry(
                map["text"] as? AdaptyViewConfiguration.Component.Text ?: throw adaptyError(
                    null,
                    "AdaptyUIError: text property not found for timeline entry",
                    AdaptyErrorCode.DECODING_FAILED,
                ),
                (map["image"] as? AdaptyViewConfiguration.Component.Reference) ?: throw adaptyError(
                    null,
                    "AdaptyUIError: image property not found for timeline entry",
                    AdaptyErrorCode.DECODING_FAILED,
                ),
                (map["shape"] as? AdaptyViewConfiguration.Component.Shape) ?: throw adaptyError(
                    null,
                    "AdaptyUIError: shape property not found for timeline entry",
                    AdaptyErrorCode.DECODING_FAILED,
                ),
                map["image_color"] as? AdaptyViewConfiguration.Component.Reference,
                (map["gradient"] as? AdaptyViewConfiguration.Component.Reference) ?: throw adaptyError(
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
