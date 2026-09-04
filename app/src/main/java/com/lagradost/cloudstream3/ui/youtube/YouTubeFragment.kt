package com.lagradost.cloudstream3.ui.youtube

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.appcompat.widget.SearchView
import com.lagradost.cloudstream3.APIHolder
import com.lagradost.cloudstream3.CloudStreamApp.Companion.getKey
import com.lagradost.cloudstream3.CloudStreamApp.Companion.setKey
import com.lagradost.cloudstream3.CommonActivity.showToast
import com.lagradost.cloudstream3.HomePageList
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.YouTubeProvider
import com.lagradost.cloudstream3.databinding.FragmentYoutubeBinding
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.CopyOnWriteArrayList

class YouTubeFragment : Fragment() {
    companion object {
        private const val YOUTUBE_LAST_SEARCH_KEY = "youtube_last_search_query"
    }

    private var binding: FragmentYoutubeBinding? = null
    private var tubeAdapter: ParentItemAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val layoutId = if (isLayout(TV)) R.layout.fragment_youtube_tv else R.layout.fragment_youtube
        val view = inflater.inflate(layoutId, container, false)
        binding = FragmentYoutubeBinding.bind(view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tubeAdapter = ParentItemAdapter(
            id = 1,
            clickCallback = { callback: SearchClickCallback ->
                SearchHelper.handleSearchClickCallback(callback)
            },
            moreInfoClickCallback = { _ -> },
            expandCallback = null
        )

        binding?.youtubeMasterRecycler?.adapter = tubeAdapter

        binding?.youtubeRefreshBtt?.setOnClickListener {
            loadYouTubeContent()
        }

        binding?.smarttubeAppLaunchBtt?.setOnClickListener {
            launchSmartTubeApp()
        }

        binding?.youtubeSearchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (!query.isNullOrBlank()) {
                    searchYouTube(query.trim())
                    binding?.youtubeSearchView?.clearFocus()
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrBlank()) {
                    loadYouTubeContent()
                }
                return true
            }
        })

        loadYouTubeContent()
    }

    private fun launchSmartTubeApp() {
        val context = context ?: return
        val packages = listOf(
            "com.teamsmart.videomanager.tv",
            "com.liskovsoft.videomanager",
            "com.google.android.youtube.tv",
            "com.google.android.youtube"
        )
        for (pkg in packages) {
            val intent = context.packageManager.getLaunchIntentForPackage(pkg)
            if (intent != null) {
                startActivity(intent)
                return
            }
        }
        // If not installed, open SmartTube GitHub releases
        showToast("SmartTube not installed. Opening download page...", Toast.LENGTH_LONG)
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://smarttubeapp.github.io/"))
        startActivity(browserIntent)
    }

    private fun loadYouTubeContent() {
        binding?.youtubeLoading?.isVisible = true
        binding?.youtubeEmptyState?.isVisible = false

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val ytProvider = APIHolder.allProviders.firstOrNull { it is YouTubeProvider } ?: YouTubeProvider().also {
                    APIHolder.allProviders.add(it)
                    APIHolder.addPluginMapping(it)
                }

                val rows = mutableListOf<ExpandableHomepageList>()

                val lastSearch = getKey<String>(YOUTUBE_LAST_SEARCH_KEY)
                if (!lastSearch.isNullOrBlank()) {
                    try {
                        val suggestedResults = ytProvider.search(lastSearch).orEmpty()
                        if (suggestedResults.isNotEmpty()) {
                            rows.add(
                                ExpandableHomepageList(
                                    HomePageList(
                                        name = "Suggested for You (\"$lastSearch\")",
                                        list = CopyOnWriteArrayList(suggestedResults),
                                        isHorizontalImages = true
                                    ),
                                    currentPage = 1,
                                    hasNext = false
                                )
                            )
                        }
                    } catch (e: Exception) {
                        logError(e)
                    }
                }

                val repo = APIRepository(ytProvider)
                when (val data = repo.getMainPage(1, null)) {
                    is Resource.Success -> {
                        data.value.forEach { home ->
                            home?.items?.forEach { list ->
                                if (list.list.isNotEmpty()) {
                                    rows.add(
                                        ExpandableHomepageList(
                                            list.copy(
                                                name = list.name,
                                                list = CopyOnWriteArrayList(list.list)
                                            ),
                                            currentPage = 1,
                                            hasNext = home.hasNext
                                        )
                                    )
                                }
                            }
                        }
                    }
                    else -> Unit
                }

                withContext(Dispatchers.Main) {
                    binding?.youtubeLoading?.isVisible = false
                    if (rows.isEmpty()) {
                        binding?.youtubeEmptyState?.isVisible = true
                    } else {
                        binding?.youtubeEmptyState?.isVisible = false
                        tubeAdapter?.submitList(rows)
                    }
                }
            } catch (e: Exception) {
                logError(e)
                withContext(Dispatchers.Main) {
                    binding?.youtubeLoading?.isVisible = false
                    binding?.youtubeEmptyState?.isVisible = true
                }
            }
        }
    }

    private fun searchYouTube(query: String) {
        setKey(YOUTUBE_LAST_SEARCH_KEY, query)
        binding?.youtubeLoading?.isVisible = true
        binding?.youtubeEmptyState?.isVisible = false

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val ytProvider = APIHolder.allProviders.firstOrNull { it is YouTubeProvider } ?: YouTubeProvider().also {
                    APIHolder.allProviders.add(it)
                    APIHolder.addPluginMapping(it)
                }

                val results = ytProvider.search(query).orEmpty()
                val rows = if (results.isNotEmpty()) {
                    listOf(
                        ExpandableHomepageList(
                            HomePageList(
                                name = "Search Results for \"$query\"",
                                list = CopyOnWriteArrayList(results),
                                isHorizontalImages = true
                            ),
                            currentPage = 1,
                            hasNext = false
                        )
                    )
                } else emptyList()

                withContext(Dispatchers.Main) {
                    binding?.youtubeLoading?.isVisible = false
                    if (rows.isEmpty()) {
                        binding?.youtubeEmptyState?.isVisible = true
                    } else {
                        binding?.youtubeEmptyState?.isVisible = false
                        tubeAdapter?.submitList(rows)
                    }
                }
            } catch (e: Exception) {
                logError(e)
                withContext(Dispatchers.Main) {
                    binding?.youtubeLoading?.isVisible = false
                    binding?.youtubeEmptyState?.isVisible = true
                }
            }
        }
    }

    override fun onDestroyView() {
        binding = null
        tubeAdapter = null
        super.onDestroyView()
    }
}
