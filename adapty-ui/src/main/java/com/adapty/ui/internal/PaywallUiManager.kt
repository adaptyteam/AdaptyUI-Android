package com.adapty.ui.internal

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.text.TextUtils.TruncateAt
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.view.ViewOutlineProvider
import android.view.ViewTreeObserver.OnPreDrawListener
import android.widget.FrameLayout
import android.widget.ImageView.ScaleType
import android.widget.TextView
import androidx.annotation.RestrictTo
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.constraintlayout.widget.ConstraintSet.*
import com.adapty.models.AdaptyPaywall
import com.adapty.models.AdaptyPaywallProduct
import com.adapty.models.AdaptyViewConfiguration
import com.adapty.models.AdaptyViewConfiguration.Asset
import com.adapty.models.AdaptyViewConfiguration.Component
import com.adapty.ui.AdaptyPaywallInsets
import com.adapty.ui.AdaptyPaywallView
import com.adapty.ui.R
import com.adapty.utils.AdaptyLogLevel.Companion.VERBOSE
import com.adapty.utils.AdaptyLogLevel.Companion.WARN
import kotlin.math.min

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class PaywallUiManager(
    private val flowKey: String,
    private val configDataProvider: ViewConfigDataProvider,
    private val drawableHelper: DrawableHelper,
    private val textHelper: TextHelper,
) {

    fun onSizeChanged(view: View, w: Int, h: Int) {
        paywallViewSizeChangeConsumed = false
        val padding = ((min(w, h) - LOADING_INDICATOR_WIDTH_DP.dp(view.context)) / 2).toInt()
        loadingView?.setPadding(padding, padding, padding, padding)
    }

    private val productCellViews = mutableListOf<View>()
    private var loadingView: View? = null
    private var contentContainer: ConstraintLayout? = null

    private var paywallViewSizeChangeConsumed = false
    private var contentSizeChangeConsumed = false

    fun buildLayout(
        paywallView: AdaptyPaywallView,
        viewConfig: AdaptyViewConfiguration,
        paywall: AdaptyPaywall,
        products: List<AdaptyPaywallProduct>?,
        insets: AdaptyPaywallInsets,
        interactionListener: InteractionListener,
    ) {
        val (screenWidth, screenHeight) = (paywallView.context as Activity).windowManager.getScreenSize()

        val backgroundImageHeight = computeBackgroundImageHeight(paywallView.context, screenHeight)

        paywallView.content {
            imageView {
                id = View.generateViewId()

                layoutParams = LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    backgroundImageHeight,
                )
                imageBitmap = configDataProvider.getAsset<Asset.Image>(viewConfig, COMPONENT_KEY_COVER_IMAGE_0)
                    .getBitmap(screenWidth)
                scaleType = ScaleType.CENTER_CROP
            }

            scrollView {
                id = View.generateViewId()

                layoutParams = LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    LayoutParams.WRAP_CONTENT,
                )

                isVerticalScrollBarEnabled = false
                overScrollMode = View.OVER_SCROLL_NEVER

                constraintLayout {
                    id = View.generateViewId()
                    layoutParams = LayoutParams(
                        LayoutParams.MATCH_PARENT,
                        LayoutParams.WRAP_CONTENT,
                    )

                    val cornerRadiusPx = CONTENT_BG_CORNER_RADIUS_DP.dp(context)
                    val contentMarginTopPx = backgroundImageHeight - cornerRadiusPx.toInt()

                    val contentBgView: View

                    view {
                        id = View.generateViewId()
                        background =
                            drawableHelper.createContentBackgroundDrawable(
                                configDataProvider.getAssetForComponent<Asset.Color>(
                                    viewConfig,
                                    COMPONENT_KEY_BG_COLOR,
                                ).value,
                                cornerRadiusPx,
                            )
                    }.also { contentBgView = it }

                    val titleTextViewId: Int

                    textView {
                        id = View.generateViewId()

                        textSize = 24f
                        includeFontPadding = false
                        gravity = Gravity.CENTER_HORIZONTAL
                        maxLines = 2
                        ellipsize = TruncateAt.END

                        applyTextComponentAttrs(this, viewConfig, COMPONENT_KEY_TITLE)
                        resizeTextOnPreDrawIfNeeded(retainOriginalHeight = false)
                    }.also { titleTextViewId = it.id }

                    val featureCollection =
                        configDataProvider.getComponent<Component.TextCollection>(
                            viewConfig,
                            COMPONENT_KEY_FEATURES,
                        )
                    val startDrawable = drawableHelper.getTickDrawable(
                        context,
                        configDataProvider.getAssetForComponent<Asset.Color>(
                            viewConfig,
                            COMPONENT_KEY_ACCENT_COLOR_1,
                        ).value,
                    )

                    val featureViewIds = mutableListOf<Int>()

                    for (text in featureCollection.texts) {
                        if (configDataProvider.getString(viewConfig, text.stringId) == null) continue

                        textView {
                            id = View.generateViewId()
                            setCompoundDrawablesWithIntrinsicBounds(startDrawable, null, null, null)
                            compoundDrawablePadding =
                                FEATURE_DRAWABLE_PADDING_DP.dp(context).toInt()
                            textSize = 14f
                            includeFontPadding = false
                            applyTextComponentAttrs(this, viewConfig, text)
                        }.also { featureViewIds.add(it.id) }
                    }

                    val constraintSet = ConstraintSet()
                        .also { it.clone(this) }

                    val productsSize =
                        products?.takeIf { it.isNotEmpty() }?.size ?: paywall.vendorProductIds.size
                    for (i in 0 until productsSize) {
                        val productCell: View

                        view {
                            id = View.generateViewId()
                            background = drawableHelper.createProductCellBackgroundDrawable(
                                PRODUCT_CELL_SELECTED_STROKE_WIDTH_DP.dp(context).toInt(),
                                configDataProvider.getAssetForComponent<Asset.Color>(
                                    viewConfig,
                                    COMPONENT_KEY_ACCENT_COLOR_0,
                                ).value,
                                configDataProvider.getAssetForComponent<Asset.Color>(
                                    viewConfig,
                                    COMPONENT_KEY_PRODUCT_BG_COLOR,
                                ).value,
                                PRODUCT_CELL_CORNER_RADIUS_DP.dp(context),
                            )
                        }.also {
                            productCellViews.add(it)
                            productCell = it
                        }

                        products?.getOrNull(i)?.let { product ->
                            fillCellWithProduct(
                                productCell,
                                viewConfig,
                                product,
                                i,
                                constraintSet,
                                interactionListener
                            )
                        }
                    }
                    constraintSet.applyTo(this)

                    val purchaseButtonId: Int
                    textView {
                        id = View.generateViewId()
                        gravity = Gravity.CENTER
                        textSize = 18f
                        includeFontPadding = false
                        applyTextComponentAttrs(
                            this,
                            viewConfig,
                            COMPONENT_KEY_PURCHASE_BUTTON_TEXT,
                        )

                        background =
                            drawableHelper.createPurchaseButtonBackgroundDrawable(
                                PURCHASE_BUTTON_CORNER_RADIUS_DP.dp(context),
                                configDataProvider.getAssetForComponent<Asset.Color>(
                                    viewConfig,
                                    COMPONENT_KEY_ACCENT_COLOR_0,
                                ).value,
                            )

                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                setForegroundFromAttr(android.R.attr.selectableItemBackground)
                            }
                            clipToOutline = true
                            outlineProvider = ViewOutlineProvider.BACKGROUND
                        } catch (e: Throwable) {}

                        setOnClickListener { interactionListener.onPurchaseButtonClicked() }
                    }.also { purchaseButtonId = it.id }

                    val serviceButtons = mutableListOf<View>()

                    serviceButton {
                        applyTextComponentAttrs(this, viewConfig, COMPONENT_KEY_TERMS_BUTTON)

                        setOnClickListener {
                            configDataProvider.getTermsUrl(viewConfig)?.let { url ->
                                log(VERBOSE) { "$LOG_PREFIX $flowKey onTerms tap" }
                                interactionListener.onUrlClicked(url)
                            }
                        }
                    }.also { serviceButtons.add(it) }

                    serviceButton {
                        applyTextComponentAttrs(this, viewConfig, COMPONENT_KEY_PRIVACY_BUTTON)

                        setOnClickListener {
                            configDataProvider.getPrivacyUrl(viewConfig)?.let { url ->
                                log(VERBOSE) { "$LOG_PREFIX $flowKey onPrivacy tap" }
                                interactionListener.onUrlClicked(url)
                            }
                        }
                    }.also { serviceButtons.add(it) }

                    serviceButton {
                        applyTextComponentAttrs(this, viewConfig, COMPONENT_KEY_RESTORE_BUTTON)

                        setOnClickListener {
                            interactionListener.onRestoreButtonClicked()
                        }
                    }.also { serviceButtons.add(it) }

                    viewTreeObserver.addOnPreDrawListener(object : OnPreDrawListener {
                        override fun onPreDraw(): Boolean {
                            if (!(paywallViewSizeChangeConsumed && contentSizeChangeConsumed) && height > 0) {
                                val contentBgViewExpectedTop = contentBgView.topCoord
                                val contentBgViewExpectedBottom =
                                    (serviceButtons[0].bottomCoord + CONTENT_BOTTOM_MARGIN_DP.dp(context).toInt() + insets.bottom)
                                        .coerceAtLeast(paywallView.bottomCoord)
                                contentBgView.layoutParams = contentBgView.layoutParams.apply {
                                    height = contentBgViewExpectedBottom - contentBgViewExpectedTop
                                }
                                paywallViewSizeChangeConsumed = true
                                contentSizeChangeConsumed = true
                            }
                            return true
                        }
                    })

                    applyConstraints {
                        contentBgView.id constrainWidth MATCH_CONSTRAINT constrainHeight WRAP_CONTENT
                        contentBgView.id horizontallyTo PARENT_ID
                        TOP of contentBgView.id to TOP of PARENT_ID margin contentMarginTopPx

                        val edgeMargin = CONTENT_HORIZONTAL_EDGE_MARGIN_DP.dp(context).toInt()

                        titleTextViewId constrainWidth MATCH_CONSTRAINT constrainHeight WRAP_CONTENT
                        titleTextViewId horizontallyTo PARENT_ID margins edgeMargin
                        TOP of titleTextViewId to TOP of contentBgView.id margin cornerRadiusPx.toInt()


                        val featureMarginTop = TOPMOST_FEATURE_TOP_MARGIN_DP.dp(context).toInt()
                        val featureMarginBetween =
                            FEATURE_VERTICAL_MARGIN_BETWEEN_DP.dp(context).toInt()
                        featureViewIds.forEachIndexed { i, viewId ->
                            viewId constrainWidth MATCH_CONSTRAINT constrainHeight WRAP_CONTENT
                            viewId horizontallyTo PARENT_ID margins edgeMargin

                            val featureViewTopAnchor: Int
                            val featureViewMarginTop: Int
                            if (i == 0) {
                                featureViewTopAnchor = titleTextViewId
                                featureViewMarginTop = featureMarginTop
                            } else {
                                featureViewTopAnchor = featureViewIds[i - 1]
                                featureViewMarginTop = featureMarginBetween
                            }
                            TOP of viewId to BOTTOM of featureViewTopAnchor margin featureViewMarginTop
                        }

                        val productsMarginTop = TOPMOST_PRODUCT_TOP_MARGIN_DP.dp(context).toInt()
                        val productsMarginBetween =
                            PRODUCT_CELL_VERTICAL_MARGIN_BETWEEN_DP.dp(context).toInt()
                        val productCellHeight = PRODUCT_CELL_HEIGHT_DP.dp(context).toInt()
                        productCellViews.forEachIndexed { i, view ->
                            val viewId = view.id
                            viewId constrainWidth MATCH_CONSTRAINT constrainHeight productCellHeight
                            viewId horizontallyTo PARENT_ID margins edgeMargin

                            val productCellTopAnchor: Int
                            val productCellMarginTop: Int
                            if (i == 0) {
                                productCellTopAnchor =
                                    featureViewIds.lastOrNull() ?: titleTextViewId
                                productCellMarginTop = productsMarginTop
                            } else {
                                productCellTopAnchor = productCellViews[i - 1].id
                                productCellMarginTop = productsMarginBetween
                            }
                            TOP of viewId to BOTTOM of productCellTopAnchor margin productCellMarginTop
                        }

                        val purchaseButtonHeight = PURCHASE_BUTTON_HEIGHT_DP.dp(context).toInt()
                        val purchaseButtonMarginTop =
                            PURCHASE_BUTTON_TOP_MARGIN_DP.dp(context).toInt()
                        purchaseButtonId constrainWidth MATCH_CONSTRAINT constrainHeight purchaseButtonHeight
                        purchaseButtonId horizontallyTo PARENT_ID margins edgeMargin

                        val purchaseButtonTopAnchor =
                            productCellViews.lastOrNull()?.id ?: featureViewIds.lastOrNull() ?: titleTextViewId
                        TOP of purchaseButtonId to BOTTOM of purchaseButtonTopAnchor margin purchaseButtonMarginTop goneMargin purchaseButtonMarginTop

                        val serviceButtonMarginTop =
                            SERVICE_BUTTON_TOP_MARGIN_DP.dp(context).toInt()
                        serviceButtons.forEach { view ->
                            val viewId = view.id
                            viewId constrainWidth MATCH_CONSTRAINT constrainHeight WRAP_CONTENT
                            TOP of viewId to BOTTOM of purchaseButtonId margin serviceButtonMarginTop
                        }

                        val chainIds = serviceButtons.map { it.id }.toIntArray()
                        START of PARENT_ID chain spread with chainIds to END of PARENT_ID edgeMargins edgeMargin
                    }
                }.also { contentContainer = it }
            }

            if (!configDataProvider.isHardPaywall(viewConfig)) {
                val size = CLOSE_BUTTON_SIZE_DP.dp(context).toInt()

                imageView {
                    id = View.generateViewId()
                    layoutParams = FrameLayout.LayoutParams(size, size).apply {
                        marginStart = CLOSE_BUTTON_HORIZONTAL_MARGIN_DP.dp(context).toInt()
                        topMargin = CLOSE_BUTTON_TOP_MARGIN_DP.dp(context).toInt() + insets.top
                    }

                    setImageResource(R.drawable.adapty_cross)

                    setOnClickListener {
                        interactionListener.onCloseClicked()
                    }
                }
            }

            loadingView {
                id = View.generateViewId()
                layoutParams = LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    LayoutParams.MATCH_PARENT,
                )
                isIndeterminate = true
                isClickable = true
                setBackgroundColor(Color.parseColor(LOADING_BG_COLOR_HEX))
                visibility = View.GONE
            }.also { loadingView = it }
        }
    }

    fun onProductsLoaded(
        products: List<AdaptyPaywallProduct>,
        viewConfig: AdaptyViewConfiguration,
        interactionListener: InteractionListener,
    ) {
        val contentContainer = contentContainer ?: return
        val constraintSet = ConstraintSet().apply { clone(contentContainer) }
        productCellViews.forEachIndexed { i, productCell ->
            val product = products.getOrNull(i)
            if (product != null) {
                contentContainer.fillCellWithProduct(
                    productCell,
                    viewConfig,
                    product,
                    i,
                    constraintSet,
                    interactionListener,
                )
            } else {
                productCell.visibility = View.GONE
            }
        }
        constraintSet.applyTo(contentContainer)
    }

    fun clearOldPaywall() {
        productCellViews.clear()
        paywallViewSizeChangeConsumed = false
        contentSizeChangeConsumed = false
        loadingView = null
        contentContainer = null
    }

    fun toggleLoadingView(show: Boolean) {
        loadingView?.visibility = if (show) View.VISIBLE else View.GONE
    }

    @SuppressLint("SetTextI18n")
    private fun ViewGroup.fillCellWithProduct(
        productCell: View,
        viewConfig: AdaptyViewConfiguration,
        product: AdaptyPaywallProduct,
        productOrder: Int,
        constraintSet: ConstraintSet,
        interactionListener: InteractionListener,
    ) {
        productCell.setOnClickListener {
            if (productCell.isSelected) return@setOnClickListener
            productCellViews.forEach { view -> view.isSelected = false }
            interactionListener.onProductChosen(product)
            productCell.isSelected = true
        }

        val productTitleViewId: Int
        textView {
            id = View.generateViewId()
            textSize = 18f
            includeFontPadding = false
            setSingleLine()
            maxLines = 1
            ellipsize = TruncateAt.END
            gravity = Gravity.CENTER_VERTICAL
            setTextColor(
                configDataProvider.getAssetForComponent<Asset.Color>(
                    viewConfig,
                    COMPONENT_KEY_PRODUCT_TITLE,
                ).value
            )

            text = product.localizedTitle

            resizeTextOnPreDrawIfNeeded(retainOriginalHeight = true)
        }.also { productTitleViewId = it.id }

        val productPriceViewId: Int
        textView {
            id = View.generateViewId()
            textSize = 18f
            includeFontPadding = false
            setTextColor(
                configDataProvider.getAssetForComponent<Asset.Color>(
                    viewConfig,
                    COMPONENT_KEY_PRODUCT_TITLE,
                ).value
            )

            text = product.createTextFromLocalizedPrice(product.localizedPrice, paint)
        }.also { productPriceViewId = it.id }

        var productDiscountViewId: Int? = null
        if (product.hasDiscount()) {
            textView {
                id = View.generateViewId()
                includeFontPadding = false
                textSize = 14f
                setSingleLine()
                maxLines = 1
                ellipsize = TruncateAt.END
                gravity = Gravity.CENTER_VERTICAL
                setTextColor(
                    configDataProvider.getAssetForComponent<Asset.Color>(
                        viewConfig,
                        COMPONENT_KEY_PRODUCT_SECONDARY_TITLE,
                    ).value
                )

                text = product.createDiscountText(paint)

                resizeTextOnPreDrawIfNeeded(retainOriginalHeight = true)
            }.also { productDiscountViewId = it.id }
        }

        var productPricePerWeekViewId: Int? = null
        if (product.canConvertPriceToWeekly()) {
            textView {
                id = View.generateViewId()
                includeFontPadding = false
                textSize = 14f
                setTextColor(
                    configDataProvider.getAssetForComponent<Asset.Color>(
                        viewConfig,
                        COMPONENT_KEY_PRODUCT_PRICE_WEEK,
                    ).value
                )

                text = product.createPricePerWeekText(paint)
            }.also { productPricePerWeekViewId = it.id }
        }

        var mainProductTagViewId: Int? = null
        if (productOrder == 0) {
            interactionListener.onProductChosen(product)
            productCell.isSelected = true

            textView {
                id = View.generateViewId()
                includeFontPadding = false
                textSize = MAIN_PRODUCT_TAG_DEFAULT_TEXT_SIZE_SP
                gravity = Gravity.CENTER
                val horizontalPadding = MAIN_PRODUCT_TAG_HORIZONTAL_PADDING_DP.dp(context).toInt()
                setPadding(horizontalPadding, paddingTop, horizontalPadding, paddingBottom)

                background =
                    drawableHelper.createMainProductBackgroundDrawable(
                        MAIN_PRODUCT_TAG_CORNER_RADIUS_DP.dp(context),
                        configDataProvider.getAssetForComponent<Asset.Color>(
                            viewConfig,
                            COMPONENT_KEY_ACCENT_COLOR_0,
                        ).value,
                    )

                applyTextComponentAttrs(this, viewConfig, COMPONENT_KEY_MAIN_PRODUCT_TAG_TEXT)
            }.also { mainProductTagViewId = it.id }
        }

        addConstraints(constraintSet) {
            val productCellHorizontalPadding =
                PRODUCT_CELL_HORIZONTAL_PADDING_DP.dp(context).toInt()
            val productCellVerticalPadding = PRODUCT_CELL_VERTICAL_PADDING_DP.dp(context).toInt()

            productTitleViewId constrainWidth MATCH_CONSTRAINT constrainHeight WRAP_CONTENT
            START of productTitleViewId to START of productCell.id margin productCellHorizontalPadding
            END of productTitleViewId to START of productPriceViewId margin productCellHorizontalPadding
            TOP of productTitleViewId to TOP of productCell.id margin productCellVerticalPadding

            productPriceViewId constrainBoth WRAP_CONTENT
            END of productPriceViewId to END of productCell.id margin productCellHorizontalPadding
            TOP of productPriceViewId to TOP of productCell.id margin productCellVerticalPadding

            productDiscountViewId?.let { viewId ->
                val endConstraintViewId: Int
                val endConstraintSide: Int
                val marginEnd: Int
                val pricePerWeekViewId = productPricePerWeekViewId
                if (pricePerWeekViewId == null) {
                    endConstraintViewId = productCell.id
                    endConstraintSide = END
                    marginEnd = productCellHorizontalPadding
                } else {
                    endConstraintViewId = pricePerWeekViewId
                    endConstraintSide = START
                    marginEnd = 4f.dp(context).toInt()
                }

                viewId constrainWidth MATCH_CONSTRAINT constrainHeight WRAP_CONTENT
                START of viewId to START of productCell.id margin productCellHorizontalPadding
                END of viewId to endConstraintSide of endConstraintViewId margin marginEnd
                BOTTOM of viewId to BOTTOM of productCell.id margin productCellVerticalPadding
            }

            productPricePerWeekViewId?.let { viewId ->
                viewId constrainBoth WRAP_CONTENT
                END of viewId to END of productCell.id margin productCellHorizontalPadding
                BOTTOM of viewId to BOTTOM of productCell.id margin productCellVerticalPadding
            }

            mainProductTagViewId?.let { viewId ->
                val mainProductTagComponent = configDataProvider.getComponent<Component.Text>(
                    viewConfig,
                    COMPONENT_KEY_MAIN_PRODUCT_TAG_TEXT,
                )
                val fontSizeSp = mainProductTagComponent.size
                    ?: configDataProvider.getAsset<Asset.Font>(
                        viewConfig,
                        mainProductTagComponent.fontId,
                    ).size
                    ?: MAIN_PRODUCT_TAG_DEFAULT_TEXT_SIZE_SP
                val heightDp = fontSizeSp + MAIN_PRODUCT_TAG_VERTICAL_PADDING_DP * 2
                val heightPx = heightDp.dp(context).toInt()
                val marginEndPx = MAIN_PRODUCT_TAG_END_MARGIN_DP.dp(context).toInt()
                val marginBottomPx = (PRODUCT_CELL_HEIGHT_DP - heightDp / 2).dp(context).toInt()

                viewId constrainWidth WRAP_CONTENT constrainHeight heightPx
                END of viewId to END of productCell.id margin marginEndPx
                BOTTOM of viewId to BOTTOM of productCell.id margin marginBottomPx
            }
        }
    }

    private fun applyTextComponentAttrs(
        textView: TextView,
        viewConfig: AdaptyViewConfiguration,
        componentId: String,
    ) {
        applyTextComponentAttrs(
            textView,
            viewConfig,
            configDataProvider.getComponent(viewConfig, componentId),
        )
    }

    private fun applyTextComponentAttrs(
        textView: TextView,
        viewConfig: AdaptyViewConfiguration,
        text: Component.Text,
    ) {
        textView.text = configDataProvider.getString(viewConfig, text.stringId)
        val font = configDataProvider.getAsset<Asset.Font>(viewConfig, text.fontId)

        TypefaceHolder.getOrPut(font.value, font.style).let(textView::setTypeface)

        val fontColor = font.color
        if (fontColor != null) {
            textView.setTextColor(fontColor)
        } else {
            text.textColorId?.let { textColorId ->
                configDataProvider.getAsset<Asset.Color>(viewConfig, textColorId)
            }?.value?.let(textView::setTextColor)
        }

        (text.size ?: font.size)?.let(textView::setTextSize)
    }

    private fun computeBackgroundImageHeight(context: Context, screenHeight: Int) =
        (screenHeight * BG_IMAGE_HEIGTH_COEF)
            .coerceAtLeast(BG_IMAGE_MIN_HEIGTH_DP.dp(context)).toInt()

    private fun TextView.resizeTextOnPreDrawIfNeeded(retainOriginalHeight: Boolean) {
        viewTreeObserver.addOnPreDrawListener(object : OnPreDrawListener {
            private var lastWidth = 0
            private var lastHeight = 0
            private var retryLastCalculations = false
            private var retryCounter = 0

            override fun onPreDraw(): Boolean {
                val layout = layout ?: return true

                if (lastHeight != height) {
                    lastHeight = height
                    if (retainOriginalHeight)
                        minHeight = lastHeight
                }

                if (lastWidth != width || retryLastCalculations) {
                    lastWidth = width
                    val text = text.toString()
                    try {
                        if (!textHelper.isTruncated(text, this@resizeTextOnPreDrawIfNeeded, layout))
                            return true

                        setTextSize(
                            TypedValue.COMPLEX_UNIT_PX,
                            textHelper.findBestScaledTextSize(text, this@resizeTextOnPreDrawIfNeeded, layout),
                        )
                        if (!retainOriginalHeight)
                            contentSizeChangeConsumed = false

                        retryLastCalculations = false
                        retryCounter = 0
                    } catch (e: Exception) {
                        log(WARN) { "$LOG_PREFIX $flowKey couldn't scale text: ${e.localizedMessage ?: e.message}" }
                        if (++retryCounter <= 3) {
                            retryLastCalculations = true
                        } else {
                            retryLastCalculations = false
                            retryCounter = 0
                        }
                        return true
                    }
                    return false
                }
                return true
            }
        })
    }

    interface InteractionListener {
        fun onProductChosen(product: AdaptyPaywallProduct)
        fun onPurchaseButtonClicked()
        fun onRestoreButtonClicked()
        fun onUrlClicked(url: String)
        fun onCloseClicked()
    }
}