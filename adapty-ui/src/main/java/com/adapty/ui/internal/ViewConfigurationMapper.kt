@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal

import android.graphics.Color
import androidx.annotation.ColorInt
import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.internal.utils.adaptyError
import com.adapty.models.AdaptyPaywall
import com.adapty.ui.AdaptyUI.ViewConfiguration
import com.adapty.ui.AdaptyUI.ViewConfiguration.*
import kotlin.math.abs

private typealias JsonObject = Map<String, Any>
private typealias JsonArray = Iterable<JsonObject>

internal class ViewConfigurationMapper {

    private fun <T> JsonObject.safeGet(key: String) = runCatching { get(key) as T }.getOrNull()

    private companion object {
        const val DATA = "data"
        const val PAYWALL_BUILDER_ID = "paywall_builder_id"
        const val PAYWALL_BUILDER_CONFIG = "paywall_builder_config"
        const val IS_HARD_PAYWALL = "is_hard_paywall"
        const val TEMPLATE_ID = "template_id"
        const val DEFAULT_LOCALIZATION = "default_localization"
        const val MAIN_IMAGE_RELATIVE_HEIGHT = "main_image_relative_height"
        const val ASSETS = "assets"
        const val LOCALIZATIONS = "localizations"

        const val ID = "id"
        const val TYPE = "type"
        const val VALUE = "value"
        const val VALUES = "values"
        const val POINTS = "points"
        const val URL = "url"
        const val PREVIEW_VALUE = "preview_value"

        const val FAMILY_NAME = "family_name"
        const val RESOURCES = "resources"
        const val WEIGHT = "weight"
        const val IS_ITALIC = "italic"
        const val SIZE = "size"
        const val COLOR = "color"
        const val HORIZONTAL_ALIGN = "horizontal_align"

        const val STRINGS = "strings"
        const val FALLBACK = "fallback"
        const val HAS_TAGS = "has_tags"

        const val STYLES = "styles"
    }

    fun map(data: JsonObject, paywall: AdaptyPaywall): ViewConfiguration {
        val data = data.safeGet<JsonObject>(DATA) ?: data
        val id = data.safeGet<String>(PAYWALL_BUILDER_ID) ?: throw adaptyError(
            message = "id in ViewConfiguration should not be null",
            adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
        )
        val config = data.safeGet<JsonObject>(PAYWALL_BUILDER_CONFIG) ?: throw adaptyError(
            message = "config in ViewConfiguration should not be null",
            adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
        )

        return ViewConfiguration(
            id = id,
            paywall = paywall,
            isHard = config.safeGet(IS_HARD_PAYWALL) ?: false,
            templateId = config.safeGet(TEMPLATE_ID),
            defaultLocalization = config.safeGet(DEFAULT_LOCALIZATION),
            mainImageRelativeHeight = config.safeGet<Number>(MAIN_IMAGE_RELATIVE_HEIGHT)?.toFloat() ?: 1f,
            assets = mapVisualAssets(config.safeGet(ASSETS)),
            localizations = config.safeGet<JsonArray>(LOCALIZATIONS)?.associate { localization ->
                val localizationId = localization.safeGet<String>(ID) ?: throw adaptyError(
                    message = "id in Localization should not be null",
                    adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                )

                val strings = localization.safeGet<JsonArray>(STRINGS)?.associate { str ->
                    val strId = str.safeGet<String>(ID)
                    val strValue = str.safeGet<String>(VALUE)
                    if (strId == null || strValue == null) {
                        throw adaptyError(
                            message = "id and value in strings in Localization should not be null",
                            adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                        )
                    }

                    strId to Localization.Str(strValue, str.safeGet(FALLBACK), str.safeGet(HAS_TAGS) ?: false)
                }.orEmpty()

                val assets = mapVisualAssets(localization.safeGet(ASSETS))

                localizationId to Localization(strings, assets)
            }.orEmpty(),
            styles = config.safeGet<JsonObject>(STYLES)?.mapValues { (_, styleValue) ->
                (styleValue as? Map<*, *>)?.let(::mapStyle)
            }.orEmpty(),
        )
    }

