package com.adapty.ui.internal.cache

import androidx.annotation.RestrictTo
import com.adapty.ui.AdaptyUI
import com.adapty.ui.AdaptyUI.ViewConfiguration.Asset.Image
import com.adapty.ui.internal.LOG_PREFIX
import com.adapty.ui.internal.log
import com.adapty.utils.AdaptyLogLevel.Companion.VERBOSE

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class MediaFetchService(
    private val singleMediaHandlerFactory: SingleMediaHandlerFactory,
) {

    fun preloadMedia(configId: String, urls: Collection<String>) {
        log(VERBOSE) { "$LOG_PREFIX #AdaptyMediaCache# preloading media from config with id: $configId" }
        urls.forEach { url ->
            load(url)
        }
    }

    private fun load(url: String) {
        val handler = singleMediaHandlerFactory.get(url)
        handler.loadMedia()
    }

    fun loadImage(
        remoteImage: AdaptyUI.ViewConfiguration.Asset.RemoteImage,
        handlePreview: ((preview: Image) -> Unit)?,
        handleResult: ((image: Image) -> Unit)?,
    ) {
        remoteImage.preview?.let { preview -> handlePreview?.invoke(preview) }

        val handler = singleMediaHandlerFactory.get(remoteImage.url)

        handler.loadMedia(
            onResult = { result ->
                result.mapCatching { file ->
                    handleResult?.invoke(Image(source = Image.Source.File(file)))
                }
            },
        )
    }
}