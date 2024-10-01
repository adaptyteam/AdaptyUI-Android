package com.adapty.ui.internal.ui.element

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.internal.mapping.element.Assets
import com.adapty.ui.internal.text.StringId
import com.adapty.ui.internal.text.StringWrapper
import com.adapty.ui.internal.ui.attributes.Align
import com.adapty.ui.internal.ui.attributes.DimSpec
import com.adapty.ui.internal.ui.attributes.toComposeAlignment
import com.adapty.ui.internal.ui.fillWithBaseParams
import com.adapty.ui.internal.utils.EventCallback

@InternalAdaptyApi
public class GridItem internal constructor(
    dimAxis: DimSpec.Axis,
    sideSpec: DimSpec?,
    override var content: UIElement,
    internal val align: Align,
    baseProps: BaseProps,
) : UIElement, SingleContainer {

    override val baseProps: BaseProps =
        if (dimAxis == DimSpec.Axis.X)
            baseProps.copy(widthSpec = sideSpec)
        else
            baseProps.copy(heightSpec = sideSpec)

    override fun toComposable(
        resolveAssets: () -> Assets,
        resolveText: @Composable (StringId) -> StringWrapper?,
        resolveState: () -> Map<String, Any>,
        eventCallback: EventCallback,
        modifier: Modifier,
    ): @Composable () -> Unit = {
        Box(
            contentAlignment = align.toComposeAlignment(),
            modifier = modifier.fillMaxSize(),
        ) {
            content.toComposable(
                resolveAssets,
                resolveText,
                resolveState,
                eventCallback,
                Modifier.fillWithBaseParams(content, resolveAssets),
            ).invoke()
        }
    }

    override fun ColumnScope.toComposableInColumn(
        resolveAssets: () -> Assets,
        resolveText: @Composable (StringId) -> StringWrapper?,
        resolveState: () -> Map<String, Any>,
        eventCallback: EventCallback,
        modifier: Modifier,
    ): @Composable () -> Unit = {
        Box(
            contentAlignment = align.toComposeAlignment(),
            modifier = modifier.fillMaxWidth(),
        ) {
            content.run {
                this@toComposableInColumn.toComposableInColumn(
                    resolveAssets,
                    resolveText,
                    resolveState,
                    eventCallback,
                    this@toComposableInColumn.fillModifierWithScopedParams(
                        content,
                        Modifier.fillWithBaseParams(content, resolveAssets)
                    ),
                ).invoke()
            }
        }
    }

    override fun RowScope.toComposableInRow(
        resolveAssets: () -> Assets,
        resolveText: @Composable (StringId) -> StringWrapper?,
        resolveState: () -> Map<String, Any>,
        eventCallback: EventCallback,
        modifier: Modifier,
    ): @Composable () -> Unit = {
        Box(
            contentAlignment = align.toComposeAlignment(),
            modifier = modifier.fillMaxHeight(),
        ) {
            content.run {
                this@toComposableInRow.toComposableInRow(
                    resolveAssets,
                    resolveText,
                    resolveState,
                    eventCallback,
                    this@toComposableInRow.fillModifierWithScopedParams(
                        content,
                        Modifier.fillWithBaseParams(content, resolveAssets)
                    ),
                ).invoke()
            }
        }
    }
}