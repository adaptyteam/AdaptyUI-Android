package com.adapty.ui.internal.ui.element

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.internal.mapping.element.Assets
import com.adapty.ui.internal.text.StringId
import com.adapty.ui.internal.text.StringWrapper
import com.adapty.ui.internal.ui.attributes.TextAlign
import com.adapty.ui.internal.utils.EventCallback

@InternalAdaptyApi
public class TextElement internal constructor(
    internal val stringId: StringId,
    textAlign: TextAlign,
    internal val maxLines: Int?,
    internal val onOverflow: OnOverflowMode?,
    attributes: Attributes,
    baseProps: BaseProps,
) : BaseTextElement(textAlign, attributes, baseProps) {

    override fun toComposable(
        resolveAssets: () -> Assets,
        resolveText: @Composable (StringId) -> StringWrapper?,
        resolveState: () -> Map<String, Any>,
        eventCallback: EventCallback,
        modifier: Modifier,
    ): @Composable () -> Unit = {
        renderTextInternal(
            attributes,
            textAlign,
            maxLines,
            onOverflow,
            modifier,
            resolveAssets,
        ) {
            resolveText(stringId)
        }
    }
}