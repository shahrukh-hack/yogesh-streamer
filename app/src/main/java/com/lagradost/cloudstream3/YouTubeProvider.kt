package com.lagradost.cloudstream3

import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.extractors.YoutubeExtractor
import com.lagradost.cloudstream3.utils.AppUtils.tryParseJson
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.mvvm.logError
import com.fasterxml.jackson.annotation.JsonProperty

class YouTubeProvider : MainAPI() {
    override var name = "YouTube"
    override var mainUrl = "https://pipedapi.kavin.rocks"
    override val hasMainPage = true
    override val hasQuickSearch = true
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries, TvType.Live)

    override val mainPage = mainPageOf(
        "trending?region=US" to "Trending Now",
        "trending?region=IN" to "Popular in India",
        "trending?region=GB" to "Top Global",
        "trending?region=CA" to "Entertainment"
    )

    data class PipedStreamItem(
        @JsonProperty("url") val url: String? = null,
        @JsonProperty("title") val title: String? = null,
        @JsonProperty("thumbnail") val thumbnail: String? = null,
        @JsonProperty("uploaderName") val uploaderName: String? = null,
        @JsonProperty("duration") val duration: Long? = null,
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse? {
        val instances = listOf(
            "https://pipedapi.kavin.rocks",
            "https://api.piped.privacy.com.de",
            "https://piped-api.garudalinux.org",
            "https://api.piped.yt"
        )
        
        for (base in instances) {
            try {
                val fullUrl = "$base/${request.data}"
                val response = app.get(fullUrl, timeout = 10).text
                val items = tryParseJson<List<PipedStreamItem>>(response) ?: continue
                if (items.isNotEmpty()) {
                    val searchResults = items.filter { !it.url.isNullOrBlank() && !it.title.isNullOrBlank() }.map { item ->
                        val videoId = item.url?.substringAfter("watch?v=").orEmpty()
                        newMovieSearchResponse(
                            name = item.title ?: "YouTube Video",
                            url = "https://www.youtube.com/watch?v=$videoId",
                            type = TvType.Movie
                        ) {
                            this.posterUrl = item.thumbnail
                        }
                    }
                    return newHomePageResponse(request.name, searchResults, false)
                }
            } catch (e: Exception) {
                logError(e)
            }
        }
        return null
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val instances = listOf(
            "https://pipedapi.kavin.rocks",
            "https://api.piped.privacy.com.de",
            "https://piped-api.garudalinux.org",
            "https://api.piped.yt"
        )

        for (base in instances) {
            try {
                val fullUrl = "$base/search?q=$query&filter=videos"
                val response = app.get(fullUrl, timeout = 10).text
                val items = tryParseJson<Map<String, Any>>(response)
                val itemsList = tryParseJson<List<PipedStreamItem>>(
                    com.lagradost.cloudstream3.utils.AppUtils.toJson(items?.get("items") ?: emptyList<Any>())
                ) ?: continue

                if (itemsList.isNotEmpty()) {
                    return itemsList.filter { !it.url.isNullOrBlank() && !it.title.isNullOrBlank() }.map { item ->
                        val videoId = item.url?.substringAfter("watch?v=").orEmpty()
                        newMovieSearchResponse(
                            name = item.title ?: "YouTube Video",
                            url = "https://www.youtube.com/watch?v=$videoId",
                            type = TvType.Movie
                        ) {
                            this.posterUrl = item.thumbnail
                        }
                    }
                }
            } catch (e: Exception) {
                logError(e)
            }
        }
        return emptyList()
    }

    override suspend fun load(url: String): LoadResponse {
        val videoId = url.substringAfter("watch?v=").substringBefore("&").substringAfter("youtu.be/").substringBefore("?")
        return newMovieLoadResponse(
            name = "YouTube Video",
            url = url,
            type = TvType.Movie,
            dataUrl = url
        )
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val extractor = YoutubeExtractor()
        extractor.getUrl(data, null, subtitleCallback, callback)
        return true
    }
}
