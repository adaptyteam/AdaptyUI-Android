@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal

import androidx.annotation.RestrictTo
import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.internal.utils.adaptyError
import com.adapty.models.AdaptyViewConfiguration
import com.adapty.models.AdaptyViewConfiguration.Asset
import com.adapty.models.AdaptyViewConfiguration.Component

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class ViewConfigDataProvider {

    fun <T : Component> getComponent(viewConfig: AdaptyViewConfiguration, componentId: String): T {
        checkConfig(viewConfig)
        return viewConfig.getComponent(componentId, STYLE_KEY_DEFAULT) ?: throw adaptyError(
            null,
            "AdaptyUIError: component not found ($componentId)",
            AdaptyErrorCode.DECODING_FAILED,
        )
    }

    fun <T : Asset> getAsset(viewConfig: AdaptyViewConfiguration, assetId: String): T {
        checkConfig(viewConfig)
        return viewConfig.getAsset(assetId) ?: throw adaptyError(
            null,
            "AdaptyUIError: component not found ($assetId)",
            AdaptyErrorCode.DECODING_FAILED,
        )
    }

    fun <T : Asset> getAssetForComponent(
        viewConfig: AdaptyViewConfiguration,
        componentId: String,
    ): T {
        checkConfig(viewConfig)
        val component =
            getComponent<Component.Reference>(viewConfig, componentId)
        return getAsset(viewConfig, component.assetId)
    }

    fun getString(viewConfig: AdaptyViewConfiguration, strId: String): String? {
        checkConfig(viewConfig)
        return viewConfig.getString(strId)
    }

    fun getTermsUrl(viewConfig: AdaptyViewConfiguration): String? {
        checkConfig(viewConfig)
        return viewConfig.getTermsUrl()
    }

    fun getPrivacyUrl(viewConfig: AdaptyViewConfiguration): String? {
        checkConfig(viewConfig)
        return viewConfig.getPrivacyUrl()
    }

    fun isHardPaywall(viewConfig: AdaptyViewConfiguration): Boolean {
        checkConfig(viewConfig)
        return viewConfig.isHard
    }

    private fun checkConfig(viewConfig: AdaptyViewConfiguration) {
        if (viewConfig.templateId != CONFIG_KEY_TEMPLATE_1) {
            throw adaptyError(
                null,
                "AdaptyUIError: unsupported template (${viewConfig.templateId})",
                AdaptyErrorCode.UNSUPPORTED_DATA,
            )
        }

        if (!viewConfig.hasStyle(STYLE_KEY_DEFAULT))
            throw adaptyError(
                null,
                "AdaptyUIError: style not found ($STYLE_KEY_DEFAULT)",
                AdaptyErrorCode.DECODING_FAILED,
            )
    }
}