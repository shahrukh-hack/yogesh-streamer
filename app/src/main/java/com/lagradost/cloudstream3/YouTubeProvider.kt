package com.lagradost.cloudstream3

import com.lagradost.cloudstream3.extractors.YoutubeExtractor
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.SubtitleFile
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.kiosk.KioskInfo
import org.schabi.newpipe.extractor.search.SearchInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem

class YouTubeProvider : MainAPI() {
    override var name = "YouTube"
    override var mainUrl = "https://www.youtube.com"
    override val hasMainPage = true
    override val hasQuickSearch = true
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries, TvType.Live)

    override val mainPage = mainPageOf(
        "Trending" to "Trending Now",
        "Music" to "Music & Hits",
        "Gaming" to "Gaming",
        "News" to "Live & News"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse? {
        val service = ServiceList.YouTube
        val kiosk = when (request.name) {
            "Music & Hits" -> service.kioskList.getListByUrl("https://www.youtube.com/feed/trending?bp=4gINGgt5dG1hX2NoYXJ0cw%3D%3D")
            "Gaming" -> service.kioskList.getListByUrl("https://www.youtube.com/feed/trending?bp=4gIcGhpnYW1pbmdfY29ycHVzX21vc3RfcG9wdWxhcg%3D%3D")
            "Live & News" -> service.kioskList.getListByUrl("https://www.youtube.com/feed/trending?bp=4gIKGghuZXdzX2FsbA%3D%3D")
            else -> service.kioskList.defaultKioskExtractor
        }
        val extractor = kiosk.factory.createNewExtractor(service, kiosk.url, null)
        extractor.fetchPage()
        val streamItems = extractor.initialPage.items.filterIsInstance<StreamInfoItem>()
        val results = streamItems.map { item ->
            newMovieSearchResponse(
                name = item.name,
                url = item.url,
                type = TvType.Movie
            ) {
                this.posterUrl = item.thumbnailUrl
                this.plot = item.uploaderName
            }
        }
        return newHomePageResponse(request.name, results, false)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val search = SearchInfo.getInfo(ServiceList.YouTube, ServiceList.YouTube.searchQHFactory.fromQuery(query))
        val streamItems = search.relatedItems.filterIsInstance<StreamInfoItem>()
        return streamItems.map { item ->
            newMovieSearchResponse(
                name = item.name,
                url = item.url,
                type = TvType.Movie
            ) {
                this.posterUrl = item.thumbnailUrl
                this.plot = item.uploaderName
            }
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val extractor = YoutubeExtractor()
        val videoId = url.substringAfter("watch?v=").substringBefore("&").substringAfter("youtu.be/").substringBefore("?")
        return newMovieLoadResponse(
            name = "YouTube Video",
            url = url,
            type = TvType.Movie,
            dataUrl = url
        ) {
            this.plot = "YouTube Video: $videoId"
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