    private fun mapStyle(value: Map<*, *>): Style {
        val value = value.toMutableMap()

        val productBlock = (value.remove("products_block") as? Map<*, *>)?.let { productBlock ->
            val type = when (val typeStr = productBlock["type"]) {
                "horizontal" -> ProductBlock.Type.HORIZONTAL
                "single" -> ProductBlock.Type.SINGLE
                "vertical" -> ProductBlock.Type.VERTICAL
                else -> throw adaptyError(
                    message = "Unsupported type (\"$typeStr\") in products_block",
                    adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                )
            }

            val mainProductIndex = (productBlock["main_product_index"] as? Number)?.toInt()?.coerceAtLeast(0) ?: 0
            val initiatePurchaseOnTap = (productBlock["initiate_purchase_on_tap"] as? Boolean) ?: false

            val products =
                (productBlock["products"] as? List<*>)
                    ?.mapNotNull { product ->
                        val productObject = (product as? Map<*, *>)?.let(::mapProductObjectComponent)
                        productObject?.let { productObject.productId to productObject }
                    }
                    ?.toMap()
                    ?: throw adaptyError(
                        message = "products in ProductBlock should not be null",
                        adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                    )

            ProductBlock(type, mainProductIndex, initiatePurchaseOnTap, products)
        } ?: throw adaptyError(
            message = "products_block in style should not be null",
            adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
        )

        val featureBlock = (value.remove("features_block") as? Map<*, *>)?.let { featureBlock ->
            val type = when (val typeStr = featureBlock["type"]) {
                "list" -> FeatureBlock.Type.LIST
                "timeline" -> FeatureBlock.Type.TIMELINE
                else -> throw adaptyError(
                    message = "Unsupported type (\"$typeStr\") in features_block",
                    adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                )
            }

            val orderedItems = featureBlock.values.filterIsInstance<Map<*, *>>().sortedWith(Comparator { firstMap, secondMap ->
                compareByOrder(firstMap, secondMap)
            }).mapNotNull(::mapVisualStyleComponent)

            FeatureBlock(type, orderedItems)
        }

        val footerBlock = (value.remove("footer_block") as? Map<*, *>)?.let { footerBlock ->
            val orderedItems = footerBlock.values.filterIsInstance<Map<*, *>>().sortedWith(Comparator { firstMap, secondMap ->
                compareByOrder(firstMap, secondMap)
            }).mapNotNull(::mapVisualStyleComponent)

            FooterBlock(orderedItems)
        }

        val items = value.mapNotNull { (k, v) ->
            when {
                k !is String -> null
                v == null -> null
                else -> {
                    mapVisualStyleComponent(v)?.let { mappedValue -> k to mappedValue }
                }
            }
        }.toMap()

        return Style(featureBlock, productBlock, footerBlock, items)
    }

    private fun mapVisualStyleComponent(value: Any?): Component? =
        when (value) {
            is String -> Component.Reference(value)
            is Map<*, *> -> {
                when(value["type"]) {
                    "shape", "rectangle", "rect", "circle", "curve_up", "curve_down" -> mapShapeComponent(value)
                    "text" -> mapTextComponent(value)
                    "button" -> mapButtonComponent(value)
                    "product" -> mapProductObjectComponent(value)
                    else -> mapCustomObjectComponent(value)
                }
            }
            else -> null
        }

    private fun mapShapeComponent(map: Map<*, *>): Component.Shape {
        val rawType = map["type"]?.takeIf { it != "shape" } ?: map["value"]

        val type = when(rawType) {
            "circle" -> Component.Shape.Type.Circle
            "curve_up" -> Component.Shape.Type.RectWithArc(Component.Shape.Type.RectWithArc.ABS_ARC_HEIGHT)
            "curve_down" -> Component.Shape.Type.RectWithArc(-Component.Shape.Type.RectWithArc.ABS_ARC_HEIGHT)
            else -> {
                val cornerRadius = map["rect_corner_radius"]
                Component.Shape.Type.Rectangle(
                    when(cornerRadius) {
                        is Number -> Component.Shape.CornerRadius.Same(cornerRadius.toFloat())
                        is List<*> -> {
                            when {
                                cornerRadius.isEmpty() -> Component.Shape.CornerRadius.None
                                cornerRadius.size == 1 -> Component.Shape.CornerRadius.Same((cornerRadius[0] as? Number)?.toFloat() ?: 0f)
                                else -> {
                                    val tl = (cornerRadius.getOrNull(0) as? Number)?.toFloat() ?: 0f
                                    val tr = (cornerRadius.getOrNull(1) as? Number)?.toFloat() ?: 0f
                                    val br = (cornerRadius.getOrNull(2) as? Number)?.toFloat() ?: 0f
                                    val bl = (cornerRadius.getOrNull(3) as? Number)?.toFloat() ?: 0f

                                    Component.Shape.CornerRadius.Different(tl, tr, br, bl)
                                }
                            }
                        }
                        is Map<*, *> -> {
                            when {
                                cornerRadius.isEmpty() -> Component.Shape.CornerRadius.None
                                else -> {
                                    val tl = (cornerRadius["tl"] as? Number)?.toFloat() ?: 0f
                                    val tr = (cornerRadius["tr"] as? Number)?.toFloat() ?: 0f
                                    val br = (cornerRadius["br"] as? Number)?.toFloat() ?: 0f
                                    val bl = (cornerRadius["bl"] as? Number)?.toFloat() ?: 0f

                                    Component.Shape.CornerRadius.Different(tl, tr, br, bl)
                                }
                            }
                        }
                        else -> Component.Shape.CornerRadius.None
                    }
                )
            }
        }

        return Component.Shape(
            backgroundAssetId = map["background"] as? String,
            type = type,
            (map["border"] as? String)?.let { assetId ->
                Component.Shape.Border(assetId, (map["thickness"] as? Number)?.toFloat() ?: 0f)
            }
        )
    }

