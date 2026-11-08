package com.lagradost.cloudstream3.ui.sports

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.lagradost.cloudstream3.APIHolder.allProviders
import com.lagradost.cloudstream3.HomePageList
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.databinding.FragmentLiveSportsBinding
import com.lagradost.cloudstream3.mvvm.Resource
import com.lagradost.cloudstream3.mvvm.logError
import com.lagradost.cloudstream3.ui.APIRepository
import com.lagradost.cloudstream3.ui.home.HomeViewModel.ExpandableHomepageList
import com.lagradost.cloudstream3.ui.home.ParentItemAdapter
import com.lagradost.cloudstream3.ui.search.SearchClickCallback
import com.lagradost.cloudstream3.ui.search.SearchHelper
import com.lagradost.cloudstream3.ui.settings.Globals.TV
import com.lagradost.cloudstream3.ui.settings.Globals.isLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.CopyOnWriteArrayList

class LiveSportsFragment : Fragment() {
    companion object {
        // High-performance In-Memory Stale-While-Revalidate (SWR) Cache
        private val cachedSportsRows = CopyOnWriteArrayList<ExpandableHomepageList>()
        private var lastFetchTimestamp: Long = 0L
        private const val CACHE_EXPIRY_MS = 5 * 60 * 1000L // 5 minutes freshness
    }

    private var binding: FragmentLiveSportsBinding? = null
    private var sportsAdapter: ParentItemAdapter? = null
    private var fetchJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val layoutId = if (isLayout(TV)) R.layout.fragment_live_sports_tv else R.layout.fragment_live_sports
        val view = inflater.inflate(layoutId, container, false)
        binding = FragmentLiveSportsBinding.bind(view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sportsAdapter = ParentItemAdapter(
            id = 0,
            clickCallback = { callback: SearchClickCallback ->
                SearchHelper.handleSearchClickCallback(callback)
            },
            moreInfoClickCallback = { _ -> },
            expandCallback = null
        )

        binding?.sportsMasterRecycler?.apply {
            adapter = sportsAdapter
            setHasFixedSize(true)
            itemAnimator = null // Remove blink/flicker on progressive row appends
        }

        binding?.sportsRefreshBtt?.setOnClickListener {
            loadSportsContent(forceRefresh = true)
        }

        // Instant 0ms Render from SWR Cache if available
        if (cachedSportsRows.isNotEmpty()) {
            sportsAdapter?.submitList(ArrayList(cachedSportsRows))
            binding?.sportsLoading?.isVisible = false
            binding?.sportsEmptyState?.isVisible = false

            // Background revalidation if stale
            if (System.currentTimeMillis() - lastFetchTimestamp > CACHE_EXPIRY_MS) {
                loadSportsContent(forceRefresh = false)
            }
        } else {
            loadSportsContent(forceRefresh = true)
        }
    }

    private fun sortSportsRows(rows: List<ExpandableHomepageList>): List<ExpandableHomepageList> {
        return rows.sortedByDescending { row ->
            val title = row.list.name.lowercase()
            when {
                title.contains("live") || title.contains("🔴") -> 3
                title.contains("cricket") || title.contains("match") -> 2
                title.contains("sports") -> 1
                else -> 0
            }
        }
    }

    private fun loadSportsContent(forceRefresh: Boolean = false) {
        fetchJob?.cancel()

        if (forceRefresh || cachedSportsRows.isEmpty()) {
            binding?.sportsLoading?.isVisible = true
            binding?.sportsEmptyState?.isVisible = false
        }

        fetchJob = lifecycleScope.launch(Dispatchers.IO) {
            try {
                val sportsProviders = allProviders.filter { api ->
                    val name = api.name.lowercase()
                    name.contains("cric") || name.contains("sport") ||
                    name.contains("jio") || name.contains("daddy") || name.contains("elite") ||
                    name.contains("publicsports") || name.contains("fancode") || name.contains("iptv") ||
                    api.supportedTypes.contains(TvType.Live)
                }

                if (sportsProviders.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        binding?.sportsLoading?.isVisible = false
                        if (cachedSportsRows.isEmpty()) {
                            binding?.sportsEmptyState?.isVisible = true
                        }
                    }
                    return@launch
                }

                val accumulatedRows = CopyOnWriteArrayList<ExpandableHomepageList>()
                if (!forceRefresh && cachedSportsRows.isNotEmpty()) {
                    accumulatedRows.addAll(cachedSportsRows)
                }

                // Progressive Parallel Execution: Each provider updates the UI immediately upon completion
                val jobs = sportsProviders.map { provider ->
                    launch(Dispatchers.IO) {
                        try {
                            if (!provider.hasMainPage) return@launch

                            // 3.5s strict per-provider timeout guard: slow/hung servers will NEVER block the UI
                            withTimeoutOrNull(3500L) {
                                val repo = APIRepository(provider)
                                val data = repo.getMainPage(1, null)
                                if (data is Resource.Success) {
                                    val newRows = mutableListOf<ExpandableHomepageList>()
                                    data.value.forEach { home ->
                                        home?.items?.forEach { list ->
                                            if (list.list.isNotEmpty()) {
                                                newRows.add(
                                                    ExpandableHomepageList(
                                                        list.copy(
                                                            name = "${provider.name} - ${list.name}",
                                                            list = CopyOnWriteArrayList(list.list)
                                                        ),
                                                        currentPage = 1,
                                                        hasNext = home.hasNext
                                                    )
                                                )
                                            }
                                        }
                                    }

                                    if (newRows.isNotEmpty()) {
                                        // Filter out any older rows from this exact provider before appending fresh ones
                                        val filtered = accumulatedRows.filterNot { it.list.name.startsWith(provider.name) }
                                        accumulatedRows.clear()
                                        accumulatedRows.addAll(filtered)
                                        accumulatedRows.addAll(newRows)

                                        val sortedSnapshot = sortSportsRows(ArrayList(accumulatedRows))

                                        // Progressive instant dispatch to UI
                                        withContext(Dispatchers.Main) {
                                            binding?.sportsLoading?.isVisible = false
                                            binding?.sportsEmptyState?.isVisible = false
                                            sportsAdapter?.submitList(sortedSnapshot)
                                        }
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            logError(e)
                        }
                    }
                }

                // Wait for all progressive jobs to conclude
                jobs.forEach { it.join() }

                // Save final state into high-speed memory cache
                val finalSorted = sortSportsRows(ArrayList(accumulatedRows))
                cachedSportsRows.clear()
                cachedSportsRows.addAll(finalSorted)
                lastFetchTimestamp = System.currentTimeMillis()

                withContext(Dispatchers.Main) {
                    binding?.sportsLoading?.isVisible = false
                    if (finalSorted.isEmpty()) {
                        binding?.sportsEmptyState?.isVisible = true
                    } else {
                        binding?.sportsEmptyState?.isVisible = false
                        sportsAdapter?.submitList(finalSorted)
                    }
                }
            } catch (e: Exception) {
                logError(e)
                withContext(Dispatchers.Main) {
                    binding?.sportsLoading?.isVisible = false
                    if (cachedSportsRows.isEmpty()) {
                        binding?.sportsEmptyState?.isVisible = true
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        fetchJob?.cancel()
        binding = null
        sportsAdapter = null
        super.onDestroyView()
    }
}
