package com.lagradost.cloudstream3

import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.extractors.YoutubeExtractor
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.mvvm.logError
import org.jsoup.Jsoup
import org.jsoup.parser.Parser

class YouTubeProvider : MainAPI() {
    override var name = "YouTube"
    override var mainUrl = "https://www.youtube.com"
    override val hasMainPage = true
    override val hasQuickSearch = true
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries, TvType.Live)

    override val mainPage = mainPageOf(
        "UCq-Fj5jknLsUf-MWSy4_brA" to "Trending Bollywood & Music (T-Series)",
        "UCFFbwnve3yF62-tVXkTyHqg" to "Hit Songs & Soundtracks (Zee Music)",
        "UC3gNmTGu-TTbFPpfSs5kNkg" to "Blockbuster Cinema Trailers",
        "UCAuUUnT6oDeKwE6v1NGQxug" to "Cricket Highlights & Moments (ICC)",
        "UCX6OQ3DkcsbYNE6H8uQQuVA" to "Global Trending Entertainment",
        "UCbTLwN10NoCU4WDzLf1JMOA" to "YRF Bollywood Specials"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse? {
        val channelId = request.data
        val feedUrl = "https://www.youtube.com/feeds/videos.xml?channel_id=$channelId"
        return try {
            val response = app.get(feedUrl, timeout = 15).text
            val doc = Jsoup.parse(response, "", Parser.xmlParser())
            val entries = doc.select("entry")
            val searchResults = entries.mapNotNull { entry ->
                val videoId = entry.selectFirst("yt|videoId")?.text()
                    ?: entry.selectFirst("videoId")?.text()
                    ?: entry.selectFirst("id")?.text()?.substringAfter("yt:video:")
                val title = entry.selectFirst("title")?.text()
                if (!videoId.isNullOrBlank() && !title.isNullOrBlank()) {
                    newMovieSearchResponse(
                        name = title,
                        url = "https://www.youtube.com/watch?v=$videoId",
                        type = TvType.Movie
                    ) {
                        this.posterUrl = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
                    }
                } else null
            }
            if (searchResults.isNotEmpty()) {
                newHomePageResponse(request.name, searchResults, true)
            } else null
        } catch (e: Exception) {
            logError(e)
            null
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val cleanQuery = query.trim().replace(" ", "+")
        val searchUrl = "https://www.youtube.com/results?search_query=$cleanQuery"
        return try {
            val response = app.get(
                searchUrl,
                headers = mapOf(
                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
                    "Accept-Language" to "en-US,en;q=0.9"
                ),
                timeout = 15
            ).text
            val regex = Regex(""""videoId":"([a-zA-Z0-9_-]{11})".*?"title":\{"runs":\[\{"text":"([^"]+)"""")
            val matches = regex.findAll(response).take(20).toList()
            val results = mutableListOf<SearchResponse>()
            val seenIds = mutableSetOf<String>()
            for (match in matches) {
                val videoId = match.groupValues[1]
                val title = match.groupValues[2]
                if (seenIds.add(videoId)) {
                    results.add(
                        newMovieSearchResponse(
                            name = title,
                            url = "https://www.youtube.com/watch?v=$videoId",
                            type = TvType.Movie
                        ) {
                            this.posterUrl = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
                        }
                    )
                }
            }
            results
        } catch (e: Exception) {
            logError(e)
            emptyList()
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val videoId = url.substringAfter("watch?v=").substringBefore("&").substringAfter("youtu.be/").substringBefore("?")
        return newMovieLoadResponse(
            name = "YouTube Video",
            url = url,
            type = TvType.Movie,
            dataUrl = url
        ) {
            this.posterUrl = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
        }
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