    private fun mapTextComponent(value: Map<*, *>) : Component.Text {
        val size = (value["size"] as? Number)?.toFloat()
        val textColorId = value["color"] as? String
        val fontId = value["font"] as? String
        val horizontalAlign = mapHorizontalAlign(value["horizontal_align"] as? String)
        val bulletSpace = (value["bullet_space"] as? Number)?.toFloat()

        var currentBullet: Component.Text.Item.BulletedText.Bullet? = null
        var spaceForCurrentBullet: Component.Text.Item.Space? = null
        val items = (value["items"] as? List<*>)?.mapIndexedNotNull { i, item ->
            (item as? Map<*, *>)?.let { map ->
                if (map["newline"] != null) {
                    currentBullet = null
                    spaceForCurrentBullet = null
                    return@mapIndexedNotNull Component.Text.Item.NewLine
                }

                val space = (map["space"] as? Number)?.toFloat()

                if (space != null) {
                    val space = Component.Text.Item.Space(space)

                    if (currentBullet == null) {
                        return@mapIndexedNotNull space
                    } else {
                        spaceForCurrentBullet = space
                        return@mapIndexedNotNull null
                    }
                }

                val stringId = map["string_id"] as? String

                if (stringId != null) {
                    val text = Component.Text.Item.Text(
                        stringId,
                        ((map["font"] as? String) ?: fontId) ?: throw adaptyError(
                            message = "fontId in TextItem should not be null",
                            adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                        ),
                        (map["size"] as? Number)?.toFloat() ?: size,
                        (map["color"] as? String) ?: textColorId,
                        (map["horizontal_align"] as? String)?.let(::mapHorizontalAlign) ?: horizontalAlign,
                    )

                    if ((map["bullet"] as? Boolean) == true) {
                        currentBullet = Component.Text.Item.BulletedText.TextBullet(text)
                        return@mapIndexedNotNull Component.Text.Item.NewLine.takeIf { i > 0 }
                    } else {
                        val bullet = currentBullet
                        if (bullet != null) {
                            val bulletedText = Component.Text.Item.BulletedText(
                                bullet,
                                spaceForCurrentBullet ?: bulletSpace?.let { bulletSpace -> Component.Text.Item.Space(bulletSpace) },
                                text
                            )
                            currentBullet = null
                            spaceForCurrentBullet = null
                            return@mapIndexedNotNull bulletedText
                        } else {
                            return@mapIndexedNotNull text
                        }
                    }
                }

                val imageId = map["image"] as? String

                if (imageId != null) {
                    val image = Component.Text.Item.Image(
                        imageId,
                        map["color"] as? String,
                        (map["width"] as? Number)?.toFloat() ?: throw adaptyError(
                            message = "width in ImageItem should not be null",
                            adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                        ),
                        (map["height"] as? Number)?.toFloat() ?: throw adaptyError(
                            message = "height in ImageItem should not be null",
                            adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                        ),
                    )

                    if ((map["bullet"] as? Boolean) == true) {
                        currentBullet = Component.Text.Item.BulletedText.ImageBullet(image)
                        return@mapIndexedNotNull Component.Text.Item.NewLine.takeIf { i > 0 }
                    } else {
                        return@mapIndexedNotNull image
                    }
                }

                return@mapIndexedNotNull null
            }
        }

        return if (items != null) {
            Component.Text.Multiple(items, horizontalAlign)
        } else {
            Component.Text.Single(
                (value["string_id"] as? String) ?: throw adaptyError(
                    message = "stringId in Text should not be null",
                    adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                ),
                fontId ?: throw adaptyError(
                    message = "font in Text should not be null",
                    adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                ),
                size,
                textColorId,
                horizontalAlign,
            )
        }
    }

