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
import com.adapty.internal.utils.adaptyError
import com.adapty.models.AdaptyPaywall
import com.adapty.models.AdaptyPaywallProduct
import com.adapty.models.AdaptyViewConfiguration
import java.text.Format

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class ProductBlockRenderer(
    private val viewHelper: ViewHelper,
    private val layoutHelper: LayoutHelper,
    private val textComponentHelper: TextComponentHelper,
    private val numberFormat: Format,
) {

    fun render(
        templateConfig: TemplateConfig,
        paywall: AdaptyPaywall,
        actionListener: (AdaptyViewConfiguration.Component.Button.Action) -> Unit,
        parentView: ConstraintLayout,
        purchaseButton: TextView?,
        products: List<AdaptyPaywallProduct>?,
        verticalAnchor: ViewAnchor,
        paywallScreenProps: PaywallScreen.Props,
        edgeMargin: Int,
        constraintSet: ConstraintSet,
        interactionListener: PaywallUiManager.InteractionListener,
        onTextViewHeightChangeOnResizeCallback: () -> Unit,
    ): List<ProductViewsBundle> {
        val productBlock = templateConfig.getProducts()

        val productInfos = productBlock.products
        val blockType = productBlock.blockType

        checkProductNumber(productInfos, paywall)

        val isReverseAddingOrder = templateConfig.isReverseProductAddingOrder(productBlock)

        val context = parentView.context

        val productViewsBundles = productInfos.map { productInfo ->
            viewHelper.createProductViewsBundle(
                context,
                productInfo,
                blockType,
                templateConfig,
                actionListener,
                onTextViewHeightChangeOnResizeCallback,
            )
        }
            .takeIf { it.isNotEmpty() }
            ?.let { list -> if (isReverseAddingOrder) list.reversed() else list }
            ?.onEach { viewsBundle ->
                viewsBundle.productCell?.let(parentView::addView)
                viewsBundle.productTitle?.let(parentView::addView)
                viewsBundle.productSubtitle?.let(parentView::addView)
                viewsBundle.productSecondTitle?.let(parentView::addView)
                viewsBundle.productSecondSubtitle?.let(parentView::addView)
                viewsBundle.mainProductTag?.let(parentView::addView)
            }

            return productViewsBundles?.also { productViewsBundles ->
                val productInfos = if (isReverseAddingOrder) productInfos.reversed() else productInfos

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
                        productInfos,
                        purchaseButton,
                        templateConfig,
                        isReverseAddingOrder,
                        interactionListener,
                    )
                }
            }.orEmpty()
    }

    fun fillInnerProductTexts(
        products: List<AdaptyPaywallProduct>,
        paywallScreen: PaywallScreen,
        productInfos: List<ProductInfo>,
        purchaseButton: TextView?,
        templateConfig: TemplateConfig,
        isReverseAddingOrder: Boolean,
        interactionListener: PaywallUiManager.InteractionListener,
    ) {
        fillInnerProductTexts(
            products,
            paywallScreen.props,
            paywallScreen.productViewsBundles,
            productInfos,
            purchaseButton,
            templateConfig,
            isReverseAddingOrder,
            interactionListener
        )
    }

    private fun fillInnerProductTexts(
        products: List<AdaptyPaywallProduct>,
        paywallScreenProps: PaywallScreen.Props,
        productViewsBundles: List<ProductViewsBundle>,
        productInfos: List<ProductInfo>,
        purchaseButton: TextView?,
        templateConfig: TemplateConfig,
        isReverseAddingOrder: Boolean,
        interactionListener: PaywallUiManager.InteractionListener,
    ) {
        val products = if (isReverseAddingOrder) products.reversed() else products

        productViewsBundles.forEachIndexed { i, productViewsBundle ->
            val product = products.getOrNull(i)
            val productInfo = productInfos.getOrNull(i)
            val productCell = productViewsBundle.productCell
            if (product != null && productInfo != null) {
                if (productCell != null) {
                    updateProductCellWithProduct(
                        productCell,
                        purchaseButton,
                        productViewsBundle.mainProductTag,
                        productViewsBundles,
                        product,
                        templateConfig,
                        interactionListener,
                        productInfo.isMain,
                    )
                } else {
                    interactionListener.onProductSelected(product)

                    if (purchaseButton != null) {
                        val purchaseButtonFreeTrialTextProperties =
                            templateConfig.getPurchaseButtonOfferTitle()?.let { tc ->
                                textComponentHelper.processTextComponent(
                                    purchaseButton.context,
                                    tc,
                                    templateConfig
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

                val productPlaceholders = ProductPlaceholderContentData.create(product, numberFormat)

                if (productViewsBundle.productTitle != null)
                    fillInnerText(
                        productViewsBundle.productTitle,
                        productInfo.title,
                        templateConfig,
                        productPlaceholders,
                    )

                if (productViewsBundle.productSubtitle != null)
                    fillInnerText(
                        productViewsBundle.productSubtitle,
                        productInfo.getSubtitle(product),
                        templateConfig,
                        productPlaceholders,
                    )

                if (productViewsBundle.productSecondTitle != null)
                    fillInnerText(
                        productViewsBundle.productSecondTitle,
                        productInfo.secondTitle,
                        templateConfig,
                        productPlaceholders,
                    )

                if (productViewsBundle.productSecondSubtitle != null)
                    fillInnerText(
                        productViewsBundle.productSecondSubtitle,
                        productInfo.secondSubtitle,
                        templateConfig,
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
        textComponent: AdaptyViewConfiguration.Component.Text?,
        templateConfig: TemplateConfig,
        productPlaceholders: List<ProductPlaceholderContentData>,
    ) {
        if (textComponent == null) return

        val textProperties = textComponentHelper.processTextComponent(
            textView.context,
            textComponent,
            templateConfig,
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
        mainProductTag: View?,
        productViewsBundles: List<ProductViewsBundle>,
        product: AdaptyPaywallProduct,
        templateConfig: TemplateConfig,
        interactionListener: PaywallUiManager.InteractionListener,
        isMainProduct: Boolean,
    ) {
        val purchaseButtonFreeTrialTextProperties =
            templateConfig.getPurchaseButtonOfferTitle()?.let { tc ->
                textComponentHelper.processTextComponent(productCell.context, tc, templateConfig)
            }
        val purchaseButtonRegularTextProperties = purchaseButtonFreeTrialTextProperties?.let {
            templateConfig.getPurchaseButton().title?.let { tc ->
                textComponentHelper.processTextComponent(productCell.context, tc, templateConfig)
            }
        }

        productCell.setOnClickListener {
            if (productCell.isSelected) return@setOnClickListener
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

        if (isMainProduct) {
            interactionListener.onProductSelected(product)
            productCell.isSelected = true

            mainProductTag?.post { mainProductTag.visibility = View.VISIBLE }

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
            val mainProductTag = bundle?.mainProductTag

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

            if (mainProductTag != null) {
                layoutHelper.constrainMainProductTag(mainProductTag, cell.id, blockType, constraintSet)
                mainProductTag.post { mainProductTag.visibility = View.INVISIBLE }
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