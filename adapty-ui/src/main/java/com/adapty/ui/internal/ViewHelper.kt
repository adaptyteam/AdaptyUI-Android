package com.adapty.ui.internal

import android.content.Context
import android.graphics.Color
import android.graphics.Outline
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.text.TextUtils.TruncateAt
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.RestrictTo
import androidx.constraintlayout.widget.ConstraintLayout
import com.adapty.ui.AdaptyPaywallInsets
import com.adapty.ui.AdaptyUI.ViewConfiguration.Asset
import com.adapty.ui.AdaptyUI.ViewConfiguration.Component
import com.adapty.ui.internal.cache.MediaFetchService
import com.adapty.ui.listeners.AdaptyUiTagResolver
import com.adapty.utils.AdaptyLogLevel.Companion.WARN

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class ViewHelper(
    private val flowKey: String,
    private val drawableHelper: DrawableHelper,
    private val textHelper: TextHelper,
    private val textComponentHelper: TextComponentHelper,
    private val bitmapHelper: BitmapHelper,
    private val mediaFetchService: MediaFetchService,
) {

    fun createView(context: Context, textComponent: Component.Text, templateConfig: TemplateConfig, tagResolver: AdaptyUiTagResolver): TextView {
        return TextView(context)
            .also { view ->
                view.id = View.generateViewId()

                val textProperties = textComponentHelper.processTextComponent(context, textComponent, templateConfig, tagResolver)

                applyTextProperties(view, textProperties)
                fixItalicClipping(view)
            }
    }

    fun createView(
        context: Context,
        buttonComponent: Component.Button,
        templateConfig: TemplateConfig,
        tagResolver: AdaptyUiTagResolver,
        actionListener: (Component.Button.Action) -> Unit,
        addRipple: Boolean = true,
    ): TextView {
        val view = TextView(context)
        view.id = View.generateViewId()
        applyButtonProperties(view, buttonComponent, templateConfig, actionListener, addRipple)
        fixItalicClipping(view)

        buttonComponent.title?.let { title ->
            val textProperties = textComponentHelper.processTextComponent(context, title, templateConfig, tagResolver)
            applyTextProperties(view, textProperties)
        }

        return view
    }

    fun createProductViewsBundle(
        context: Context,
        productInfo: ProductInfo,
        blockType: Products.BlockType,
        templateConfig: TemplateConfig,
        tagResolver: AdaptyUiTagResolver,
        actionListener: (Component.Button.Action) -> Unit,
        onTextViewHeightChangeOnResizeCallback: () -> Unit,
    ): ProductViewsBundle {
        val typeIsSingle = blockType == Products.BlockType.Single

        val productCell = productInfo.button?.takeIf { !typeIsSingle }?.let { button ->
            createView(context, button, templateConfig, tagResolver, actionListener, addRipple = false)
        }

        val productTitle = productInfo.title?.let { text ->
            createInnerProductText(context, text, templateConfig, tagResolver)
                .also { view -> textHelper.resizeTextOnPreDrawIfNeeded(view, true, onTextViewHeightChangeOnResizeCallback) }
        }

        val productSubtitle = productInfo.hasSubtitle.takeIf { it }?.let {
            TextView(context).also { view ->
                view.id = View.generateViewId()
                view.maxLines = 2
                view.ellipsize = TruncateAt.END
                textHelper.resizeTextOnPreDrawIfNeeded(view, true, onTextViewHeightChangeOnResizeCallback)
                fixItalicClipping(view)
            }
        }

        val productSecondTitle = productInfo.secondTitle?.let { text ->
            createInnerProductText(context, text, templateConfig, tagResolver)
                .also { view -> textHelper.resizeTextOnPreDrawIfNeeded(view, true, onTextViewHeightChangeOnResizeCallback) }
        }

        val productSecondSubtitle = productInfo.secondSubtitle?.takeIf { !typeIsSingle }?.let { text ->
            createInnerProductText(context, text, templateConfig, tagResolver)
                .also { view -> textHelper.resizeTextOnPreDrawIfNeeded(view, true, onTextViewHeightChangeOnResizeCallback) }
        }

        val productTag = productInfo.tagText?.takeIf { !typeIsSingle }?.let { tagText ->
            createMainProductTag(context, tagText, productInfo.tagShape, templateConfig, tagResolver)
                .also { view -> textHelper.resizeTextOnPreDrawIfNeeded(view, true, onTextViewHeightChangeOnResizeCallback) }
        }

        return ProductViewsBundle(
            productCell,
            productTitle,
            productSubtitle,
            productSecondTitle,
            productSecondSubtitle,
            productTag,
        )
    }

    fun createFeatureUiBlock(
        context: Context,
        features: Features,
        templateConfig: TemplateConfig,
        tagResolver: AdaptyUiTagResolver,
    ): FeatureUIBlock {
        return when (features) {
            is Features.List -> {
                val textView = createView(context, features.textComponent, templateConfig, tagResolver)

                FeatureUIBlock.List(textView)
            }
            is Features.TimeLine -> {
                val timelineDrawableWidthPx = TIMELINE_DRAWABLE_BACKGROUND_WIDTH_DP.dp(context).toInt()
                val timelineTextStartMarginPx = TIMELINE_TEXT_START_MARGIN_DP.dp(context).toInt()

                val cells = features.timelineEntries.map { timelineEntry ->
                    val textView = createView(context, timelineEntry.text, templateConfig, tagResolver)
                    val startDrawable = drawableHelper.createTimelineDrawable(timelineEntry, templateConfig, context)

                    val image = View(context)
                    image.id = View.generateViewId()
                    image.background = startDrawable

                    FeatureUIBlock.TimeLine.Cell(image, textView, timelineDrawableWidthPx, timelineTextStartMarginPx)
                }

                FeatureUIBlock.TimeLine(cells)
            }
        }
    }

    fun createContentBackgroundView(
        context: Context,
        shape: Component.Shape,
        templateConfig: TemplateConfig,
    ): View {
        return View(context).apply {
            id = View.generateViewId()
            setShapeBackgroundAsync(shape, templateConfig)
        }
    }

    fun createScrollView(context: Context): PaywallScrollView {
        return PaywallScrollView(context).apply {
            id = View.generateViewId()

            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT,
            )

            isVerticalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
        }
    }

    fun createLoadingView(context: Context): ProgressBar {
        return PaywallProgressBar(context).apply {
            id = View.generateViewId()
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT,
            )
            isIndeterminate = true
            isClickable = true
            setBackgroundColor(Color.parseColor(LOADING_BG_COLOR_HEX))
            visibility = View.GONE
        }
    }

    fun createContentContainer(context: Context, minHeight: Int? = null): ConstraintLayout {
        return ConstraintLayout(context).apply {
            id = View.generateViewId()
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT,
            ).apply {
                minHeight?.let(::setMinHeight)
            }
            clipChildren = false
        }
    }

    @JvmName("createBackgroundViewOrNull")
    fun createBackgroundView(
        context: Context,
        width: Int,
        height: Int,
        filling: Asset.Filling?,
    ): View? {
        return filling?.let { createBackgroundView(context, width, height, filling) }
    }

    fun createBackgroundView(
        context: Context,
        width: Int,
        height: Int,
        filling: Asset.Filling,
    ): View {
        return ImageView(context).apply {
            id = View.generateViewId()

            layoutParams = LayoutParams(width, height)

            setImageDrawableAsync(filling)

            scaleType = ImageView.ScaleType.CENTER_CROP
        }
    }

    private fun ImageView.setImageDrawableAsync(filling: Asset.Filling) {
        when (filling) {
            is Asset.RemoteImage -> {
                mediaFetchService.loadImage(
                    filling,
                    handlePreview = { previewImage ->
                        post {
                            if (isAttachedToWindow)
                                setImageDrawable(drawableHelper.createDrawable(previewImage))
                        }
                    },
                    handleResult = { mainImage ->
                        post {
                            if (isAttachedToWindow)
                                setImageDrawable(drawableHelper.createDrawable(mainImage))
                        }
                    }
                )
            }
            is Asset.Filling.Local -> setImageDrawable(drawableHelper.createDrawable(filling))
        }
    }

    fun createContentImage(
        context: Context,
        shape: Component.Shape,
        templateConfig: TemplateConfig,
    ): View {
        return View(context).apply {
            id = View.generateViewId()
            setShapeBackgroundAsync(shape, templateConfig)
        }
    }

    private fun View.setShapeBackgroundAsync(
        shape: Component.Shape,
        templateConfig: TemplateConfig,
    ) {
        val shapeType = drawableHelper.extractShapeType(shape, context)

        val fillAsset = shape.backgroundAssetId?.let { assetId ->
            templateConfig.getAsset<Asset.Filling>(assetId)
        }

        val stroke = shape.border?.let { border ->
            border to templateConfig.getAsset<Asset.Filling.Local>(border.assetId)
        }

        when (fillAsset) {
            is Asset.RemoteImage -> {
                mediaFetchService.loadImage(
                    fillAsset,
                    handlePreview = { previewImage ->
                        post {
                            if (isAttachedToWindow)
                                background = drawableHelper.createDrawable(shapeType, previewImage, stroke, context)
                        }
                    },
                    handleResult = { mainImage ->
                        post {
                            if (isAttachedToWindow)
                                background = drawableHelper.createDrawable(shapeType, mainImage, stroke, context)
                        }
                    }
                )
            }
            else -> background = drawableHelper.createDrawable(shapeType, fillAsset as? Asset.Filling.Local, stroke, context)
        }
    }

    fun createCloseView(
        context: Context,
        buttonComponent: Component.Button,
        templateConfig: TemplateConfig,
        insets: AdaptyPaywallInsets,
        tagResolver: AdaptyUiTagResolver,
        actionListener: (Component.Button.Action) -> Unit,
    ): View {
        val height = CLOSE_BUTTON_HEIGHT_DP.dp(context).toInt()
        val width: Int
        val title = buttonComponent.title
        return if (title is Component.Text) {
            width = LayoutParams.WRAP_CONTENT
            val horizontalPadding = CLOSE_BUTTON_TEXT_HORIZONTAL_PADDING_DP.dp(context).toInt()
            createView(context, buttonComponent, templateConfig, tagResolver, actionListener)
                .apply {
                    setPadding(horizontalPadding, paddingTop, horizontalPadding, paddingBottom)
                    setVerticalGravity(Gravity.CENTER_VERTICAL)
                }
        } else {
            width = height
            createView(context, buttonComponent, templateConfig, tagResolver, actionListener)
                .also { view ->
                    buttonComponent.shape?.backgroundAssetId?.let { assetId ->
                        view.background = BitmapDrawable(
                            context.resources,
                            bitmapHelper.getBitmap(templateConfig.getAsset(assetId)),
                        )
                    }
                }
        }.apply {
            val edgeHorizontalMargin = CLOSE_BUTTON_HORIZONTAL_MARGIN_DP.dp(context).toInt()
            layoutParams = FrameLayout.LayoutParams(width, height).apply {
                topMargin = CLOSE_BUTTON_TOP_MARGIN_DP.dp(context).toInt() + insets.top

                when (buttonComponent.align) {
                    Component.Button.Align.TRAILING -> {
                        gravity = Gravity.TOP or Gravity.END
                        marginEnd = edgeHorizontalMargin
                    }
                    Component.Button.Align.CENTER -> {
                        gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                    }
                    else -> {
                        gravity = Gravity.TOP or Gravity.START
                        marginStart = edgeHorizontalMargin
                    }
                }
            }
        }
    }

    fun createPurchaseButton(
        context: Context,
        buttonComponent: Component.Button,
        templateConfig: TemplateConfig,
        tagResolver: AdaptyUiTagResolver,
        actionListener: (Component.Button.Action) -> Unit,
    ): ComplexButton {
        val bgView = View(context).apply {
            id = View.generateViewId()
            applyButtonProperties(this, buttonComponent, templateConfig, actionListener)
        }

        val textView = buttonComponent.title?.let { title ->
            createView(context, title, templateConfig, tagResolver)
                .apply {
                    isClickable = false
                    isFocusable = false
                    maxLines = 2
                    ellipsize = TruncateAt.END
                    setVerticalGravity(Gravity.CENTER_VERTICAL)
                    textHelper.resizeTextOnPreDrawIfNeeded(this, true)
                }
        }

        val paddings = ComplexButton.Paddings.all(PURCHASE_BUTTON_PADDING_DP.dp(context).toInt())

        return ComplexButton(bgView, textView, paddings)
    }

    private fun createMainProductTag(
        context: Context,
        textComponent: Component.Text,
        shapeComponent: Component.Shape?,
        templateConfig: TemplateConfig,
        tagResolver: AdaptyUiTagResolver,
    ): TextView {
        return createView(context, textComponent, templateConfig, tagResolver)
            .apply {
                makeSingleLine(this)

                gravity = Gravity.CENTER

                shapeComponent?.let { shape ->
                    setShapeBackgroundAsync(shape, templateConfig)
                }

                val horizontalPadding = PRODUCT_TAG_HORIZONTAL_PADDING_DP.dp(context).toInt()
                setPadding(horizontalPadding, paddingTop, horizontalPadding, paddingBottom)
            }
    }

    private fun createInnerProductText(
        context: Context,
        textComponent: Component.Text,
        templateConfig: TemplateConfig,
        tagResolver: AdaptyUiTagResolver,
    ): TextView {
        return TextView(context)
            .also { view ->
                view.id = View.generateViewId()

                val properties = textComponentHelper.processTextComponent(context, textComponent, templateConfig, tagResolver)

                view.setHorizontalGravity(properties.horizontalGravity)
                properties.textSize?.let(view::setTextSize)

                if (properties is TextProperties.Single) {
                    properties.typeface?.let(view::setTypeface)
                    properties.textColor?.let(view::setTextColor)
                }

                view.transformationMethod = null
                view.includeFontPadding = false

                makeSingleLine(view)
                fixItalicClipping(view)
            }
    }

    fun createFooterButton(
        context: Context,
        buttonComponent: Component.Button,
        templateConfig: TemplateConfig,
        tagResolver: AdaptyUiTagResolver,
        actionListener: (Component.Button.Action) -> Unit,
        onTextViewHeightChangeOnResizeCallback: () -> Unit,
    ): TextView {
        return createView(context, buttonComponent, templateConfig, tagResolver, actionListener)
            .apply {
                val verticalPadding = FOOTER_BUTTON_VERTICAL_PADDING_DP.dp(context).toInt()
                setPadding(paddingLeft, verticalPadding, paddingRight, verticalPadding)
                gravity = Gravity.CENTER

                val horizontalPadding = FOOTER_BUTTON_HORIZONTAL_PADDING_DP.dp(context).toInt()
                setPadding(horizontalPadding, paddingTop, horizontalPadding, paddingBottom)

                makeSingleLine(this)

                val bgDrawable = background
                if (bgDrawable == null) {
                    applyBackgroundRipple(this)

                    val rippleCornerRadiusDefault = FOOTER_BUTTON_OUTLINE_CORNER_RADIUS_DP.dp(context)

                    clipToOutline = true
                    outlineProvider = object : ViewOutlineProvider() {
                        override fun getOutline(view: View, outline: Outline) {
                            outline.setRoundRect(
                                0,
                                0,
                                view.width,
                                view.height,
                                rippleCornerRadiusDefault,
                            )
                        }
                    }
                } else {
                    applyForegroundRipple(this, bgDrawable)
                }

                textHelper.resizeTextOnPreDrawIfNeeded(this, true, onTextViewHeightChangeOnResizeCallback)
            }
    }

    private fun applyButtonProperties(
        view: View,
        buttonComponent: Component.Button,
        templateConfig: TemplateConfig,
        actionListener: (Component.Button.Action) -> Unit,
        addRipple: Boolean = true,
    ) {
        buttonComponent.shape?.let { shape ->
            val shapes = listOf(
                android.R.attr.state_selected to buttonComponent.selectedShape,
                android.R.attr.state_enabled to shape,
            )
            val buttonBg = drawableHelper.createDrawable(shapes, templateConfig, view.context)

            view.background = buttonBg

            if (addRipple)
                applyForegroundRipple(view, buttonBg)
        }

        buttonComponent.action?.let { action ->
            view.setOnClickListener {
                actionListener.invoke(action)
            }
        }

        try {
            view.visibility = if (buttonComponent.isVisible) View.VISIBLE else View.INVISIBLE

            buttonComponent.transitionIn?.let { transitionIn ->
                if (transitionIn.startDelayMillis > 0) {
                    view.postDelayed({
                        if (!view.isAttachedToWindow) return@postDelayed
                        startTransition(view, transitionIn)
                    }, transitionIn.startDelayMillis)
                } else {
                    startTransition(view, transitionIn)
                }
            }
        } catch (e: LinkageError) {
            log(WARN) { "$LOG_PREFIX $flowKey With this version of Adapty SDK, the 'Show button after delay' feature does not work. Please update to 2.10.1 or newer." }
        }
    }

    private fun startTransition(view: View, transition: Component.Button.Transition) {
        when (transition) {
            is Component.Button.Transition.Fade -> {
                view.visibility = View.VISIBLE
                view.alpha = 0f
                view.animate()
                    .alpha(1f)
                    .setInterpolator(transition.interpolator)
                    .setDuration(transition.durationMillis)
            }
        }
    }

    private fun applyForegroundRipple(view: View, bgDrawable: Drawable) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val typedValue = resolveAttr(view.context, android.R.attr.selectableItemBackground)
                val fgDrawable = drawableHelper.createForegroundRippleDrawable(
                    view.context,
                    typedValue.resourceId,
                    bgDrawable
                )

                if (fgDrawable != null) {
                    view.foreground = fgDrawable
                }
            }
        } catch (e: Throwable) { }
    }

    private fun applyBackgroundRipple(view: View) {
        val typedValue = resolveAttr(view.context, android.R.attr.selectableItemBackground)

        if (typedValue.resourceId != 0) {
            view.setBackgroundResource(typedValue.resourceId)
        }
    }

    private fun resolveAttr(context: Context, attrRes: Int): TypedValue {
        val typedValue = TypedValue()

        context.theme.resolveAttribute(attrRes, typedValue, true)
        return typedValue
    }

    fun applyTextProperties(view: TextView, properties: TextProperties) {
        view.text = properties.text
        view.setHorizontalGravity(properties.horizontalGravity)
        view.textAlignment = properties.textAlignment
        properties.textSize?.let(view::setTextSize)

        if (properties is TextProperties.Single) {
            properties.typeface?.let(view::setTypeface)
            properties.textColor?.let(view::setTextColor)
        }

        view.transformationMethod = null
        view.includeFontPadding = false
    }

    private fun makeSingleLine(view: TextView, ellipsize: TruncateAt = TruncateAt.END) {
        view.setSingleLine()
        view.maxLines = 1
        view.ellipsize = ellipsize
    }

    private fun fixItalicClipping(view: TextView) {
        view.setShadowLayer(1f, 10f, 0f, Color.TRANSPARENT)
    }
}