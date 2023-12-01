package com.adapty.ui.internal

import android.content.Context
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.TextPaint
import android.text.style.AlignmentSpan
import android.text.style.ForegroundColorSpan
import android.text.style.ImageSpan
import android.text.style.RelativeSizeSpan
import androidx.annotation.RestrictTo
import com.adapty.models.AdaptyViewConfiguration
import com.adapty.models.AdaptyViewConfiguration.Component.Text
import com.adapty.utils.AdaptyLogLevel

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class TextComponentHelper(
    private val flowKey: String,
) {

    fun processTextComponent(
        context: Context,
        textComponent: Text,
        templateConfig: TemplateConfig,
        productPlaceholders: List<ProductPlaceholderContentData>? = null
    ): TextProperties {
        val propertiesBuilder = TextProperties.Builder()
        propertiesBuilder.horizontalGravity = textComponent.horizontalAlign.toGravity()

        when (textComponent) {
            is Text.Single -> {
                propertiesBuilder.isMultiple = false

                val font = templateConfig.getAsset<AdaptyViewConfiguration.Asset.Font>(textComponent.fontId)

                propertiesBuilder.typeface = TypefaceHolder.getOrPut(context, font.value, font.style)

                val content = templateConfig.getString(textComponent.stringId)
                    ?.let { str ->
                        if (productPlaceholders != null) {
                            val paint = TextPaint().apply {
                                propertiesBuilder.typeface?.let(::setTypeface)
                            }
                            replaceProductPlaceholders(str, productPlaceholders, paint)
                        } else {
                            str
                        }
                    }
                propertiesBuilder.text = content

                propertiesBuilder.textSize = textComponent.size ?: font.size

                val textColorFromComponent = textComponent.textColorId?.let { assetId ->
                    templateConfig.getAsset<AdaptyViewConfiguration.Asset.Color>(assetId)
                }

                propertiesBuilder.textColor = textColorFromComponent?.value ?: font.color

                return propertiesBuilder.build()
            }

            is Text.Multiple -> {
                propertiesBuilder.isMultiple = true

                val content = SpannableStringBuilder()

                textComponent.items.forEach { item ->
                    when (item) {
                        is Text.Item.Text -> {
                            val currentStr =
                                processTextItem(context, templateConfig, item, textComponent, propertiesBuilder, productPlaceholders) ?: return@forEach
                            content.append(currentStr)
                        }

                        is Text.Item.NewLine -> content.append("\n")

                        is Text.Item.BulletedText -> {
                            val textPart = item.text
                            val spacePart = item.space

                            val currentStr =
                                processTextItem(context, templateConfig, textPart, textComponent, propertiesBuilder, productPlaceholders) ?: return@forEach

                            val spacePx = spacePart?.value?.dp(context)?.toInt() ?: 0
                            val verticalSpacePx = templateConfig.featureSpacing.dp(context).toInt()

                            val bulletSpan = when (val bullet = item.bullet) {
                                is Text.Item.BulletedText.ImageBullet -> {
                                    val image = bullet.image

                                    val widthPx = image.width.dp(context).toInt()
                                    val heightPx = image.height.dp(context).toInt()

                                    val drawable = templateConfig.getAsset<AdaptyViewConfiguration.Asset.Image>(image.imageId)
                                        .getBitmap(widthPx, heightPx, AdaptyViewConfiguration.Asset.Image.ScaleType.FIT_MAX)?.let { bitmap ->
                                            BitmapDrawable(context.resources, bitmap)
                                        } ?: kotlin.run {
                                        log(AdaptyLogLevel.ERROR) {
                                            "$LOG_PREFIX_ERROR $flowKey couldn't get bitmap for assetId ${image.imageId}"
                                        }
                                        SpaceDrawable(spacePx)
                                    }

                                    image.tintColorId?.let { tintId ->
                                        val tintColor = templateConfig.getAsset<AdaptyViewConfiguration.Asset.Color>(tintId).value
                                        drawable.colorFilter = PorterDuffColorFilter(tintColor, PorterDuff.Mode.SRC_IN)
                                    }
                                    drawable.setBounds(0, 0, widthPx, heightPx)

                                    IconBulletSpan(drawable, spacePx, verticalSpacePx)
                                }
                                is Text.Item.BulletedText.TextBullet -> {
                                    val bulletText =
                                        processTextItem(context, templateConfig, bullet.text, textComponent, propertiesBuilder, productPlaceholders)
                                            ?: SpannableString("")

                                    TextBulletSpan(bulletText, spacePx, verticalSpacePx)
                                }
                            }

                            currentStr.setSpan(
                                bulletSpan,
                                0,
                                currentStr.length,
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                            )

                            content.append(currentStr)
                        }

                        is Text.Item.Space -> {
                            val currentReplacementStr = SpannableString(" ")

                            val drawable = SpaceDrawable(item.value.dp(context).toInt())

                            val imageSpan = ImageSpan(drawable, ImageSpan.ALIGN_BASELINE)

                            currentReplacementStr.setSpan(
                                imageSpan,
                                0,
                                currentReplacementStr.length,
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                            )

                            content.append(currentReplacementStr)
                        }

                        is Text.Item.Image -> {
                            val widthPx = item.width.dp(context).toInt()
                            val heightPx = item.height.dp(context).toInt()
                            templateConfig.getAsset<AdaptyViewConfiguration.Asset.Image>(item.imageId)
                                .getBitmap(widthPx, heightPx, AdaptyViewConfiguration.Asset.Image.ScaleType.FIT_MAX)?.let { bitmap ->
                                    val drawable = BitmapDrawable(context.resources, bitmap)
                                    item.tintColorId?.let { tintId ->
                                        val tintColor =
                                            templateConfig.getAsset<AdaptyViewConfiguration.Asset.Color>(tintId).value
                                        drawable.colorFilter = PorterDuffColorFilter(tintColor, PorterDuff.Mode.SRC_IN)
                                    }
                                    drawable.setBounds(0, 0, widthPx, heightPx)

                                    val currentReplacementStr = SpannableString(" ")

                                    val imageSpan = ImageSpan(drawable, ImageSpan.ALIGN_BASELINE)

                                    currentReplacementStr.setSpan(
                                        imageSpan,
                                        0,
                                        currentReplacementStr.length,
                                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                                    )

                                    content.append(currentReplacementStr)
                                }
                        }
                    }
                }
                propertiesBuilder.text = content

                return propertiesBuilder.build()
            }
        }
    }

    private fun replaceProductPlaceholders(str: String, productPlaceholders: List<ProductPlaceholderContentData>, paint: TextPaint): String {
        var str = str
        productPlaceholders.forEach { data ->
            str = when (data) {
                is ProductPlaceholderContentData.Simple -> str.replace(data.placeholder, data.value)
                is ProductPlaceholderContentData.Extended -> str.replace(
                    data.placeholder,
                    replaceCurrencyCodeWithSymbol(
                        data.value,
                        data.currencyCode,
                        data.currencySymbol,
                        paint,
                    )
                )
                is ProductPlaceholderContentData.Drop -> if (str.contains(data.placeholder)) "" else str
            }
        }
        return str
    }

    private fun replaceCurrencyCodeWithSymbol(
        text: String,
        currencyCode: String,
        currencySymbol: String,
        paint: Paint,
    ): String {
        return when {
            text.contains(currencyCode, true)
                    && currencyCode.isNotBlank()
                    && !currencyCode.equals(currencySymbol, true)
                    && currencySymbol.all { paint.hasGlyphCompat(it.toString()) } -> {
                text.replace(currencyCode, currencySymbol, true)
            }
            else -> text
        }
    }

    private fun Paint.hasGlyphCompat(string: String): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return hasGlyph(string)
        }
        val missingSymbol = "\uD809\uDCAB".toCharArray()
        val missingSymbolBounds = Rect()
            .also { bounds -> getTextBounds(missingSymbol, 0, missingSymbol.size, bounds) }

        val testSymbol = string.toCharArray()
        val testSymbolBounds = Rect()
            .also { bounds -> getTextBounds(testSymbol, 0, testSymbol.size, bounds) }
        return testSymbolBounds != missingSymbolBounds
    }

    private fun processTextItem(
        context: Context,
        templateConfig: TemplateConfig,
        item: Text.Item.Text,
        textComponent: Text,
        textPropertiesBuilder: TextProperties.Builder,
        productPlaceholders: List<ProductPlaceholderContentData>?,
    ): SpannableString? {

        val font = templateConfig.getAsset<AdaptyViewConfiguration.Asset.Font>(item.fontId)

        val typeface = TypefaceHolder.getOrPut(context, font.value, font.style)

        val currentStr = templateConfig.getString(item.stringId)
            ?.let { str ->
                if (productPlaceholders != null) {
                    val paint = TextPaint().apply {
                        setTypeface(typeface)
                    }
                    SpannableString(replaceProductPlaceholders(str, productPlaceholders, paint))
                } else {
                    SpannableString(str)
                }
            }

        if (currentStr.isNullOrEmpty()) return null

        val spans = mutableListOf<Any>()

        spans.add(CustomTypefaceSpan(typeface))

        val textColor = font.color ?: (item.textColorId?.let { textColorId ->
            templateConfig.getAsset<AdaptyViewConfiguration.Asset.Color>(textColorId)
        }?.value)

        if (textColor != null)
            spans.add(ForegroundColorSpan(textColor))

        val currentTextSize = item.size ?: font.size
        if (currentTextSize != null) {
            textPropertiesBuilder.textSize?.let { baseTextSize ->
                (currentTextSize / baseTextSize).takeIf { it != 1f }?.let { proportion ->
                    spans.add(
                        RelativeSizeSpan(proportion)
                    )
                }
            } ?: kotlin.run {
                textPropertiesBuilder.textSize = currentTextSize
            }
        }

        item.horizontalAlign.takeIf { it != textComponent.horizontalAlign }?.let { align ->
            spans.add(
                AlignmentSpan.Standard(align.toLayoutAlignment())
            )
        }

        spans.forEach { span ->
            currentStr.setSpan(
                span,
                0,
                currentStr.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        return currentStr
    }
}