@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal

import androidx.annotation.RestrictTo
import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.internal.utils.adaptyError
import com.adapty.models.AdaptyViewConfiguration
import com.adapty.models.AdaptyViewConfiguration.Asset
import com.adapty.models.AdaptyViewConfiguration.Component
import com.adapty.models.AdaptyViewConfiguration.ProductBlock

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class BasicTemplateConfig(viewConfig: AdaptyViewConfiguration): TemplateConfig(viewConfig) {

    override val renderDirection: RenderDirection get() = RenderDirection.TOP_TO_BOTTOM

    val mainImageRelativeHeight: Float get() = viewConfig.mainImageRelativeHeight

    override fun getScreenBackground(): Asset.Filling {
        val component = getComponent<Component.Reference>(COMPONENT_KEY_COVER_IMAGE)
        return getAsset(component.assetId)
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class TransparentTemplateConfig(viewConfig: AdaptyViewConfiguration): TemplateConfig(viewConfig) {

    override val renderDirection: RenderDirection get() = RenderDirection.BOTTOM_TO_TOP

    override fun getScreenBackground(): Asset.Filling {
        val component = getComponent<Component.Reference>(COMPONENT_KEY_BACKGROUND_IMAGE)
        return getAsset(component.assetId)
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class FlatTemplateConfig(viewConfig: AdaptyViewConfiguration): TemplateConfig(viewConfig) {

    override val renderDirection: RenderDirection get() = RenderDirection.TOP_TO_BOTTOM

    val mainImageRelativeHeight: Float get() = viewConfig.mainImageRelativeHeight

    override fun getScreenBackground(): Asset.Filling? {
        val component = getComponentOrNull<Component.Reference>(COMPONENT_KEY_BACKGROUND)
        return component?.assetId?.let { assetId -> getAsset(assetId) }
    }

    fun getTopImage(): Component.Shape = getComponent(COMPONENT_KEY_COVER_IMAGE)
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal sealed class TemplateConfig(protected val viewConfig: AdaptyViewConfiguration) {

    abstract val renderDirection: RenderDirection

    abstract fun getScreenBackground(): Asset.Filling?

    fun getContentBackground(): Component.Shape {
        return getComponent(COMPONENT_KEY_MAIN_CONTENT_SHAPE)
    }

    val contentEdgeMargin: Float = CONTENT_HORIZONTAL_EDGE_MARGIN_DP

    val verticalSpacing: Float = CONTENT_VERTICAL_SPACING_DP

    val featureSpacing: Float = FEATURE_VERTICAL_SPACING_DP

    val purchaseButtonHeight: Float = PURCHASE_BUTTON_HEIGHT_DP

    fun getTitle(): Component.Text? = getComponentOrNull(COMPONENT_KEY_TITLE_ROWS)

    fun getPurchaseButton(): Component.Button = getComponent(COMPONENT_KEY_PURCHASE_BUTTON)

    fun getPurchaseButtonOfferTitle(): Component.Text? =
        getComponentOrNull(COMPONENT_KEY_PURCHASE_BUTTON_OFFER_TITLE)

    fun getCloseButton(): Component.Button = getComponent(COMPONENT_KEY_CLOSE_BUTTON)

    fun isReverseProductAddingOrder(productBlockType: Products.BlockType): Boolean {
        return renderDirection != RenderDirection.TOP_TO_BOTTOM && productBlockType == Products.BlockType.Vertical
    }

    protected fun <T : Component> getComponent(componentId: String): T {
        return getComponentOrNull(componentId) ?: throw adaptyError(
            null,
            "AdaptyUIError: component not found ($componentId)",
            AdaptyErrorCode.DECODING_FAILED,
        )
    }

    protected fun <T : Component> getComponentOrNull(componentId: String): T? {
        val style = getDefaultStyleOrError()
        return style.items[componentId] as? T
    }

    fun <T : Asset> getAsset(assetId: String): T {
        return viewConfig.getAsset(assetId) ?: throw adaptyError(
            null,
            "AdaptyUIError: asset not found ($assetId)",
            AdaptyErrorCode.DECODING_FAILED,
        )
    }

    fun getString(strId: String): String? =
        viewConfig.getString(strId)

    fun isHardPaywall(): Boolean = viewConfig.isHard

    fun getFeatures(): Features? {
        val style = getDefaultStyleOrError()
        return style.featureBlock?.let { featureBlock ->
            when (featureBlock.type) {
                AdaptyViewConfiguration.FeatureBlock.Type.LIST -> {
                    featureBlock.orderedItems.filterIsInstance<Component.Text>().getOrNull(0)
                        ?.let { Features.List(it)}
                }
                AdaptyViewConfiguration.FeatureBlock.Type.TIMELINE -> {
                    val timelineEntries = featureBlock.orderedItems.filterIsInstance<Component.CustomObject>().map { rawTimelineEntry ->
                        TimelineEntry.from(rawTimelineEntry.properties.toMap())
                    }
                    Features.TimeLine(timelineEntries)
                }
            }
        }
    }

    fun getProducts(): Products {
        val productBlock = getDefaultStyleOrError().productBlock

        val blockType = when (productBlock.type) {
            ProductBlock.Type.SINGLE -> Products.BlockType.Single
            ProductBlock.Type.VERTICAL -> Products.BlockType.Vertical
            ProductBlock.Type.HORIZONTAL -> Products.BlockType.Horizontal
        }

        val products = productBlock.orderedItems.filterIsInstance<Component.CustomObject>()
            .mapIndexed { i, rawProductInfo ->
                ProductInfo.from(rawProductInfo.properties.toMap(), i == productBlock.mainProductIndex)
            }
            .withProductLayoutOrdering(this, blockType)

        return Products(products, blockType)
    }

    fun getFooterButtons(): List<Component.Button> {
        val style = getDefaultStyleOrError()
        return style.footerBlock?.orderedItems?.filterIsInstance<Component.Button>().orEmpty()
    }

    private fun getDefaultStyleOrError(): AdaptyViewConfiguration.Style {
        return viewConfig.getStyle(STYLE_KEY_DEFAULT) ?: throw adaptyError(
            null,
            "AdaptyUIError: style not found ($STYLE_KEY_DEFAULT)",
            AdaptyErrorCode.DECODING_FAILED,
        )
    }

    companion object {
        fun from(viewConfig: AdaptyViewConfiguration): TemplateConfig {
            return when (viewConfig.templateId) {
                CONFIG_KEY_TEMPLATE_BASIC -> BasicTemplateConfig(viewConfig)
                CONFIG_KEY_TEMPLATE_TRANSPARENT -> TransparentTemplateConfig(viewConfig)
                CONFIG_KEY_TEMPLATE_FLAT -> FlatTemplateConfig(viewConfig)
                else -> throw adaptyError(
                    null,
                    "AdaptyUIError: unsupported template (${viewConfig.templateId})",
                    AdaptyErrorCode.UNSUPPORTED_DATA,
                )
            }
        }
    }

    enum class RenderDirection {
        TOP_TO_BOTTOM, BOTTOM_TO_TOP
    }
}
