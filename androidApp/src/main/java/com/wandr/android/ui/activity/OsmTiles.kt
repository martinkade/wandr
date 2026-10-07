package com.wandr.android.ui.activity

import android.content.Context
import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import com.wandr.android.ui.activity.OsmTiles.KEEP_SECONDS
import com.wandr.domain.geo.StaticMapLayout
import com.wandr.domain.model.GpsTrackpoint
import okhttp3.OkHttpClient
import okio.Path.Companion.toOkioPath

/**
 * Map tiles of the route maps: their own image loader with a 100 MB disk cache. The tile server only allows a week of
 * caching; here a tile stays valid for [KEEP_SECONDS] (30 days) so recorded routes still show on a map without network.
 * Identifies the app to the tile server, as its usage policy asks.
 */
object OsmTiles {
    private const val KEEP_SECONDS = 30L * 24 * 60 * 60
    private const val DISK_CACHE_BYTES = 100L * 1024 * 1024
    private const val USER_AGENT = "WANDR/1.0 (com.mediabeam.fitness; Android)"

    @Volatile
    private var instance: ImageLoader? = null

    fun loader(context: Context): ImageLoader = instance ?: synchronized(this) {
        instance ?: create(context.applicationContext).also { instance = it }
    }

    private fun create(context: Context): ImageLoader {
        val client = OkHttpClient.Builder()
            .addNetworkInterceptor { chain ->
                val response = chain.proceed(
                    chain.request().newBuilder().header("User-Agent", USER_AGENT).build()
                )
                response.newBuilder().header("Cache-Control", "public, max-age=$KEEP_SECONDS")
                    .removeHeader("Expires").build()
            }
            .build()
        return ImageLoader.Builder(context)
            .components { add(OkHttpNetworkFetcherFactory(callFactory = { client })) }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("osm_tiles").toOkioPath())
                    .maxSizeBytes(DISK_CACHE_BYTES)
                    .build()
            }
            .memoryCache { MemoryCache.Builder().maxSizePercent(context, 0.1).build() }
            .build()
    }

    /**
     * Loads the tiles of [route] into the disk cache, for the sizes the maps of an activity are shown in (feed card and
     * details), so the route is on a map later even without network. Runs in the background, results are not awaited.
     */
    fun prefetch(context: Context, route: List<GpsTrackpoint>) {
        if (route.isEmpty()) return
        val metrics = context.resources.displayMetrics
        val widthPx = metrics.widthPixels.toDouble()
        val padding = 32 * metrics.density.toDouble()
        val density = metrics.density.toDouble()
        val feedCard = StaticMapLayout.fit(route, widthPx, 192 * density, padding)
        // The details show the map as their header: taller, clear of the top bar and of the sheet over its lower edge.
        val details = StaticMapLayout.fit(
            route,
            widthPx,
            320 * density,
            padding,
            topPadding = 96 * density,
            bottomPadding = (32 + 28) * density
        )
        val urls = (feedCard.tiles + details.tiles).map { it.url }.toSet()
        val loader = loader(context)
        urls.forEach { url ->
            loader.enqueue(
                ImageRequest.Builder(context.applicationContext)
                    .data(url)
                    .memoryCachePolicy(CachePolicy.DISABLED)
                    .build()
            )
        }
    }
}
