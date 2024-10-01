@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.AdaptyUI.LocalizedViewConfiguration.Screen
import com.adapty.ui.AdaptyUI.LocalizedViewConfiguration.ScreenBundle
import com.adapty.ui.internal.mapping.element.Assets
import com.adapty.ui.internal.text.StringId
import com.adapty.ui.internal.text.StringWrapper
import com.adapty.ui.internal.ui.attributes.DimSpec
import com.adapty.ui.internal.ui.attributes.Shape
import com.adapty.ui.internal.ui.attributes.toExactDp
import com.adapty.ui.internal.utils.EventCallback

@Composable
internal fun renderDefaultScreen(
    screenBundle: ScreenBundle,
    resolveAssets: () -> Assets,
    resolveText: @Composable (StringId) -> StringWrapper?,
    resolveState: () -> SnapshotStateMap<String, Any>,
    eventCallback: EventCallback,
) {
    when (val defaultScreen = screenBundle.defaultScreen) {
        is Screen.Default.Basic -> renderBasicTemplate(
            defaultScreen,
            resolveAssets,
            resolveText,
            resolveState,
            eventCallback,
        )
        is Screen.Default.Flat -> renderFlatTemplate(
            defaultScreen,
            resolveAssets,
            resolveText,
            resolveState,
            eventCallback,
        )
        is Screen.Default.Transparent -> renderTransparentTemplate(
            defaultScreen,
            resolveAssets,
            resolveText,
            resolveState,
            eventCallback,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun renderBasicTemplate(
    defaultScreen: Screen.Default,
    resolveAssets: () -> Assets,
    resolveText: @Composable (StringId) -> StringWrapper?,
    resolveState: () -> SnapshotStateMap<String, Any>,
    eventCallback: EventCallback,
) {
    val finalContentHeightState = remember { mutableStateOf<Dp?>(null) }
    val initialContentHeightPxState = remember { mutableIntStateOf(0) }
    val initialFooterHeightPxState = remember { mutableStateOf(0.takeIf { defaultScreen.footer != null }) }

    val coverHeightOrNull = (defaultScreen.cover?.baseProps?.heightSpec as? DimSpec.Specified)
        ?.value
        ?.toExactDp(DimSpec.Axis.Y)

    BoxWithConstraints(
        contentAlignment = Alignment.TopCenter,
        modifier = Modifier
            .clickable(enabled = false, onClick = {})
            .backgroundOrSkip(Shape.plain(defaultScreen.background), resolveAssets),
    ) {
        defaultScreen.cover?.let { cover ->
            cover.toComposable(
                resolveAssets,
                resolveText,
                resolveState,
                eventCallback,
                Modifier.fillWithBaseParams(cover, resolveAssets),
            ).invoke()
        }
        val boxMaxHeight = maxHeight
        val contentOffsetY = remember {
            (defaultScreen.content.baseProps.offset?.y ?: 0f).dp
        }
        defaultScreen.content.baseProps.offset?.consumed = true
        val configuration = LocalConfiguration.current
        val density = LocalDensity.current
        val windowInsets = WindowInsets.systemBars

        CompositionLocalProvider(
            LocalOverscrollConfiguration provides null,
        ) {
            defaultScreen.content.toComposable(
                resolveAssets,
                resolveText,
                resolveState,
                eventCallback,
                Modifier
                    .verticalScroll(rememberScrollState())
                    .onSizeChanged { size ->
                        if (size.height <= 0 || initialContentHeightPxState.intValue > 0)
                            return@onSizeChanged
                        initialContentHeightPxState.intValue = size.height
                    }
                    .run {
                        val finalContentHeight = finalContentHeightState.value
                        if (finalContentHeight != null)
                            return@run height(finalContentHeight)
                        val initialFooterHeightPx = initialFooterHeightPxState.value
                        if (initialFooterHeightPx == 0) return@run this
                        val initialContentHeightPx = initialContentHeightPxState.intValue
                        with(density) {
                            val footerOverlap = initialFooterHeightPx?.let {
                                (initialContentHeightPx + initialFooterHeightPx - (configuration.screenHeightDp.dp.toPx() + (windowInsets.getBottom(this) + windowInsets.getTop(this))))
                                    .coerceAtLeast(0f)
                            } ?: 0f

                            val minimumHeight = boxMaxHeight + footerOverlap.toDp()

                            val contentHeight = max(minimumHeight, initialContentHeightPx.toDp())
                            height(contentHeight)
                                .also { finalContentHeightState.value = contentHeight }
                        }
                    }
                    .padding(
                        PaddingValues(top = (coverHeightOrNull ?: 0.dp) + contentOffsetY)
                    )
                    .fillWithBaseParams(defaultScreen.content, resolveAssets),
            ).invoke()
        }

        defaultScreen.footer?.let { footer ->
            footer.toComposable(
                resolveAssets,
                resolveText,
                resolveState,
                eventCallback,
                Modifier
                    .align(Alignment.BottomCenter)
                    .onSizeChanged { size ->
                        if (size.height <= 0 || (initialFooterHeightPxState.value ?: 0) > 0)
                            return@onSizeChanged
                        initialFooterHeightPxState.value = size.height
                    }
                    .fillWithBaseParams(footer, resolveAssets),
            ).invoke()
        }
        defaultScreen.overlay?.let { overlay ->
            overlay.toComposable(
                resolveAssets,
                resolveText,
                resolveState,
                eventCallback,
                Modifier.fillWithBaseParams(overlay, resolveAssets),
            ).invoke()
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun renderFlatTemplate(
    defaultScreen: Screen.Default,
    resolveAssets: () -> Assets,
    resolveText: @Composable (StringId) -> StringWrapper?,
    resolveState: () -> SnapshotStateMap<String, Any>,
    eventCallback: EventCallback,
) {
    val finalContentHeightState = remember { mutableStateOf<Dp?>(null) }
    val initialContentHeightPxState = remember { mutableIntStateOf(0) }
    val initialFooterHeightPxState = remember { mutableStateOf(0.takeIf { defaultScreen.footer != null }) }

    BoxWithConstraints(
        contentAlignment = Alignment.TopCenter,
        modifier = Modifier
            .clickable(enabled = false, onClick = {})
            .backgroundOrSkip(Shape.plain(defaultScreen.background), resolveAssets),
    ) {
        defaultScreen.cover?.let { cover ->
            cover.toComposable(
                resolveAssets,
                resolveText,
                resolveState,
                eventCallback,
                Modifier.fillWithBaseParams(cover, resolveAssets)
            ).invoke()
        }
        val boxMaxHeight = maxHeight
        val configuration = LocalConfiguration.current
        val density = LocalDensity.current
        val windowInsets = WindowInsets.systemBars

        CompositionLocalProvider(
            LocalOverscrollConfiguration provides null,
        ) {
            defaultScreen.content.toComposable(
                resolveAssets,
                resolveText,
                resolveState,
                eventCallback,
                Modifier
                    .onSizeChanged { size ->
                        if (size.height <= 0 || initialContentHeightPxState.intValue > 0)
                            return@onSizeChanged
                        initialContentHeightPxState.intValue = size.height
                    }
                    .run {
                        val finalContentHeight = finalContentHeightState.value
                        if (finalContentHeight != null)
                            return@run height(finalContentHeight)
                        val initialFooterHeightPx = initialFooterHeightPxState.value
                        if (initialFooterHeightPx == 0) return@run this
                        val initialContentHeightPx = initialContentHeightPxState.intValue
                        with(density) {
                            val footerOverlap = initialFooterHeightPx?.let {
                                (initialContentHeightPx + initialFooterHeightPx - (configuration.screenHeightDp.dp.toPx() + (windowInsets.getBottom(this) + windowInsets.getTop(this))))
                                    .coerceAtLeast(0f)
                            } ?: 0f

                            val minimumHeight = boxMaxHeight + footerOverlap.toDp()

                            val contentHeight = max(minimumHeight, initialContentHeightPx.toDp())
                            height(contentHeight)
                                .also { finalContentHeightState.value = contentHeight }
                        }
                    }
                    .fillWithBaseParams(defaultScreen.content, resolveAssets)
                    .verticalScroll(rememberScrollState()),
            ).invoke()
        }

        defaultScreen.footer?.let { footer ->
            footer.toComposable(
                resolveAssets,
                resolveText,
                resolveState,
                eventCallback,
                Modifier
                    .align(Alignment.BottomCenter)
                    .onSizeChanged { size ->
                        if (size.height <= 0 || (initialFooterHeightPxState.value ?: 0) > 0)
                            return@onSizeChanged
                        initialFooterHeightPxState.value = size.height
                    }
                    .fillWithBaseParams(footer, resolveAssets),
            ).invoke()
        }
        defaultScreen.overlay?.let { overlay ->
            overlay.toComposable(
                resolveAssets,
                resolveText,
                resolveState,
                eventCallback,
                Modifier.fillWithBaseParams(overlay, resolveAssets),
            ).invoke()
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun renderTransparentTemplate(
    defaultScreen: Screen.Default,
    resolveAssets: () -> Assets,
    resolveText: @Composable (StringId) -> StringWrapper?,
    resolveState: () -> SnapshotStateMap<String, Any>,
    eventCallback: EventCallback,
) {
    Box(
        contentAlignment = Alignment.BottomCenter,
        modifier = Modifier
            .clickable(enabled = false, onClick = {})
            .backgroundOrSkip(Shape.plain(defaultScreen.background), resolveAssets),
    ) {
        defaultScreen.content.toComposable(
            resolveAssets,
            resolveText,
            resolveState,
            eventCallback,
            Modifier.fillWithBaseParams(defaultScreen.content, resolveAssets),
        ).invoke()
        defaultScreen.footer?.let { footer ->
            CompositionLocalProvider(
                LocalOverscrollConfiguration provides null,
            ) {
                footer.toComposable(
                    resolveAssets,
                    resolveText,
                    resolveState,
                    eventCallback,
                    Modifier
                        .verticalScroll(rememberScrollState(), reverseScrolling = true)
                        .height(IntrinsicSize.Max)
                        .fillWithBaseParams(footer, resolveAssets)
                ).invoke()
            }
        }
        defaultScreen.overlay?.let { overlay ->
            overlay.toComposable(
                resolveAssets,
                resolveText,
                resolveState,
                eventCallback,
                Modifier.fillWithBaseParams(overlay, resolveAssets),
            ).invoke()
        }
    }
}