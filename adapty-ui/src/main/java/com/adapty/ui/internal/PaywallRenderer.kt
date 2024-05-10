package com.adapty.ui.internal

import android.app.Activity
import android.content.Context
import android.view.ViewTreeObserver.OnPreDrawListener
import androidx.annotation.RestrictTo
import androidx.constraintlayout.widget.ConstraintSet
import androidx.constraintlayout.widget.ConstraintSet.BOTTOM
import androidx.constraintlayout.widget.ConstraintSet.MATCH_CONSTRAINT
import androidx.constraintlayout.widget.ConstraintSet.MATCH_CONSTRAINT_WRAP
import androidx.constraintlayout.widget.ConstraintSet.TOP
import androidx.constraintlayout.widget.ConstraintSet.WRAP_CONTENT
import com.adapty.models.AdaptyPaywall
import com.adapty.models.AdaptyPaywallProduct
import com.adapty.ui.AdaptyPaywallInsets
import com.adapty.ui.AdaptyPaywallView
import com.adapty.ui.AdaptyUI.ViewConfiguration.Component
import com.adapty.ui.AdaptyUI.ViewConfiguration.Component.Shape
import com.adapty.ui.listeners.AdaptyUiTagResolver
import kotlin.math.abs

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class BasicPaywallRenderer(
    override val templateConfig: BasicTemplateConfig,
    productBlockRenderer: ProductBlockRenderer,
    viewHelper: ViewHelper,
    layoutHelper: LayoutHelper,
) : PaywallRenderer(templateConfig, productBlockRenderer, viewHelper, layoutHelper) {

    override fun render(
        paywall: AdaptyPaywall,
        products: List<AdaptyPaywallProduct>?,
        paywallView: AdaptyPaywallView,
        insets: AdaptyPaywallInsets,
        tagResolver: AdaptyUiTagResolver,
        actionListener: (Component.Button.Action) -> Unit,
        interactionListener: PaywallUiManager.InteractionListener,
    ): PaywallScreen {
        val context = paywallView.context

        val (screenWidth, screenHeight) = (context as Activity).windowManager.getScreenSize()

        val backgroundImageHeight = (screenHeight * templateConfig.mainImageRelativeHeight).toInt()

        viewHelper.createBackgroundView(
            context,
            screenWidth,
            backgroundImageHeight,
            templateConfig.getScreenBackground(),
        )
            .also(paywallView::addView)

        val scrollView = viewHelper.createScrollView(context)
            .also(paywallView::addView)

        val contentContainer = viewHelper.createContentContainer(context)
            .also(scrollView::addView)

        val constraintSet = ConstraintSet().apply { clone(contentContainer) }

        val mainContentShape = templateConfig.getContentBackground()

        val curvedPartHeightPx = getContentContainerCurvedPartHeightPx(context, mainContentShape)

        val verticalAnchor =
            ViewAnchor(contentContainer, TOP, TOP, backgroundImageHeight - curvedPartHeightPx)

        val contentBgView =
            viewHelper.createContentBackgroundView(context, mainContentShape, templateConfig)
                .also(contentContainer::addView)

        layoutHelper.constrain(
            contentBgView.id,
            MATCH_CONSTRAINT,
            MATCH_CONSTRAINT,
            verticalAnchor,
            constraintSet,
        )

        verticalAnchor.update(
            contentBgView,
            verticalAnchor.side,
            curvedPartHeightPx.coerceAtLeast(CONTENT_MINIMUM_TOP_MARGIN_DP.dp(context).toInt()),
        )

        val edgeMargin = templateConfig.contentEdgeMargin.dp(context).toInt()

        val verticalSpacing = templateConfig.verticalSpacing.dp(context).toInt()

        val paywallScreenProps = PaywallScreen.Props()

        val onTextViewHeightChangeOnResizeCallback: () -> Unit = {
            paywallScreenProps.contentSizeChangeConsumed = false
        }

        templateConfig.getTitle()?.let { titleTextComponent ->
            val titleView = viewHelper.createView(context, titleTextComponent, templateConfig, tagResolver)
                .also(contentContainer::addView)

            layoutHelper.constrain(
                titleView.id,
                MATCH_CONSTRAINT,
                WRAP_CONTENT,
                verticalAnchor,
                constraintSet,
                edgeMargin,
            )

            verticalAnchor.update(titleView, BOTTOM, verticalSpacing)
        }

        templateConfig.getFeatures()?.let { features ->
            val featureUIBlock = viewHelper.createFeatureUiBlock(context, features, templateConfig, tagResolver)

            when (featureUIBlock) {
                is FeatureUIBlock.List -> {
                    contentContainer.addView(featureUIBlock.textView)
                }
                is FeatureUIBlock.TimeLine -> {
                    featureUIBlock.entries.forEach { featureCell ->
                        contentContainer.addView(featureCell.textView)
                        contentContainer.addView(featureCell.imageView)
                    }
                }
            }

            val featureSpacing = templateConfig.featureSpacing.dp(context).toInt()

            layoutHelper.constrainFeatureViews(
                featureUIBlock,
                verticalAnchor,
                featureSpacing,
                edgeMargin,
                constraintSet,
                templateConfig,
            )

            verticalAnchor.update(verticalAnchor.view, BOTTOM, verticalSpacing)
        }

        val purchaseButton = viewHelper.createPurchaseButton(
            context,
            templateConfig.getPurchaseButton(),
            templateConfig,
            tagResolver,
            actionListener,
        )
            .also { view ->
                view.setOnClickListener { interactionListener.onPurchaseButtonClicked() }
            }

        val productViewBundles = productBlockRenderer.render(
            templateConfig,
            paywall,
            actionListener,
            contentContainer,
            purchaseButton.textView,
            products,
            verticalAnchor,
            paywallScreenProps,
            edgeMargin,
            constraintSet,
            tagResolver,
            interactionListener,
            onTextViewHeightChangeOnResizeCallback,
        )

        verticalAnchor.update(verticalAnchor.view, BOTTOM, verticalSpacing)

        purchaseButton.addToViewGroup(contentContainer)

        layoutHelper.constrain(
            purchaseButton,
            MATCH_CONSTRAINT,
            templateConfig.purchaseButtonHeight.dp(context).toInt(),
            verticalAnchor,
            constraintSet,
            edgeMargin,
        )

        verticalAnchor.updateView(purchaseButton.bgView)

        templateConfig.getFooterButtons().takeIf { it.isNotEmpty() }
            ?.let { buttonComponents ->
                val footerButtons = buttonComponents.map { buttonComponent ->
                    viewHelper.createFooterButton(
                        context,
                        buttonComponent,
                        templateConfig,
                        tagResolver,
                        actionListener,
                        onTextViewHeightChangeOnResizeCallback,
                    )
                        .also(contentContainer::addView)
                }

                layoutHelper.constrainFooterButtons(
                    footerButtons,
                    verticalAnchor,
                    edgeMargin,
                    constraintSet,
                )

                verticalAnchor.updateView(footerButtons.first())
            }

        contentContainer.addOnPreDrawListener(object: OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                if (!paywallScreenProps.areConsumed && contentContainer.height > 0) {
                    val contentBgViewExpectedTop = contentBgView.topCoord
                    val contentBgViewExpectedBottom =
                        verticalAnchor.view.bottomCoord + CONTENT_BOTTOM_MARGIN_DP.dp(context)
                            .toInt() + insets.bottom
                    contentBgView.layoutParams = contentBgView.layoutParams.apply {
                        height = contentBgViewExpectedBottom.coerceAtLeast(paywallView.bottomCoord) - contentBgViewExpectedTop
                    }
                    paywallScreenProps.paywallViewSizeChangeConsumed = true
                    paywallScreenProps.contentSizeChangeConsumed = true

                    scrollView.setFooterInfo(
                        purchaseButton,
                        screenHeight - insets.bottom - STICKY_FOOTER_BOTTOM_MARGIN_DP.dp(context).toInt()
                    )
                }
                return true
            }
        })

        constraintSet.applyTo(contentContainer)

        if (!templateConfig.isHardPaywall()) {
            val closeButtonComponent = templateConfig.getCloseButton()

            viewHelper.createCloseView(
                context,
                closeButtonComponent,
                templateConfig,
                insets,
                tagResolver,
                actionListener
            )
                .also(paywallView::addView)
        }

        val loadingView = viewHelper.createLoadingView(context)
            .also(paywallView::addView)

        return PaywallScreen(
            contentContainer,
            purchaseButton,
            productViewBundles,
            loadingView,
            paywallScreenProps,
        )
    }

    private fun getContentContainerCurvedPartHeightPx(context: Context, shape: Shape): Int {
        return (when (val shapeType = shape.type) {
            is Shape.Type.RectWithArc -> abs(shapeType.arcHeight)
            is Shape.Type.Rectangle -> when (val cornerRadius = shapeType.cornerRadius) {
                is Shape.CornerRadius.Different -> cornerRadius.topLeft
                is Shape.CornerRadius.Same -> cornerRadius.value
                is Shape.CornerRadius.None -> 0f
            }
            else -> 0f
        }).dp(context).toInt()
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class TransparentPaywallRenderer(
    override val templateConfig: TransparentTemplateConfig,
    productBlockRenderer: ProductBlockRenderer,
    viewHelper: ViewHelper,
    layoutHelper: LayoutHelper,
) : PaywallRenderer(templateConfig, productBlockRenderer, viewHelper, layoutHelper) {

    override fun render(
        paywall: AdaptyPaywall,
        products: List<AdaptyPaywallProduct>?,
        paywallView: AdaptyPaywallView,
        insets: AdaptyPaywallInsets,
        tagResolver: AdaptyUiTagResolver,
        actionListener: (Component.Button.Action) -> Unit,
        interactionListener: PaywallUiManager.InteractionListener,
    ): PaywallScreen {
        val context = paywallView.context

        val (screenWidth, screenHeight) = (context as Activity).windowManager.getScreenSize()

        viewHelper.createBackgroundView(
            context,
            screenWidth,
            screenHeight,
            templateConfig.getScreenBackground(),
        )
            .also(paywallView::addView)

        val scrollView = viewHelper.createScrollView(context)
            .also(paywallView::addView)

        val contentContainer = viewHelper.createContentContainer(context, screenHeight)
            .also(scrollView::addView)

        val constraintSet = ConstraintSet().apply { clone(contentContainer) }

        val verticalAnchor = ViewAnchor(contentContainer, BOTTOM, BOTTOM, 0)

        val mainContentShape = templateConfig.getContentBackground()

        val contentBgView =
            viewHelper.createContentBackgroundView(context, mainContentShape, templateConfig)
                .also(contentContainer::addView)

        layoutHelper.constrain(
            contentBgView.id,
            MATCH_CONSTRAINT,
            MATCH_CONSTRAINT,
            verticalAnchor,
            constraintSet,
        )

        verticalAnchor.update(
            contentBgView,
            verticalAnchor.side,
            CONTENT_BOTTOM_MARGIN_DP.dp(context).toInt() + insets.bottom,
        )

        val edgeMargin = templateConfig.contentEdgeMargin.dp(context).toInt()

        val verticalSpacing = templateConfig.verticalSpacing.dp(context).toInt()

        val paywallScreenProps = PaywallScreen.Props()

        val onTextViewHeightChangeOnResizeCallback: () -> Unit = {
            paywallScreenProps.contentSizeChangeConsumed = false
        }

        templateConfig.getFooterButtons().takeIf { it.isNotEmpty() }
            ?.let { buttonComponents ->
                val footerButtons = buttonComponents.map { buttonComponent ->
                    viewHelper.createFooterButton(
                        context,
                        buttonComponent,
                        templateConfig,
                        tagResolver,
                        actionListener,
                        onTextViewHeightChangeOnResizeCallback,
                    )
                        .also(contentContainer::addView)
                }

                layoutHelper.constrainFooterButtons(
                    footerButtons,
                    verticalAnchor,
                    edgeMargin,
                    constraintSet,
                )

                verticalAnchor.update(footerButtons.first(), TOP, verticalSpacing)
            }

        val purchaseButton = viewHelper.createPurchaseButton(
            context,
            templateConfig.getPurchaseButton(),
            templateConfig,
            tagResolver,
            actionListener,
        )
            .also { view ->
                view.setOnClickListener { interactionListener.onPurchaseButtonClicked() }
            }

        layoutHelper.constrain(
            purchaseButton,
            MATCH_CONSTRAINT,
            templateConfig.purchaseButtonHeight.dp(context).toInt(),
            verticalAnchor,
            constraintSet,
            edgeMargin,
        )

        verticalAnchor.update(purchaseButton.bgView, TOP, verticalSpacing)

        val productViewBundles = productBlockRenderer.render(
            templateConfig,
            paywall,
            actionListener,
            contentContainer,
            purchaseButton.textView,
            products,
            verticalAnchor,
            paywallScreenProps,
            edgeMargin,
            constraintSet,
            tagResolver,
            interactionListener,
            onTextViewHeightChangeOnResizeCallback,
        )

        verticalAnchor.update(verticalAnchor.view, TOP, verticalSpacing)

        purchaseButton.addToViewGroup(contentContainer)

        templateConfig.getFeatures()?.let { features ->
            val featureUIBlock = viewHelper.createFeatureUiBlock(context, features, templateConfig, tagResolver)

            when (featureUIBlock) {
                is FeatureUIBlock.List -> {
                    contentContainer.addView(featureUIBlock.textView)
                }
                is FeatureUIBlock.TimeLine -> {
                    featureUIBlock.entries.forEach { featureCell ->
                        contentContainer.addView(featureCell.textView)
                        contentContainer.addView(featureCell.imageView)
                    }
                }
            }

            val featureSpacing = templateConfig.featureSpacing.dp(context).toInt()

            layoutHelper.constrainFeatureViews(
                featureUIBlock,
                verticalAnchor,
                featureSpacing,
                edgeMargin,
                constraintSet,
                templateConfig,
            )

            verticalAnchor.update(verticalAnchor.view, TOP, verticalSpacing)
        }

        templateConfig.getTitle()?.let { titleTextComponent ->
            val titleView = viewHelper.createView(context, titleTextComponent, templateConfig, tagResolver)
                .also(contentContainer::addView)

            layoutHelper.constrain(
                titleView.id,
                MATCH_CONSTRAINT,
                WRAP_CONTENT,
                verticalAnchor,
                constraintSet,
                edgeMargin,
            )

            verticalAnchor.updateView(titleView)
        }

        contentContainer.addOnPreDrawListener(object: OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                if (!paywallScreenProps.areConsumed && contentContainer.height > 0) {
                    val contentBgViewExpectedBottom = contentBgView.bottomCoord
                    val contentBgViewExpectedTop = verticalAnchor.view.topCoord - insets.top

                    contentBgView.layoutParams = contentBgView.layoutParams.apply {
                        height = contentBgViewExpectedBottom - contentBgViewExpectedTop.coerceAtMost(paywallView.topCoord)
                    }
                    paywallScreenProps.paywallViewSizeChangeConsumed = true
                    paywallScreenProps.contentSizeChangeConsumed = true
                }
                return true
            }
        })

        constraintSet.applyTo(contentContainer)

        if (!templateConfig.isHardPaywall()) {
            val closeButtonComponent = templateConfig.getCloseButton()

            viewHelper.createCloseView(
                context,
                closeButtonComponent,
                templateConfig,
                insets,
                tagResolver,
                actionListener
            )
                .also(paywallView::addView)
        }

        val loadingView = viewHelper.createLoadingView(context)
            .also(paywallView::addView)

        return PaywallScreen(
            contentContainer,
            purchaseButton,
            productViewBundles,
            loadingView,
            paywallScreenProps,
        )
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class FlatPaywallRenderer(
    override val templateConfig: FlatTemplateConfig,
    productBlockRenderer: ProductBlockRenderer,
    viewHelper: ViewHelper,
    layoutHelper: LayoutHelper,
) : PaywallRenderer(templateConfig, productBlockRenderer, viewHelper, layoutHelper) {

    override fun render(
        paywall: AdaptyPaywall,
        products: List<AdaptyPaywallProduct>?,
        paywallView: AdaptyPaywallView,
        insets: AdaptyPaywallInsets,
        tagResolver: AdaptyUiTagResolver,
        actionListener: (Component.Button.Action) -> Unit,
        interactionListener: PaywallUiManager.InteractionListener,
    ): PaywallScreen {
        val context = paywallView.context

        val (screenWidth, screenHeight) = (context as Activity).windowManager.getScreenSize()

        viewHelper.createBackgroundView(
            context,
            screenWidth,
            screenHeight,
            templateConfig.getScreenBackground(),
        )
            ?.also(paywallView::addView)

        val scrollView = viewHelper.createScrollView(context)
            .also(paywallView::addView)

        val contentContainer = viewHelper.createContentContainer(context)
            .also(scrollView::addView)

        val constraintSet = ConstraintSet().apply { clone(contentContainer) }

        val mainContentShape = templateConfig.getContentBackground()

        val verticalAnchor = ViewAnchor(contentContainer, TOP, TOP, 0)

        val contentBgView =
            viewHelper.createContentBackgroundView(context, mainContentShape, templateConfig)
                .also(contentContainer::addView)

        layoutHelper.constrain(
            contentBgView.id,
            MATCH_CONSTRAINT,
            MATCH_CONSTRAINT,
            verticalAnchor,
            constraintSet,
        )

        verticalAnchor.update(
            contentBgView,
            verticalAnchor.side,
            FLAT_TOP_IMAGE_MARGIN_DP.dp(context).toInt() + insets.top,
        )

        val topImageHeight = (screenHeight * templateConfig.mainImageRelativeHeight).toInt()

        val edgeMargin = templateConfig.contentEdgeMargin.dp(context).toInt()

        val verticalSpacing = templateConfig.verticalSpacing.dp(context).toInt()

        val topImageShape = templateConfig.getTopImage()

        val topImageView = viewHelper.createContentImage(
            context,
            topImageShape,
            templateConfig,
        )
            .also(contentContainer::addView)

        layoutHelper.constrain(
            topImageView.id,
            MATCH_CONSTRAINT,
            topImageHeight,
            MATCH_CONSTRAINT_WRAP,
            null,
            verticalAnchor,
            constraintSet,
            edgeMargin,
        )

        verticalAnchor.update(topImageView, BOTTOM, edgeMargin)

        val paywallScreenProps = PaywallScreen.Props()

        val onTextViewHeightChangeOnResizeCallback: () -> Unit = {
            paywallScreenProps.contentSizeChangeConsumed = false
        }

        templateConfig.getTitle()?.let { titleTextComponent ->
            val titleView = viewHelper.createView(context, titleTextComponent, templateConfig, tagResolver)
                .also(contentContainer::addView)

            layoutHelper.constrain(
                titleView.id,
                MATCH_CONSTRAINT,
                WRAP_CONTENT,
                verticalAnchor,
                constraintSet,
                edgeMargin,
            )

            verticalAnchor.update(titleView, BOTTOM, verticalSpacing)
        }

        templateConfig.getFeatures()?.let { features ->
            val featureUIBlock = viewHelper.createFeatureUiBlock(context, features, templateConfig, tagResolver)

            when (featureUIBlock) {
                is FeatureUIBlock.List -> {
                    contentContainer.addView(featureUIBlock.textView)
                }
                is FeatureUIBlock.TimeLine -> {
                    featureUIBlock.entries.forEach { featureCell ->
                        contentContainer.addView(featureCell.textView)
                        contentContainer.addView(featureCell.imageView)
                    }
                }
            }

            val featureSpacing = templateConfig.featureSpacing.dp(context).toInt()

            layoutHelper.constrainFeatureViews(
                featureUIBlock,
                verticalAnchor,
                featureSpacing,
                edgeMargin,
                constraintSet,
                templateConfig,
            )

            verticalAnchor.update(verticalAnchor.view, BOTTOM, verticalSpacing)
        }

        val purchaseButton = viewHelper.createPurchaseButton(
            context,
            templateConfig.getPurchaseButton(),
            templateConfig,
            tagResolver,
            actionListener,
        )
            .also { view ->
                view.setOnClickListener { interactionListener.onPurchaseButtonClicked() }
            }

        val productViewBundles = productBlockRenderer.render(
            templateConfig,
            paywall,
            actionListener,
            contentContainer,
            purchaseButton.textView,
            products,
            verticalAnchor,
            paywallScreenProps,
            edgeMargin,
            constraintSet,
            tagResolver,
            interactionListener,
            onTextViewHeightChangeOnResizeCallback,
        )

        verticalAnchor.update(verticalAnchor.view, BOTTOM, verticalSpacing)

        purchaseButton.addToViewGroup(contentContainer)

        layoutHelper.constrain(
            purchaseButton,
            MATCH_CONSTRAINT,
            templateConfig.purchaseButtonHeight.dp(context).toInt(),
            verticalAnchor,
            constraintSet,
            edgeMargin,
        )

        verticalAnchor.updateView(purchaseButton.bgView)

        templateConfig.getFooterButtons().takeIf { it.isNotEmpty() }
            ?.let { buttonComponents ->
                val footerButtons = buttonComponents.map { buttonComponent ->
                    viewHelper.createFooterButton(
                        context,
                        buttonComponent,
                        templateConfig,
                        tagResolver,
                        actionListener,
                        onTextViewHeightChangeOnResizeCallback,
                    )
                        .also(contentContainer::addView)
                }

                layoutHelper.constrainFooterButtons(
                    footerButtons,
                    verticalAnchor,
                    edgeMargin,
                    constraintSet,
                )

                verticalAnchor.updateView(footerButtons.first())
            }

        contentContainer.addOnPreDrawListener(object: OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                if (!paywallScreenProps.areConsumed && contentContainer.height > 0) {
                    val contentBgViewExpectedTop = contentBgView.topCoord
                    val contentBgViewExpectedBottom =
                        verticalAnchor.view.bottomCoord + CONTENT_BOTTOM_MARGIN_DP.dp(context)
                            .toInt() + insets.bottom
                    contentBgView.layoutParams = contentBgView.layoutParams.apply {
                        height = contentBgViewExpectedBottom - contentBgViewExpectedTop
                    }
                    paywallScreenProps.paywallViewSizeChangeConsumed = true
                    paywallScreenProps.contentSizeChangeConsumed = true

                    scrollView.setFooterInfo(
                        purchaseButton,
                        screenHeight - insets.bottom - STICKY_FOOTER_BOTTOM_MARGIN_DP.dp(context).toInt()
                    )
                }
                return true
            }
        })

        constraintSet.applyTo(contentContainer)

        if (!templateConfig.isHardPaywall()) {
            val closeButtonComponent = templateConfig.getCloseButton()

            viewHelper.createCloseView(
                context,
                closeButtonComponent,
                templateConfig,
                insets,
                tagResolver,
                actionListener
            )
                .also(paywallView::addView)
        }

        val loadingView = viewHelper.createLoadingView(context)
            .also(paywallView::addView)

        return PaywallScreen(
            contentContainer,
            purchaseButton,
            productViewBundles,
            loadingView,
            paywallScreenProps,
        )
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal sealed class PaywallRenderer(
    protected open val templateConfig: TemplateConfig,
    protected val productBlockRenderer: ProductBlockRenderer,
    protected val viewHelper: ViewHelper,
    protected val layoutHelper: LayoutHelper,
) {

    abstract fun render(
        paywall: AdaptyPaywall,
        products: List<AdaptyPaywallProduct>?,
        paywallView: AdaptyPaywallView,
        insets: AdaptyPaywallInsets,
        tagResolver: AdaptyUiTagResolver,
        actionListener: (Component.Button.Action) -> Unit,
        interactionListener: PaywallUiManager.InteractionListener,
    ): PaywallScreen

    companion object {

        fun create(
            templateConfig: TemplateConfig,
            productBlockRenderer: ProductBlockRenderer,
            viewHelper: ViewHelper,
            layoutHelper: LayoutHelper
        ): PaywallRenderer {
            return when (templateConfig) {
                is BasicTemplateConfig -> BasicPaywallRenderer(
                    templateConfig,
                    productBlockRenderer,
                    viewHelper,
                    layoutHelper
                )

                is TransparentTemplateConfig -> TransparentPaywallRenderer(
                    templateConfig,
                    productBlockRenderer,
                    viewHelper,
                    layoutHelper
                )

                is FlatTemplateConfig -> FlatPaywallRenderer(
                    templateConfig,
                    productBlockRenderer,
                    viewHelper,
                    layoutHelper
                )
            }
        }
    }
}