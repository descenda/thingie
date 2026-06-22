package org.cycb.canvas.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

data class LinkMetadata(
    val url: String,
    val title: String? = null,
    val description: String? = null,
    val image: String? = null,
    val siteName: String? = null
)

class LinkPreviewViewModel : ViewModel() {
    private val _metadataCache = MutableStateFlow<Map<String, LinkMetadata>>(emptyMap())
    val metadataCache: StateFlow<Map<String, LinkMetadata>> = _metadataCache

    fun fetchMetadata(url: String) {
        if (_metadataCache.value.containsKey(url)) return

        viewModelScope.launch {
            try {
                val metadata = withContext(Dispatchers.IO) {
                    val doc = Jsoup.connect(url)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                        .get()
                    
                    val title = doc.select("meta[property=og:title]").attr("content").ifEmpty { doc.title() }
                    val description = doc.select("meta[property=og:description]").attr("content").ifEmpty {
                        doc.select("meta[name=description]").attr("content")
                    }
                    val image = doc.select("meta[property=og:image]").attr("content")
                    val siteName = doc.select("meta[property=og:site_name]").attr("content")

                    LinkMetadata(url, title, description, image, siteName)
                }
                _metadataCache.value = _metadataCache.value + (url to metadata)
            } catch (e: Exception) {
                // Ignore errors for now
            }
        }
    }
}