    private fun mapButtonComponent(value: Map<*, *>) : Component.Button {
        return Component.Button(
            (value["shape"] as? Map<*, *>)?.let(::mapShapeComponent),
            (value["selected_shape"] as? Map<*, *>)?.let(::mapShapeComponent),
            (value["title"] as? Map<*, *>)?.let(::mapTextComponent),
            (value["selected_title"] as? Map<*, *>)?.let(::mapTextComponent),
            mapButtonAlign(value["align"] as? String),
            (value["action"] as? Map<*, *>)?.let(::mapButtonAction),
            (value["visibility"] as? Boolean) ?: true,
            (value["transition_in"] as? Map<*, *>)?.let(::mapButtonTransition),
        )
    }

    private fun mapProductObjectComponent(value: Map<*, *>) : Component.ProductObject {
        val productId = (value["product_id"] as? String) ?: throw adaptyError(
            message = "productId in Product should not be null",
            adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
        )

        val properties = value.mapNotNull { (k, v) ->
            when (k) {
                !is String -> null
                "type", "order", "product_id" -> null
                else -> mapVisualStyleComponent(v)?.let { k to it }
            }
        }.toMap()

        return Component.ProductObject(productId, properties)
    }

    private fun mapCustomObjectComponent(value: Map<*, *>) : Component.CustomObject? {
        val type = value["type"] as? String ?: return null

        val properties = value.mapNotNull { (k, v) ->
            when (k) {
                !is String -> null
                "type", "order" -> null
                else -> k to v
            }
        }.sortedWith(Comparator { (_, firstValue), (_, secondValue) ->
            compareByOrder(firstValue, secondValue)
        }).mapNotNull { (k, v) -> mapVisualStyleComponent(v)?.let { k to it }  }

        return Component.CustomObject(type, properties)
    }

    private fun compareByOrder(firstMap: Any?, secondMap: Any?): Int {
        val firstOrder = ((firstMap as? Map<*, *>)?.get("order") as? Number)?.toInt() ?: return 0
        val secondOrder = ((secondMap as? Map<*, *>)?.get("order") as? Number)?.toInt() ?: return 0
        val diff = firstOrder - secondOrder
        return if (diff == 0) 0 else diff / abs(diff)
    }

    private fun mapHorizontalAlign(value: String?) =
        when (value) {
            "center" -> HorizontalAlign.CENTER
            "right" -> HorizontalAlign.RIGHT
            else -> HorizontalAlign.LEFT
        }

    private fun mapButtonAlign(value: String?) =
        when (value) {
            "leading" -> Component.Button.Align.LEADING
            "trailing" -> Component.Button.Align.TRAILING
            "fill" -> Component.Button.Align.FILL
            else -> Component.Button.Align.CENTER
        }

    private fun mapButtonAction(value: Map<*, *>): Component.Button.Action? {
        return when (val type = value["type"]) {
            "open_url" -> Component.Button.Action.OpenUrl(
                (value["url"] as? String) ?: throw adaptyError(
                    message = "url value should not be null when type is open_url",
                    adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                )
            )
            "restore" -> Component.Button.Action.Restore
            "close" -> Component.Button.Action.Close
            "custom" -> Component.Button.Action.Custom(
                (value["custom_id"] as? String) ?: throw adaptyError(
                    message = "custom_id value should not be null when type is custom",
                    adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                )
            )
            else -> null
        }
    }

    private fun mapButtonTransition(value: Map<*, *>): Component.Button.Transition? {
        return when (value["type"]) {
            "fade" -> Component.Button.Transition.Fade(
                (value["duration"] as? Number)?.toLong() ?: 300L,
                (value["start_delay"] as? Number)?.toLong() ?: 0L,
                (value["interpolator"] as? String) ?: "ease_in_out"
            )
            else -> null
        }
    }

