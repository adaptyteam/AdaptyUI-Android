package com.adapty.ui.internal

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal object PaywallPresenterFactory {

    fun create(flowKey: String): PaywallPresenter {
        val configDataProvider = ViewConfigDataProvider()
        val drawableHelper = DrawableHelper()
        val textHelper = TextHelper()
        val layoutBuilder =
            PaywallUiManager(flowKey, configDataProvider, drawableHelper, textHelper)
        return PaywallPresenter(flowKey, layoutBuilder)
    }
}