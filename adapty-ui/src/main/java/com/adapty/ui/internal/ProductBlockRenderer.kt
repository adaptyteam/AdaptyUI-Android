@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal

import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.annotation.RestrictTo
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.constraintlayout.widget.ConstraintSet.BOTTOM
import androidx.constraintlayout.widget.ConstraintSet.END
import androidx.constraintlayout.widget.ConstraintSet.START
import androidx.constraintlayout.widget.ConstraintSet.TOP
import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.internal.utils.PriceFormatter
import com.adapty.internal.utils.adaptyError
import com.adapty.models.AdaptyPaywall
import com.adapty.models.AdaptyPaywallProduct
import com.adapty.ui.AdaptyUI
import com.adapty.ui.listeners.AdaptyUiTagResolver

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class ProductBlockRenderer(
    private val viewHelper: ViewHelper,
    private val layoutHelper: LayoutHelper,
    private val textComponentHelper: TextComponentHelper,
    private val priceFormatter: PriceFormatter,
) {

    fun render(
        templateConfig: TemplateConfig,
        paywall: AdaptyPaywall,
        actionListener: (AdaptyUI.ViewConfiguration.Component.Button.Action) -> Unit,
        parentView: ConstraintLayout,
        purchaseButton: TextView?,
        products: List<AdaptyPaywallProduct>?,
        verticalAnchor: ViewAnchor,
        paywallScreenProps: PaywallScreen.Props,
        edgeMargin: Int,
        constraintSet: ConstraintSet,
        tagResolver: AdaptyUiTagResolver,
        interactionListener: PaywallUiManager.InteractionListener,
        onTextViewHeightChangeOnResizeCallback: () -> Unit,
    ): List<ProductViewsBundle> {
        val productBlock = templateConfig.getProductBlock(paywall)

        val blockType = productBlock.blockType
        val productInfos = productBlock.paywallOrderedProducts
            .withProductLayoutOrdering(templateConfig, blockType)

        checkProductNumber(productInfos, paywall)

        val context = parentView.context

        val productViewsBundles = productInfos.map { productInfo ->
            viewHelper.createProductViewsBundle(
                context,
                productInfo,
                blockType,
                templateConfig,
                tagResolver,
                actionListener,
                onTextViewHeightChangeOnResizeCallback,
            )
        }
            .takeIf { it.isNotEmpty() }
            ?.onEach { viewsBundle ->
                viewsBundle.productCell?.let(parentView::addView)
                viewsBundle.productTitle?.let(parentView::addView)
                viewsBundle.productSubtitle?.let(parentView::addView)
                viewsBundle.productSecondTitle?.let(parentView::addView)
                viewsBundle.productSecondSubtitle?.let(parentView::addView)
                viewsBundle.productTag?.let(parentView::addView)
            }

            return productViewsBundles?.also { productViewsBundles ->
                val fromTopToBottom = templateConfig.renderDirection == TemplateConfig.RenderDirection.TOP_TO_BOTTOM

                val productCells = productViewsBundles.mapNotNull { it.productCell }

                if (productCells.isNotEmpty()) {
                    renderMultiple(
                        context,
                        productCells,
                        blockType as Products.BlockType.Multiple,
                        verticalAnchor,
                        edgeMargin,
                        fromTopToBottom,
                        constraintSet,
                        productViewsBundles,
                    )
                } else {
                    renderSingle(
                        productViewsBundles,
                        parentView,
                        edgeMargin,
                        verticalAnchor,
                        fromTopToBottom,
                        constraintSet,
                    )
                }

                if (!products.isNullOrEmpty()) {
                    fillInnerProductTexts(
                        products,
                        paywallScreenProps,
                        productViewsBundles,
                        productBlock,
                        purchaseButton,
                        templateConfig,
                        tagResolver,
                        interactionListener,
                    )
                }
            }.orEmpty()
    }

    fun fillInnerProductTexts(
        products: List<AdaptyPaywallProduct>,
        paywallScreen: PaywallScreen,
        productBlock: Products,
        purchaseButton: TextView?,
        templateConfig: TemplateConfig,
        tagResolver: AdaptyUiTagResolver,
        interactionListener: PaywallUiManager.InteractionListener,
    ) {
        fillInnerProductTexts(
            products,
            paywallScreen.props,
            paywallScreen.productViewsBundles,
            productBlock,
            purchaseButton,
            templateConfig,
            tagResolver,
            interactionListener
        )
    }

    private fun fillInnerProductTexts(
        products: List<AdaptyPaywallProduct>,
        paywallScreenProps: PaywallScreen.Props,
        productViewsBundles: List<ProductViewsBundle>,
        productBlock: Products,
        purchaseButton: TextView?,
        templateConfig: TemplateConfig,
        tagResolver: AdaptyUiTagResolver,
        interactionListener: PaywallUiManager.InteractionListener,
    ) {
        val products = products.withProductLayoutOrdering(templateConfig, productBlock.blockType)
        val productInfos = productBlock.paywallOrderedProducts
            .withProductLayoutOrdering(templateConfig, productBlock.blockType)

        productViewsBundles.forEachIndexed { i, productViewsBundle ->
            val product = products.getOrNull(i)
            val productInfo = productInfos.getOrNull(i)
            val initiatePurchaseOnTap = productBlock.initiatePurchaseOnTap
            val productCell = productViewsBundle.productCell
            if (product != null && productInfo != null) {
                if (productCell != null) {
                    updateProductCellWithProduct(
                        productCell,
                        purchaseButton,
                        productViewsBundle.productTag,
                        productViewsBundles,
                        product,
                        templateConfig,
                        tagResolver,
                        interactionListener,
                        productInfo.isMain,
                        initiatePurchaseOnTap,
                    )
                } else {
                    interactionListener.onProductSelected(product)

                    if (purchaseButton != null) {
                        val purchaseButtonFreeTrialTextProperties =
                            templateConfig.getPurchaseButtonOfferTitle()?.let { tc ->
                                textComponentHelper.processTextComponent(
                                    purchaseButton.context,
                                    tc,
                                    templateConfig,
                                    tagResolver,
                                )
                            }

                        if (purchaseButtonFreeTrialTextProperties != null && product.hasFreeTrial()) {
                            viewHelper.applyTextProperties(
                                purchaseButton,
                                purchaseButtonFreeTrialTextProperties,
                            )
                        }
                    }
                }

                val productPlaceholders = ProductPlaceholderContentData.create(product, priceFormatter)

                if (productViewsBundle.productTitle != null)
                    fillInnerText(
                        productViewsBundle.productTitle,
                        productInfo.title,
                        templateConfig,
                        tagResolver,
                        productPlaceholders,
                    )

                if (productViewsBundle.productSubtitle != null)
                    fillInnerText(
                        productViewsBundle.productSubtitle,
                        productInfo.getSubtitle(product),
                        templateConfig,
                        tagResolver,
                        productPlaceholders,
                    )

                if (productViewsBundle.productSecondTitle != null)
                    fillInnerText(
                        productViewsBundle.productSecondTitle,
                        productInfo.secondTitle,
                        templateConfig,
                        tagResolver,
                        productPlaceholders,
                    )

                if (productViewsBundle.productSecondSubtitle != null)
                    fillInnerText(
                        productViewsBundle.productSecondSubtitle,
                        productInfo.secondSubtitle,
                        templateConfig,
                        tagResolver,
                        productPlaceholders,
                    )
            } else {
                productCell?.post {
                    productCell.visibility = View.GONE
                    paywallScreenProps.contentSizeChangeConsumed = false
                }
            }
        }
    }

    private fun fillInnerText(
        textView: TextView,
        textComponent: AdaptyUI.ViewConfiguration.Component.Text?,
        templateConfig: TemplateConfig,
        tagResolver: AdaptyUiTagResolver,
        productPlaceholders: List<ProductPlaceholderContentData>,
    ) {
        if (textComponent == null) return

        val textProperties = textComponentHelper.processTextComponent(
            textView.context,
            textComponent,
            templateConfig,
            tagResolver,
            productPlaceholders,
        )

        viewHelper.applyTextProperties(
            textView,
            textProperties,
        )
    }

    private fun updateProductCellWithProduct(
        productCell: View,
        purchaseButton: TextView?,
        productTag: View?,
        productViewsBundles: List<ProductViewsBundle>,
        product: AdaptyPaywallProduct,
        templateConfig: TemplateConfig,
        tagResolver: AdaptyUiTagResolver,
        interactionListener: PaywallUiManager.InteractionListener,
        isMainProduct: Boolean,
        initiatePurchaseOnTap: Boolean,
    ) {
        val purchaseButtonFreeTrialTextProperties =
            templateConfig.getPurchaseButtonOfferTitle()?.let { tc ->
                textComponentHelper.processTextComponent(productCell.context, tc, templateConfig, tagResolver)
            }
        val purchaseButtonRegularTextProperties = purchaseButtonFreeTrialTextProperties?.let {
            templateConfig.getPurchaseButton().title?.let { tc ->
                textComponentHelper.processTextComponent(productCell.context, tc, templateConfig, tagResolver)
            }
        }

        productCell.setOnClickListener {
            if (!productCell.isSelected) {
                productViewsBundles.forEach { bundle -> bundle.productCell?.isSelected = false }
                interactionListener.onProductSelected(product)
                productCell.isSelected = true

                if (purchaseButton != null && purchaseButtonFreeTrialTextProperties != null && purchaseButtonRegularTextProperties != null) {
                    viewHelper.applyTextProperties(
                        purchaseButton,
                        if (product.hasFreeTrial()) purchaseButtonFreeTrialTextProperties else purchaseButtonRegularTextProperties,
                    )
                }
            }
            if (initiatePurchaseOnTap)
                interactionListener.onPurchaseButtonClicked()
        }

        productTag?.post { productTag.visibility = View.VISIBLE }

        if (isMainProduct) {
            interactionListener.onProductSelected(product)
            productCell.isSelected = true

            if (purchaseButton != null && purchaseButtonFreeTrialTextProperties != null && product.hasFreeTrial()) {
                viewHelper.applyTextProperties(
                    purchaseButton,
                    purchaseButtonFreeTrialTextProperties,
                )
            }
        }
    }

    private fun renderSingle(
        productViewsBundles: List<ProductViewsBundle>,
        parentView: ConstraintLayout,
        edgeMargin: Int,
        verticalAnchor: ViewAnchor,
        fromTopToBottom: Boolean,
        constraintSet: ConstraintSet,
    ) {
        val context = parentView.context
        val productInnerTextsVerticalSpacingPx =
            PRODUCT_INNER_TEXTS_VERTICAL_SPACING_DP.dp(context).toInt()

        productViewsBundles.getOrNull(0)?.let { bundle ->
            val anchors = listOf(
                ViewAnchor(parentView, START, START, edgeMargin),
                ViewAnchor(parentView, END, END, edgeMargin),
                verticalAnchor,
            )

            val innerProductTexts = if (fromTopToBottom) {
                listOfNotNull(
                    bundle.productTitle,
                    bundle.productSubtitle,
                    bundle.productSecondTitle,
                )
            } else {
                listOfNotNull(
                    bundle.productSecondTitle,
                    bundle.productSubtitle,
                    bundle.productTitle,
                )
            }

            innerProductTexts.forEachIndexed { i, view ->
                layoutHelper.constrainInnerProductText(view.id, anchors, constraintSet)

                verticalAnchor.update(view, verticalAnchor.side, productInnerTextsVerticalSpacingPx)
            }
        }
    }

    private fun renderMultiple(
        context: Context,
        productCells: List<View>,
        blockType: Products.BlockType.Multiple,
        verticalAnchor: ViewAnchor,
        edgeMargin: Int,
        fromTopToBottom: Boolean,
        constraintSet: ConstraintSet,
        productViewsBundles: List<ProductViewsBundle>,
    ) {
        val productCellHorizontalPaddingPx =
            PRODUCT_CELL_HORIZONTAL_PADDING_DP.dp(context).toInt()

        val productCellVerticalPaddingPx =
            PRODUCT_CELL_VERTICAL_PADDING_DP.dp(context).toInt()

        val productInnerTextsHorizontalSpacingPx =
            PRODUCT_INNER_TEXTS_HORIZONTAL_SPACING_DP.dp(context).toInt()

        val productInnerTextsVerticalSpacingPx =
            PRODUCT_INNER_TEXTS_VERTICAL_SPACING_DP.dp(context).toInt()

        layoutHelper.constrainProductCells(
            context,
            productCells,
            blockType,
            verticalAnchor,
            edgeMargin,
            fromTopToBottom,
            constraintSet,
        )

        productCells.forEachIndexed { i, cell ->
            val bundle = productViewsBundles.getOrNull(i)

            val productTitle = bundle?.productTitle
            val productSubtitle = bundle?.productSubtitle
            val productSecondTitle = bundle?.productSecondTitle
            val productSecondSubtitle = bundle?.productSecondSubtitle
            val productTag = bundle?.productTag

            if (productTitle != null) {
                val anchors = when (blockType) {
                    Products.BlockType.Vertical -> {
                        listOf(
                            ViewAnchor(cell, TOP, TOP, productCellVerticalPaddingPx),
                            ViewAnchor(cell, START, START, productCellHorizontalPaddingPx),
                            productSecondTitle?.let { secondTitle ->
                                ViewAnchor(secondTitle, START, END, productInnerTextsHorizontalSpacingPx)
                            } ?: kotlin.run {
                                ViewAnchor(cell, END, END, productCellHorizontalPaddingPx)
                            }
                        )
                    }

                    Products.BlockType.Horizontal -> {
                        listOf(
                            ViewAnchor(cell, TOP, TOP, productCellVerticalPaddingPx),
                            ViewAnchor(cell, START, START, productCellHorizontalPaddingPx),
                            ViewAnchor(cell, END, END, productCellHorizontalPaddingPx),
                        )
                    }
                }

                layoutHelper.constrainInnerProductText(productTitle.id, anchors, constraintSet)
            }

            if (productSubtitle != null) {
                val anchors = when (blockType) {
                    Products.BlockType.Vertical -> {
                        listOf(
                            ViewAnchor(cell, BOTTOM, BOTTOM, productCellVerticalPaddingPx),
                            ViewAnchor(cell, START, START, productCellHorizontalPaddingPx),
                            productSecondSubtitle?.let { secondSubtitle ->
                                ViewAnchor(secondSubtitle, START, END, productInnerTextsHorizontalSpacingPx)
                            } ?: kotlin.run {
                                ViewAnchor(cell, END, END, productCellHorizontalPaddingPx)
                            }
                        )
                    }

                    Products.BlockType.Horizontal -> {
                        listOf(
                            productTitle?.let { title ->
                                ViewAnchor(title, BOTTOM, TOP, productInnerTextsVerticalSpacingPx)
                            } ?: kotlin.run {
                                ViewAnchor(cell, TOP, TOP, productCellVerticalPaddingPx)
                            },
                            ViewAnchor(cell, START, START, productCellHorizontalPaddingPx),
                            ViewAnchor(cell, END, END, productCellHorizontalPaddingPx),
                        )
                    }
                }

                layoutHelper.constrainInnerProductText(productSubtitle.id, anchors, constraintSet)
            }

            if (productSecondTitle != null) {
                val anchors = when (blockType) {
                    Products.BlockType.Vertical -> {
                        listOf(
                            ViewAnchor(cell, TOP, TOP, productCellVerticalPaddingPx),
                            ViewAnchor(cell, END, END, productCellHorizontalPaddingPx),
                        )
                    }

                    Products.BlockType.Horizontal -> {
                        listOf(
                            productSecondSubtitle?.let { secondSubtitle ->
                                ViewAnchor(secondSubtitle, TOP, BOTTOM, productInnerTextsVerticalSpacingPx)
                            } ?: kotlin.run {
                                ViewAnchor(cell, BOTTOM, BOTTOM, productCellVerticalPaddingPx)
                            },
                            ViewAnchor(cell, START, START, productCellHorizontalPaddingPx),
                            ViewAnchor(cell, END, END, productCellHorizontalPaddingPx),
                        )
                    }
                }

                layoutHelper.constrainInnerProductText(productSecondTitle.id, anchors, constraintSet)
            }

            if (productSecondSubtitle != null) {
                val anchors = when (blockType) {
                    Products.BlockType.Vertical -> {
                        listOf(
                            ViewAnchor(cell, BOTTOM, BOTTOM, productCellVerticalPaddingPx),
                            ViewAnchor(cell, END, END, productCellHorizontalPaddingPx),
                        )
                    }

                    Products.BlockType.Horizontal -> {
                        listOf(
                            ViewAnchor(cell, BOTTOM, BOTTOM, productCellVerticalPaddingPx),
                            ViewAnchor(cell, START, START, productCellHorizontalPaddingPx),
                            ViewAnchor(cell, END, END, productCellHorizontalPaddingPx),
                        )
                    }
                }

                layoutHelper.constrainInnerProductText(productSecondSubtitle.id, anchors, constraintSet)
            }

            if (productTag != null) {
                layoutHelper.constrainMainProductTag(productTag, cell.id, blockType, constraintSet)
                productTag.post { productTag.visibility = View.INVISIBLE }
            }
        }
    }

    private fun checkProductNumber(productInfos: List<ProductInfo>, paywall: AdaptyPaywall) {
        if (paywall.vendorProductIds.size > productInfos.size) throw adaptyError(
            null,
            "AdaptyUIError: product number in paywall (${paywall.vendorProductIds.size}) should not exceed the one in viewConfig (${productInfos.size})",
            AdaptyErrorCode.WRONG_PARAMETER,
        )
    }
}