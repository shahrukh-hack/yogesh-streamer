package com.lagradost.cloudstream3.ui.player

import android.util.Log
import com.lagradost.cloudstream3.APIHolder.getApiFromNameNull
import com.lagradost.cloudstream3.APIHolder.unixTime
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.ui.APIRepository
import com.lagradost.cloudstream3.ui.result.ResultEpisode
import com.lagradost.cloudstream3.utils.AppContextUtils.html
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

data class Cache(
    val linkCache: MutableSet<ExtractorLink>,
    val subtitleCache: MutableSet<SubtitleData>,
    /** When it was last updated */
    var lastCachedTimestamp: Long = unixTime,
    /** If it has fully loaded */
    var saturated: Boolean,
)

class RepoLinkGenerator(
    episodes: List<ResultEpisode>,
    val page: LoadResponse? = null,
) : VideoGenerator<ResultEpisode>(episodes) {
    companion object {
        const val TAG = "RepoLink"
        val cache: HashMap<Pair<String, Int>, Cache> =
            hashMapOf()
    }

    override val hasCache = true
    override val canSkipLoading = true
    override fun getId(index: Int): Int? = videos.getOrNull(index)?.id

    // this is a simple array that is used to instantly load links if they are already loaded
    //var linkCache = Array<Set<ExtractorLink>>(size = episodes.size, init = { setOf() })
    //var subsCache = Array<Set<SubtitleData>>(size = episodes.size, init = { setOf() })

    @Throws
    override suspend fun generateLinks(
        clearCache: Boolean,
        sourceTypes: Set<ExtractorLinkType>,
        callback: (Pair<ExtractorLink?, ExtractorUri?>) -> Unit,
        subtitleCallback: (SubtitleData) -> Unit,
        offset: Int,
        isCasting: Boolean,
    ): Boolean {
        val current = videos.getOrNull(offset) ?: return false

        val currentCache = synchronized(cache) {
            cache[current.apiName to current.id] ?: Cache(
                mutableSetOf(),
                mutableSetOf(),
                unixTime,
                false
            ).also {
                cache[current.apiName to current.id] = it
            }
        }

        // These act as a general filter to prevent duplication of links or names
        // Avoid any possible ConcurrentModificationException
        val currentLinksUrls = ConcurrentHashMap.newKeySet<String>()
        val currentSubsUrls = ConcurrentHashMap.newKeySet<String>()
        // Use atomics as otherwise we get race conditions when incrementing, while rare it did actually happen!
        val lastCountedSuffix = ConcurrentHashMap<String, AtomicInteger>()

        synchronized(currentCache) {
            val outdatedCache =
                unixTime - currentCache.lastCachedTimestamp > 60 * 20 // 20 minutes

            if (outdatedCache || clearCache) {
                currentCache.linkCache.clear()
                currentCache.subtitleCache.clear()
                currentCache.saturated = false
            } else if (currentCache.linkCache.isNotEmpty()) {
                Log.d(
                    TAG,
                    "Resumed previous loading from ${unixTime - currentCache.lastCachedTimestamp}s ago"
                )
            }

            // call all callbacks
            currentCache.linkCache.forEach { link ->
                currentLinksUrls.add(link.url)
                if (sourceTypes.contains(link.type)) {
                    callback(link to null)
                }
            }

            currentCache.subtitleCache.forEach { sub ->
                currentSubsUrls.add(sub.url)
                lastCountedSuffix.getOrPut(sub.originalName) { AtomicInteger(0) }.incrementAndGet()
                subtitleCallback(sub)
            }

            // this stops all execution if links are cached
            // no extra get requests
            if (currentCache.saturated) {
                return true
            }
        }

        val result = APIRepository(
            getApiFromNameNull(current.apiName) ?: throw Exception("This provider does not exist")
        ).loadLinks(
            current.data,
            isCasting = isCasting,
            subtitleCallback = { file ->
                Log.d(TAG, "Loaded SubtitleFile: $file")
                val correctFile = PlayerSubtitleHelper.getSubtitleData(file)
                if (correctFile.url.isBlank() || !currentSubsUrls.add(correctFile.url)) {
                    return@loadLinks
                }

                // this part makes sure that all names are unique for UX
                val nameDecoded = correctFile.originalName.html().toString()
                    .trim() // `%3Ch1%3Esub%20name…` → `<h1>sub name…` → `sub name…`
                val suffixCount =
                    lastCountedSuffix.getOrPut(nameDecoded) { AtomicInteger(0) }.incrementAndGet()

                val updatedFile =
                    correctFile.copy(originalName = nameDecoded, nameSuffix = "$suffixCount")

                synchronized(currentCache) {
                    if (currentCache.subtitleCache.add(updatedFile)) {
                        subtitleCallback(updatedFile)
                        currentCache.lastCachedTimestamp = unixTime
                    }
                }
            },
            callback = { link ->
                Log.d(TAG, "Loaded ExtractorLink: $link")
                if (link.url.isBlank() || !currentLinksUrls.add(link.url)) {
                    return@loadLinks
                }

                synchronized(currentCache) {
                    if (currentCache.linkCache.add(link)) {
                        if (sourceTypes.contains(link.type)) {
                            callback(Pair(link, null))
                        }

                        currentCache.linkCache.add(link)
                        currentCache.lastCachedTimestamp = unixTime
                    }
                }
            }
        )

        if (currentCache.linkCache.isEmpty()) {
            val titleToSearch = page?.name ?: current.headerName ?: current.name
            if (!titleToSearch.isNullOrBlank()) {
                Log.i(TAG, "No links from ${current.apiName}. Triggering Smart Cross-Provider Fallback for: $titleToSearch")
                try {
                    val otherApis = com.lagradost.cloudstream3.APIHolder.apis.filter { it.name != current.apiName }
                    for (api in otherApis.take(6)) {
                        if (currentCache.linkCache.isNotEmpty()) break
                        try {
                            val searchResults = APIRepository(api).search(titleToSearch, 1)
                            val bestMatch = (searchResults as? com.lagradost.cloudstream3.mvvm.Resource.Success)?.value?.items?.firstOrNull() ?: continue
                            val loadRes = (APIRepository(api).load(bestMatch.url) as? com.lagradost.cloudstream3.mvvm.Resource.Success)?.value
                            val episodeData = when (loadRes) {
                                is com.lagradost.cloudstream3.MovieLoadResponse -> loadRes.dataUrl
                                is com.lagradost.cloudstream3.TvSeriesLoadResponse -> {
                                    val targetEp = current.episode ?: 1
                                    val targetSeason = current.season ?: 1
                                    loadRes.episodes.firstOrNull { it.episode == targetEp && (it.season == targetSeason || targetSeason == 1) }?.data
                                        ?: loadRes.episodes.firstOrNull()?.data
                                }
                                is com.lagradost.cloudstream3.AnimeLoadResponse -> {
                                    val targetEp = current.episode ?: 1
                                    loadRes.episodes.values.flatten().firstOrNull { it.episode == targetEp }?.data
                                        ?: loadRes.episodes.values.flatten().firstOrNull()?.data
                                }
                                else -> null
                            }
                            if (!episodeData.isNullOrBlank()) {
                                Log.i(TAG, "Fallback found working source on ${api.name}! Extracting links...")
                                APIRepository(api).loadLinks(
                                    episodeData,
                                    isCasting = isCasting,
                                    subtitleCallback = { file ->
                                        val correctFile = PlayerSubtitleHelper.getSubtitleData(file)
                                        if (correctFile.url.isNotBlank() && currentSubsUrls.add(correctFile.url)) {
                                            val nameDecoded = correctFile.originalName.html().toString().trim()
                                            val suffixCount = lastCountedSuffix.getOrPut(nameDecoded) { AtomicInteger(0) }.incrementAndGet()
                                            val updatedFile = correctFile.copy(originalName = nameDecoded, nameSuffix = "$suffixCount")
                                            synchronized(currentCache) {
                                                if (currentCache.subtitleCache.add(updatedFile)) {
                                                    subtitleCallback(updatedFile)
                                                }
                                            }
                                        }
                                    },
                                    callback = { link ->
                                        if (link.url.isNotBlank() && currentLinksUrls.add(link.url)) {
                                            synchronized(currentCache) {
                                                if (currentCache.linkCache.add(link)) {
                                                    if (sourceTypes.contains(link.type)) {
                                                        callback(Pair(link, null))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                        } catch (e: Exception) {
                            Log.d(TAG, "Fallback check on ${api.name} failed: ${e.message}")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Cross-provider fallback error", e)
                }
            }
        }

        synchronized(currentCache) {
            currentCache.saturated = currentCache.linkCache.isNotEmpty()
            currentCache.lastCachedTimestamp = unixTime
        }

        return currentCache.linkCache.isNotEmpty() || result
    }
}