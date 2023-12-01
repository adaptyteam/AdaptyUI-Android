package com.adapty.ui.internal

import android.content.Context
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal object PaywallPresenterFactory {

    fun create(flowKey: String, uiContext: Context): PaywallPresenter {
        val shaderHelper = ShaderHelper()
        val drawableHelper = DrawableHelper(shaderHelper)
        val textHelper = TextHelper(flowKey)
        val textComponentHelper = TextComponentHelper(flowKey)
        val viewHelper = ViewHelper(drawableHelper, textHelper, textComponentHelper)
        val layoutHelper = LayoutHelper()
        val productBlockRenderer =
            ProductBlockRenderer(viewHelper, layoutHelper, textComponentHelper, uiContext.getCurrentLocale())
        val layoutBuilder =
            PaywallUiManager(flowKey, viewHelper, layoutHelper, productBlockRenderer)
        return PaywallPresenter(flowKey, layoutBuilder)
    }
}