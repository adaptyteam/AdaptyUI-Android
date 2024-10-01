@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.ui.attributes

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.adapty.internal.utils.InternalAdaptyApi

internal data class EdgeEntities(
    val left: DimUnit,
    val top: DimUnit,
    val right: DimUnit,
    val bottom: DimUnit,
) {
    constructor(horizontal: DimUnit, vertical: DimUnit): this(horizontal, vertical, horizontal, vertical)
    constructor(all: DimUnit): this(all, all)
    constructor(all: Float): this(DimUnit.Exact(all))
}

@Composable
internal fun EdgeEntities.toPaddingValues(): PaddingValues {
    val (left, top, right, bottom) = this
    return PaddingValues(
        left.toExactDp(DimSpec.Axis.X),
        top.toExactDp(DimSpec.Axis.Y),
        right.toExactDp(DimSpec.Axis.X),
        bottom.toExactDp(DimSpec.Axis.Y),
    )
}

internal val EdgeEntities.horizontalSum @Composable get() = left.toExactDp(DimSpec.Axis.X) + right.toExactDp(
    DimSpec.Axis.X
)
internal val EdgeEntities.verticalSum @Composable get() = top.toExactDp(DimSpec.Axis.Y) + bottom.toExactDp(
    DimSpec.Axis.Y
)

internal val EdgeEntities?.horizontalSumOrDefault @Composable get() = this?.horizontalSum ?: 0.dp
internal val EdgeEntities?.verticalSumOrDefault @Composable get() = this?.verticalSum ?: 0.dp