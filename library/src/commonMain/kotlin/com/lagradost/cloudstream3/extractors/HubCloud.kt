package com.lagradost.cloudstream3.extractors

import com.lagradost.api.Log
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.amap
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.loadExtractor
import com.lagradost.cloudstream3.utils.newExtractorLink
import io.ktor.http.Url

class HubCloud : ExtractorApi() {
    override val name = "Hub-Cloud"
    override val mainUrl = "https://hubcloud.*"
    override val requiresReferer = false

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val tag = "HubCloud"
        val realUrl = url.takeIf {
            try { Url(it); true } catch (e: Exception) { Log.e(tag, "Invalid URL: ${e.message}"); false }
        } ?: return

        val baseUrl = getBaseUrl(realUrl)

        val href = try {
            if ("hubcloud.php" in realUrl || "drive" in realUrl) {
                realUrl
            } else {
                val resp = app.get(realUrl, referer = referer)
                val rawHref = resp.document.select("#download, a.btn-success, a.btn-primary, div.card-body a[href*='hubcloud']").attr("href")
                if (rawHref.startsWith("http", ignoreCase = true)) {
                    rawHref
                } else if (rawHref.isNotBlank()) {
                    baseUrl.trimEnd('/') + "/" + rawHref.trimStart('/')
                } else {
                    realUrl
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to extract href: ${e.message}")
            realUrl
        }

        if (href.isBlank()) {
            Log.w(tag, "No valid href found")
            return
        }

        val docResp = try {
            app.get(href, referer = realUrl)
        } catch (e: Exception) {
            Log.e(tag, "Failed to fetch document: ${e.message}")
            return
        }
        val document = docResp.document
        val size = document.selectFirst("i#size, span.badge")?.text().orEmpty()
        val header = document.selectFirst("div.card-header, h1, h2")?.text().orEmpty()

        val headerDetails = cleanTitle(header)

        val labelExtras = buildString {
            if (headerDetails.isNotEmpty()) append("[$headerDetails]")
            if (size.isNotEmpty()) append("[$size]")
        }
        val quality = getIndexQuality(header)

        document.select("div.card-body h2 a.btn, div.card-body a.btn, a[href*='download'], a[href*='token'], a[href*='pixeldrain']").amap { element ->
            val link = element.attr("href")
            val text = element.text()

            if (link.isBlank() || link.startsWith("javascript") || link == "#") return@amap

            val finalTarget = if (link.startsWith("http")) link else baseUrl.trimEnd('/') + "/" + link.trimStart('/')

            when {
                text.contains("FSL Server", ignoreCase = true) || text.contains("Fast Server", ignoreCase = true) -> {
                    callback.invoke(
                        newExtractorLink(
                            "${referer ?: "HubCloud"} [Fast Direct]",
                            "${referer ?: "HubCloud"} [Fast Direct] $labelExtras",
                            finalTarget,
                        ) { this.quality = quality }
                    )
                }

                text.contains("Download File", ignoreCase = true) || text.contains("Direct Download", ignoreCase = true) -> {
                    callback.invoke(
                        newExtractorLink(
                            "${referer ?: "HubCloud"} [Direct]",
                            "${referer ?: "HubCloud"} [Direct] $labelExtras",
                            finalTarget,
                        ) { this.quality = quality }
                    )
                }

                text.contains("BuzzServer", ignoreCase = true) || text.contains("Buzz", ignoreCase = true) -> {
                    try {
                        val buzzResp = app.get("$finalTarget/download", referer = finalTarget, allowRedirects = false)
                        val dlink = buzzResp.headers["hx-redirect"] ?: buzzResp.headers["location"] ?: ""
                        if (dlink.isNotBlank()) {
                            callback.invoke(
                                newExtractorLink(
                                    "${referer ?: "HubCloud"} [BuzzServer]",
                                    "${referer ?: "HubCloud"} [BuzzServer] $labelExtras",
                                    dlink,
                                ) { this.quality = quality }
                            )
                        }
                    } catch (_: Exception) {}
                }

                text.contains("pixeldra", ignoreCase = true) || text.contains("pixel", ignoreCase = true) || "pixeldrain" in finalTarget -> {
                    val fileId = finalTarget.substringAfterLast("/").substringBefore("?")
                    val finalURL = "https://pixeldrain.com/api/file/$fileId?download"

                    callback(
                        newExtractorLink(
                            "Pixeldrain (Ultra Fast)",
                            "Pixeldrain $labelExtras",
                            finalURL
                        ) { this.quality = quality }
                    )
                }

                text.contains("S3 Server", ignoreCase = true) || text.contains("AWS", ignoreCase = true) -> {
                    callback.invoke(
                        newExtractorLink(
                            "${referer ?: "HubCloud"} [S3 Cloud]",
                            "${referer ?: "HubCloud"} [S3 Cloud] $labelExtras",
                            finalTarget,
                        ) { this.quality = quality }
                    )
                }

                text.contains("FSLv2", ignoreCase = true) || text.contains("Mega Server", ignoreCase = true) -> {
                    callback.invoke(
                        newExtractorLink(
                            "${referer ?: "HubCloud"} [Mega Direct]",
                            "${referer ?: "HubCloud"} [Mega Direct] $labelExtras",
                            finalTarget,
                        ) { this.quality = quality }
                    )
                }

                else -> {
                    loadExtractor(finalTarget, finalTarget, subtitleCallback, callback)
                }
            }
        }
    }

    private fun getIndexQuality(str: String?): Int {
        return Regex("(\\d{3,4})[pP]").find(str.orEmpty())?.groupValues?.getOrNull(1)?.toIntOrNull()
            ?: Qualities.P1080.value
    }

    private fun getBaseUrl(url: String): String {
        return try {
            Url(url).let { "${it.protocol.name}://${it.host}" }
        } catch (_: Exception) {
            ""
        }
    }

    fun cleanTitle(title: String): String {
        val parts = title.split(".", "-", "_", " ")

        val qualityTags = listOf(
            "WEBRip", "WEB-DL", "WEB", "BluRay", "HDRip", "DVDRip", "HDTV",
            "CAM", "TS", "R5", "DVDScr", "BRRip", "BDRip", "DVD", "PDTV",
            "HD", "1080p", "720p", "480p", "2160p", "4K"
        )

        val audioTags = listOf(
            "AAC", "AC3", "DTS", "MP3", "FLAC", "DD5", "EAC3", "Atmos", "Hindi", "Dual"
        )

        val subTags = listOf(
            "ESub", "ESubs", "Subs", "MultiSub", "NoSub", "EnglishSub", "HindiSub"
        )

        val codecTags = listOf(
            "x264", "x265", "H264", "HEVC", "AVC", "10bit"
        )

        val startIndex = parts.indexOfFirst { part ->
            qualityTags.any { tag -> part.contains(tag, ignoreCase = true) }
        }

        val endIndex = parts.indexOfLast { part ->
            subTags.any { tag -> part.contains(tag, ignoreCase = true) } ||
                    audioTags.any { tag -> part.contains(tag, ignoreCase = true) } ||
                    codecTags.any { tag -> part.contains(tag, ignoreCase = true) }
        }

        return if (startIndex != -1 && endIndex != -1 && endIndex >= startIndex) {
            parts.subList(startIndex, endIndex + 1).joinToString(".")
        } else if (startIndex != -1) {
            parts.subList(startIndex, parts.size).joinToString(".")
        } else {
            parts.takeLast(3).joinToString(".")
        }
    }
}
