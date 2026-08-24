package com.lagradost.cloudstream3.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.WorkerThread
import androidx.core.net.toUri
import androidx.tvprovider.media.tv.ChannelLogoUtils
import androidx.tvprovider.media.tv.PreviewChannel
import androidx.tvprovider.media.tv.PreviewChannelHelper
import androidx.tvprovider.media.tv.PreviewProgram
import androidx.tvprovider.media.tv.TvContractCompat
import com.lagradost.cloudstream3.MainActivity
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.mvvm.logError
import com.lagradost.cloudstream3.ui.settings.Globals.TV
import com.lagradost.cloudstream3.ui.settings.Globals.isLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object TvRecommendationHelper {
    private const val CHANNEL_NAME = "Yogesh Streamer - Trending"
    private const val CHANNEL_DESCRIPTION = "Popular Movies & Shows from Yogesh Streamer"
    private const val PREF_KEY_TV_CHANNEL_ID = "pref_yogesh_tv_channel_id"

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("RestrictedApi")
    suspend fun getOrCreateDefaultChannel(context: Context): Long? {
        if (!isLayout(TV) || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return null
        return withContext(Dispatchers.IO) {
            try {
                val helper = PreviewChannelHelper(context)
                val existingChannels = helper.allChannels
                val found = existingChannels.firstOrNull { it.displayName == CHANNEL_NAME }
                if (found != null) {
                    return@withContext found.id
                }

                val appIntent = Intent(context, MainActivity::class.java).apply {
                    action = Intent.ACTION_MAIN
                    addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
                }

                val channel = PreviewChannel.Builder()
                    .setDisplayName(CHANNEL_NAME)
                    .setDescription(CHANNEL_DESCRIPTION)
                    .setAppLinkIntent(appIntent)
                    .build()

                val channelId = helper.publishChannel(channel)
                try {
                    val logoBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.splash_logo)
                    if (logoBitmap != null) {
                        ChannelLogoUtils.storeChannelLogo(context, channelId, logoBitmap)
                    }
                } catch (e: Exception) {
                    logError(e)
                }

                TvContractCompat.requestChannelBrowsable(context, channelId)
                channelId
            } catch (e: Exception) {
                logError(e)
                null
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("RestrictedApi")
    @WorkerThread
    suspend fun publishTrendingPrograms(context: Context, items: List<SearchResponse>) {
        if (!isLayout(TV) || Build.VERSION.SDK_INT < Build.VERSION_CODES.O || items.isEmpty()) return

        withContext(Dispatchers.IO) {
            try {
                val channelId = getOrCreateDefaultChannel(context) ?: return@withContext
                val helper = PreviewChannelHelper(context)

                // Clear old preview programs from this channel
                try {
                    context.contentResolver.delete(
                        TvContractCompat.buildPreviewProgramsUriForChannel(channelId),
                        null,
                        null
                    )
                } catch (e: Exception) {
                    logError(e)
                }

                // Add up to 15 top trending items
                items.take(15).forEach { item ->
                    try {
                        val isSeries = item.type == TvType.TvSeries || item.type == TvType.Anime
                        val launchUri = Uri.parse("yogeshstreamer://load?url=${Uri.encode(item.url)}&apiName=${Uri.encode(item.apiName)}")

                        val program = PreviewProgram.Builder()
                            .setChannelId(channelId)
                            .setTitle(item.name)
                            .setDescription("Watch on Yogesh Streamer")
                            .setPosterArtUri(item.posterUrl?.toUri())
                            .setIntentUri(launchUri)
                            .setInternalProviderId(item.url)
                            .setType(
                                if (isSeries) TvContractCompat.PreviewPrograms.TYPE_TV_SERIES
                                else TvContractCompat.PreviewPrograms.TYPE_MOVIE
                            )
                            .build()

                        helper.publishPreviewProgram(program)
                    } catch (e: Exception) {
                        logError(e)
                    }
                }
            } catch (e: Exception) {
                logError(e)
            }
        }
    }
}
