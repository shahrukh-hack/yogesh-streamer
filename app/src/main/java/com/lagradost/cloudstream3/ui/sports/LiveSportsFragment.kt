package com.lagradost.cloudstream3.ui.sports

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.lagradost.cloudstream3.APIHolder.allProviders
import com.lagradost.cloudstream3.APIHolder.getApiFromNameNull
import com.lagradost.cloudstream3.APIHolder.getApiProviderLangSettings
import com.lagradost.cloudstream3.HomePageList
import com.lagradost.cloudstream3.HomePageResponse
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.databinding.FragmentLiveSportsBinding
import com.lagradost.cloudstream3.mvvm.Resource
import com.lagradost.cloudstream3.mvvm.logError
import com.lagradost.cloudstream3.ui.home.HomeViewModel.ExpandableHomepageList
import com.lagradost.cloudstream3.ui.home.ParentItemAdapter
import com.lagradost.cloudstream3.ui.search.SearchClickCallback
import com.lagradost.cloudstream3.ui.search.SearchHelper
import com.lagradost.cloudstream3.ui.settings.Globals.TV
import com.lagradost.cloudstream3.ui.settings.Globals.isLayout
import com.lagradost.cloudstream3.utils.APIRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LiveSportsFragment : Fragment() {
    private var binding: FragmentLiveSportsBinding? = null
    private var sportsAdapter: ParentItemAdapter? = null

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

        binding?.sportsMasterRecycler?.adapter = sportsAdapter

        binding?.sportsRefreshBtt?.setOnClickListener {
            loadSportsContent()
        }
        loadSportsContent()
    }

    private fun loadSportsContent() {
        binding?.sportsLoading?.isVisible = true
        binding?.sportsEmptyState?.isVisible = false

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val sportsProviders = allProviders.filter { api ->
                    val name = api.name.lowercase()
                    name.contains("cric") || name.contains("sktech") || name.contains("sport") ||
                    name.contains("publicsports") || name.contains("livxow") ||
                    api.supportedTypes.contains(TvType.Live)
                }

                val allSportsRows = mutableListOf<ExpandableHomepageList>()

                for (provider in sportsProviders) {
                    try {
                        if (provider.hasMainPage) {
                            val repo = APIRepository(provider)
                            val mainPageRes = repo.getMainPage(1, null)
                            if (mainPageRes is Resource.Success) {
                                mainPageRes.value.forEach { homePageResponse ->
                                    homePageResponse?.items?.forEach { pageList ->
                                        if (pageList.list.isNotEmpty()) {
                                            allSportsRows.add(
                                                ExpandableHomepageList(
                                                    list = HomePageList(
                                                        name = "${provider.name} - ${pageList.name}",
                                                        list = pageList.list,
                                                        isHorizontalImages = pageList.isHorizontalImages
                                                    ),
                                                    currentPage = 1,
                                                    hasNext = homePageResponse.hasNext
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        logError(e)
                    }
                }

                withContext(Dispatchers.Main) {
                    binding?.sportsLoading?.isVisible = false
                    if (allSportsRows.isEmpty()) {
                        binding?.sportsEmptyState?.isVisible = true
                    } else {
                        binding?.sportsEmptyState?.isVisible = false
                        sportsAdapter?.submitList(allSportsRows)
                    }
                }
            } catch (e: Exception) {
                logError(e)
                withContext(Dispatchers.Main) {
                    binding?.sportsLoading?.isVisible = false
                    binding?.sportsEmptyState?.isVisible = true
                }
            }
        }
    }

    override fun onDestroyView() {
        binding = null
        sportsAdapter = null
        super.onDestroyView()
    }
}
