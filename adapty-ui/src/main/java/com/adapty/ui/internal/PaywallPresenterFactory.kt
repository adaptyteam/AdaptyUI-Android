package com.adapty.ui.internal

import androidx.annotation.RestrictTo
import java.text.Format
import java.text.NumberFormat

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal object PaywallPresenterFactory {

    fun create(flowKey: String): PaywallPresenter {
        val shaderHelper = ShaderHelper()
        val drawableHelper = DrawableHelper(shaderHelper)
        val textHelper = TextHelper(flowKey)
        val textComponentHelper = TextComponentHelper(flowKey)
        val viewHelper = ViewHelper(drawableHelper, textHelper, textComponentHelper)
        val layoutHelper = LayoutHelper()
        val numberFormat: Format = NumberFormat.getInstance().apply { minimumFractionDigits = 2 }
        val productBlockRenderer =
            ProductBlockRenderer(viewHelper, layoutHelper, textComponentHelper, numberFormat)
        val layoutBuilder =
            PaywallUiManager(flowKey, viewHelper, layoutHelper, productBlockRenderer)
        return PaywallPresenter(flowKey, layoutBuilder)
    }
}