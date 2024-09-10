package com.adapty.ui.internal

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.BitmapDrawable
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.AlignmentSpan
import android.text.style.ForegroundColorSpan
import android.text.style.ImageSpan
import android.text.style.RelativeSizeSpan
import androidx.annotation.RestrictTo
import com.adapty.ui.AdaptyUI
import com.adapty.ui.AdaptyUI.ViewConfiguration.Component.Text
import com.adapty.ui.listeners.AdaptyUiTagResolver
import com.adapty.utils.AdaptyLogLevel

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class TextComponentHelper(
    private val flowKey: String,
    private val bitmapHelper: BitmapHelper,
) {

    fun processTextComponent(
        context: Context,
        textComponent: Text,
        templateConfig: TemplateConfig,
        tagResolver: AdaptyUiTagResolver,
        productPlaceholders: List<ProductPlaceholderContentData>? = null
    ): TextProperties {
        val propertiesBuilder = TextProperties.Builder()
        propertiesBuilder.horizontalGravity = textComponent.horizontalAlign.toGravity()

        when (textComponent) {
            is Text.Single -> {
                propertiesBuilder.isMultiple = false

                val font = templateConfig.getAsset<AdaptyUI.ViewConfiguration.Asset.Font>(textComponent.fontId)

                propertiesBuilder.typeface = TypefaceHolder.getOrPut(context, font)

                val content = templateConfig.getString(textComponent.stringId, tagResolver)
                    ?.let { str ->
                        if (productPlaceholders != null) {
                            replaceProductPlaceholders(str, productPlaceholders)
                        } else {
                            str
                        }
                    }
                propertiesBuilder.text = content

                propertiesBuilder.textSize = textComponent.size ?: font.size

                val textColorFromComponent = textComponent.textColorId?.let { assetId ->
                    templateConfig.getAsset<AdaptyUI.ViewConfiguration.Asset.Color>(assetId)
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
                                processTextItem(context, templateConfig, item, textComponent, propertiesBuilder, tagResolver, productPlaceholders) ?: return@forEach
                            content.append(currentStr)
                        }

                        is Text.Item.NewLine -> content.append("\n")

                        is Text.Item.BulletedText -> {
                            val textPart = item.text
                            val spacePart = item.space

                            val currentStr =
                                processTextItem(context, templateConfig, textPart, textComponent, propertiesBuilder, tagResolver, productPlaceholders) ?: return@forEach

                            val spacePx = spacePart?.value?.dp(context)?.toInt() ?: 0
                            val verticalSpacePx = templateConfig.featureSpacing.dp(context).toInt()

                            val bulletSpan = when (val bullet = item.bullet) {
                                is Text.Item.BulletedText.ImageBullet -> {
                                    val image = bullet.image

                                    val widthPx = image.width.dp(context).toInt()
                                    val heightPx = image.height.dp(context).toInt()

                                    val drawable = bitmapHelper.getBitmap(
                                        templateConfig.getAsset(image.imageId),
                                        widthPx,
                                        heightPx,
                                        AdaptyUI.ViewConfiguration.Asset.Image.ScaleType.FIT_MAX,
                                    )?.let { bitmap ->
                                            BitmapDrawable(context.resources, bitmap)
                                        } ?: kotlin.run {
                                        log(AdaptyLogLevel.ERROR) {
                                            "$LOG_PREFIX_ERROR $flowKey couldn't get bitmap for assetId ${image.imageId}"
                                        }
                                        SpaceDrawable(spacePx)
                                    }

                                    image.tintColorId?.let { tintId ->
                                        val tintColor = templateConfig.getAsset<AdaptyUI.ViewConfiguration.Asset.Color>(tintId).value
                                        drawable.colorFilter = PorterDuffColorFilter(tintColor, PorterDuff.Mode.SRC_IN)
                                    }
                                    drawable.setBounds(0, 0, widthPx, heightPx)

                                    IconBulletSpan(drawable, spacePx, verticalSpacePx)
                                }
                                is Text.Item.BulletedText.TextBullet -> {
                                    val bulletText =
                                        processTextItem(context, templateConfig, bullet.text, textComponent, propertiesBuilder, tagResolver, productPlaceholders)
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
                            bitmapHelper.getBitmap(
                                templateConfig.getAsset(item.imageId),
                                widthPx,
                                heightPx,
                                AdaptyUI.ViewConfiguration.Asset.Image.ScaleType.FIT_MAX
                            )?.let { bitmap ->
                                val drawable = BitmapDrawable(context.resources, bitmap)
                                item.tintColorId?.let { tintId ->
                                    val tintColor =
                                        templateConfig.getAsset<AdaptyUI.ViewConfiguration.Asset.Color>(tintId).value
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

    private fun replaceProductPlaceholders(str: String, productPlaceholders: List<ProductPlaceholderContentData>): String {
        var str = str
        productPlaceholders.forEach { data ->
            str = when (data) {
                is ProductPlaceholderContentData.Simple -> str.replace(data.placeholder, data.value)
                is ProductPlaceholderContentData.Extended -> str.replace(data.placeholder, data.value)
                is ProductPlaceholderContentData.Drop -> if (str.contains(data.placeholder)) "" else str
            }
        }
        return str
    }

    private fun processTextItem(
        context: Context,
        templateConfig: TemplateConfig,
        item: Text.Item.Text,
        textComponent: Text,
        textPropertiesBuilder: TextProperties.Builder,
        tagResolver: AdaptyUiTagResolver,
        productPlaceholders: List<ProductPlaceholderContentData>?,
    ): SpannableString? {

        val font = templateConfig.getAsset<AdaptyUI.ViewConfiguration.Asset.Font>(item.fontId)

        val typeface = TypefaceHolder.getOrPut(context, font)

        val currentStr = templateConfig.getString(item.stringId, tagResolver)
            ?.let { str ->
                if (productPlaceholders != null) {
                    SpannableString(replaceProductPlaceholders(str, productPlaceholders))
                } else {
                    SpannableString(str)
                }
            }

        if (currentStr.isNullOrEmpty()) return null

        val spans = mutableListOf<Any>()

        spans.add(CustomTypefaceSpan(typeface))

        val textColor = font.color ?: (item.textColorId?.let { textColorId ->
            templateConfig.getAsset<AdaptyUI.ViewConfiguration.Asset.Color>(textColorId)
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