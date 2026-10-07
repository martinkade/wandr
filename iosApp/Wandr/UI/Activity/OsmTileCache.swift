import UIKit
@preconcurrency import shared

/// Map tiles of the route maps: their own 100 MB disk cache (the shared `URLCache` is small and also holds other
/// images). A cached tile is used whatever its age, so recorded routes still show on a map without network; the oldest
/// tiles go when the cache is full. Identifies the app to the tile server, as its usage policy asks.
enum OsmTileCache {
    private static let cache = URLCache(
        memoryCapacity: 20 * 1024 * 1024,
        diskCapacity: 100 * 1024 * 1024,
        directory: FileManager.default.urls(for: .cachesDirectory, in: .userDomainMask).first?.appendingPathComponent("osm_tiles")
    )

    private static let session: URLSession = {
        let configuration = URLSessionConfiguration.default
        configuration.urlCache = cache
        configuration.requestCachePolicy = .returnCacheDataElseLoad
        configuration.httpAdditionalHeaders = ["User-Agent": "WANDR/1.0 (com.mediabeam.fitness; iOS)"]
        return URLSession(configuration: configuration)
    }()

    /// The image data of a tile: from the cache, otherwise from the tile server (and stored in the cache).
    static func data(for url: URL) async -> Data? {
        let request = URLRequest(url: url, cachePolicy: .returnCacheDataElseLoad)
        if let cached = cache.cachedResponse(for: request) {
            return cached.data
        }
        guard let (data, response) = try? await session.data(for: request),
              (response as? HTTPURLResponse)?.statusCode == 200
        else {
            return nil
        }
        // Stored explicitly: the tile server's own cache headers only allow a week.
        cache.storeCachedResponse(CachedURLResponse(response: response, data: data, userInfo: nil, storagePolicy: .allowed), for: request)
        return data
    }

    /// Loads the tiles of `route` into the cache, for the sizes the maps of an activity are shown in (feed card and
    /// details), so the route is on a map later even without network. Runs in the background.
    static func prefetch(route: [GpsTrackpoint]) {
        guard !route.isEmpty else {
            return
        }
        let width = Double(UIScreen.main.bounds.width)
        let sizes = [(width, 192.0), (width - 32, 200.0)] // feed card, details (16 pt page padding)
        let urls = Set(sizes.flatMap {
            StaticMapLayout.companion.fit(points: route, width: $0.0, height: $0.1, padding: 32).tiles
        }
                       .compactMap {
                           URL(string: $0.url)
                       })
        Task.detached(priority: .utility) {
            for url in urls {
                _ = await data(for: url)
            }
        }
    }
}