    private fun mapVisualAssets(assets: JsonArray?): Map<String, Asset> {
        return assets?.mapNotNull { asset ->
            val assetId = asset.safeGet<String>(ID)
            val type = asset.safeGet<String>(TYPE)
            val value = asset.safeGet<String>(VALUE)

            if (assetId != null && type != null) {
                (when (type) {
                    "image" -> {
                        val url = asset.safeGet<String>(URL)
                        if (url != null)
                            Asset.RemoteImage(
                                url,
                                asset.safeGet<String>(PREVIEW_VALUE)?.let { preview ->
                                    Asset.Image(source = Asset.Image.Source.Base64Str(preview))
                                }
                            )
                        else
                            Asset.Image(source = Asset.Image.Source.Base64Str(value))
                    }
                    "color" -> Asset.Color(
                        value?.let(::mapVisualAssetColorString)
                            ?: throw adaptyError(
                                message = "color value should not be null",
                                adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                            )
                    )
                    "linear-gradient", "radial-gradient", "conic-gradient" -> Asset.Gradient(
                        when (type) {
                            "radial-gradient" -> Asset.Gradient.Type.RADIAL
                            "conic-gradient" -> Asset.Gradient.Type.CONIC
                            else -> Asset.Gradient.Type.LINEAR
                        },
                        (asset.safeGet<List<*>>(VALUES))?.mapNotNull {
                            (it as? Map<*, *>)?.let { value ->
                                val p = (value["p"] as? Number)?.toFloat() ?: return@mapNotNull null
                                val color = Asset.Color((value["color"] as? String)?.let(::mapVisualAssetColorString) ?: return@mapNotNull null)

                                Asset.Gradient.Value(p, color)
                            }
                        } ?: throw adaptyError(
                            message = "gradient values should not be null",
                            adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                        ),
                        asset.safeGet<Map<*, *>>(POINTS)?.let {
                            val x0 = (it["x0"] as? Number)?.toFloat() ?: return@let null
                            val y0 = (it["y0"] as? Number)?.toFloat() ?: return@let null
                            val x1 = (it["x1"] as? Number)?.toFloat() ?: return@let null
                            val y1 = (it["y1"] as? Number)?.toFloat() ?: return@let null

                            Asset.Gradient.Points(x0, y0, x1, y1)
                        } ?: throw adaptyError(
                            message = "gradient points should not be null",
                            adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                        ),
                    )
                    "font" -> Asset.Font(
                        asset.safeGet(FAMILY_NAME) ?: "adapty_system",
                        asset.safeGet<Iterable<String>>(RESOURCES)?.toList() ?: emptyList(),
                        asset.safeGet<Number>(WEIGHT)?.toInt() ?: 400,
                        asset.safeGet(IS_ITALIC) ?: false,
                        asset.safeGet<Number>(SIZE)?.toFloat(),
                        mapHorizontalAlign(asset.safeGet(HORIZONTAL_ALIGN)),
                        asset.safeGet<String?>(COLOR)?.let(::mapVisualAssetColorString),
                    )
                    else -> null
                })?.let { assetId to it }
            } else
                null
        }?.toMap().orEmpty()
    }

    @ColorInt
    private fun mapVisualAssetColorString(colorString: String): Int {
        return try {
            Color.parseColor(
                when (colorString.length) {
                    9 -> rgbaToArgbStr(colorString)
                    else -> colorString
                }
            )
        } catch (e: Exception) {
            throw adaptyError(
                message = "color value should be a valid #RRGGBB or #RRGGBBAA",
                adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED,
                originalError = e
            )
        }
    }

    private fun rgbaToArgbStr(rgbaColorString: String): String {
        return rgbaColorString.toCharArray().let { chars ->
            val a1 = chars[7]
            val a2 = chars[8]
            for (i in 8 downTo 3) {
                chars[i] = chars[i - 2]
            }
            chars[1] = a1
            chars[2] = a2
            String(chars)
        }
    }

    fun mapToMediaUrls(data: JsonObject): Pair<String, Set<String>> {
        val id = data.safeGet<String>(PAYWALL_BUILDER_ID) ?: throw adaptyError(
            message = "id in ViewConfiguration should not be null",
            adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
        )
        val config = data.safeGet<JsonObject>(PAYWALL_BUILDER_CONFIG) ?: return id to emptySet()
        val mediaUrls = mutableSetOf<String>()
        config.safeGet<JsonArray>(ASSETS)?.let { assets ->
            mediaUrls += findMediaUrls(assets)
        }
        config.safeGet<JsonArray>(LOCALIZATIONS)?.forEach { localization ->
            localization.safeGet<JsonArray>(ASSETS)?.let { assets ->
                mediaUrls += findMediaUrls(assets)
            }
        }
        return id to mediaUrls
    }

    private fun findMediaUrls(assets: JsonArray): Set<String> {
        val mediaUrls = mutableSetOf<String>()
        assets.forEach { asset ->
            if (asset.safeGet<String>(TYPE) == "image") {
                asset.safeGet<String>(URL)?.let { url -> mediaUrls.add(url) }
            }
        }
        return mediaUrls
    }
}