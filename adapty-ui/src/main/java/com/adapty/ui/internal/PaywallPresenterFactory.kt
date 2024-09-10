@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal

import android.content.Context
import androidx.annotation.RestrictTo
import com.adapty.internal.di.DIObject
import com.adapty.internal.di.Dependencies
import com.adapty.internal.di.Dependencies.OBSERVER_MODE
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.internal.utils.PriceFormatter
import com.adapty.ui.internal.cache.MediaFetchService

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal object PaywallPresenterFactory {

    fun create(flowKey: String, uiContext: Context): PaywallPresenter? {
        val bitmapHelper = BitmapHelper()
        val shaderHelper = ShaderHelper(bitmapHelper)
        val drawableHelper = DrawableHelper(shaderHelper, bitmapHelper)
        val textHelper = TextHelper(flowKey)
        val textComponentHelper = TextComponentHelper(flowKey, bitmapHelper)
        val mediaFetchService = runCatching { Dependencies.injectInternal<MediaFetchService>() }.getOrNull() ?: return null
        val viewHelper = ViewHelper(flowKey, drawableHelper, textHelper, textComponentHelper, bitmapHelper, mediaFetchService)
        val layoutHelper = LayoutHelper()
        val locale = uiContext.getCurrentLocale()
        val priceFormatter = runCatching {
            Dependencies.injectInternal<PriceFormatter>(locale.toString()) {
                DIObject({ PriceFormatter(locale) })
            }
        }.getOrNull() ?: return null
        val productBlockRenderer =
            ProductBlockRenderer(viewHelper, layoutHelper, textComponentHelper, priceFormatter)
        val layoutBuilder =
            PaywallUiManager(flowKey, viewHelper, layoutHelper, productBlockRenderer)
        val isObserverMode = runCatching { Dependencies.injectInternal<Boolean>(OBSERVER_MODE) }.getOrNull() ?: return null
        return PaywallPresenter(flowKey, isObserverMode, layoutBuilder)
    }
}