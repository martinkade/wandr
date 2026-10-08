import SwiftUI
@preconcurrency import shared

/// The route of an activity (or, while recording, the position so far) on an OpenStreetMap background. The map is
/// static (no gestures): the shared `StaticMapLayout` fits the route and lists the tiles that cover it, the route is
/// drawn above. Until the tiles are there (or offline) the route shows on a plain background. The attribution is
/// required by the OSM license.
struct TrackMapView: View {
    let trackpoints: [GpsTrackpoint]
    var height: CGFloat = 200

    var body: some View {
        GeometryReader { geo in
            let layout = trackpoints.isEmpty || geo.size.width < 1 || geo.size.height < 1 ? nil
                : StaticMapLayout.companion.fit(
                points: trackpoints, width: Double(geo.size.width), height: Double(geo.size.height), padding: 32, topPadding: 32, bottomPadding: 32)
            ZStack(alignment: .bottomTrailing) {
                Color(.secondarySystemBackground)
                if let layout {
                    ForEach(layout.tiles, id: \.self) { tile in
                        OsmTileView(url: tile.url)
                            .frame(width: CGFloat(StaticMapLayout.companion.TILE_SIZE), height: CGFloat(StaticMapLayout.companion.TILE_SIZE))
                            .position(x: CGFloat(tile.left) + CGFloat(StaticMapLayout.companion.TILE_SIZE) / 2,
                                      y: CGFloat(tile.top) + CGFloat(StaticMapLayout.companion.TILE_SIZE) / 2)
                    }
                    RouteShape(points: trackpoints.map { point in
                        let projected = layout.project(latitude: point.latitude, longitude: point.longitude)
                        return CGPoint(x: projected.first?.doubleValue ?? 0, y: projected.second?.doubleValue ?? 0)
                    })
                    Text("map_attribution")
                        .font(.system(size: 10))
                        .foregroundColor(.black.opacity(0.7))
                        .padding(.horizontal, 4)
                        .background(Color.white.opacity(0.7))
                }
            }
            .frame(width: geo.size.width, height: geo.size.height)
            .clipped()
        }
        .frame(height: height)
        .cornerRadius(12)
    }
}

/// The route as a line with a white outline, a green start and a red end (a single position is just a dot).
private struct RouteShape: View {
    let points: [CGPoint]

    var body: some View {
        ZStack {
            if points.count >= 2 {
                let path = Path { p in
                    p.move(to: points[0])
                    points.dropFirst().forEach {
                        p.addLine(to: $0)
                    }
                }
                path.stroke(Color.white, style: StrokeStyle(lineWidth: 7, lineCap: .round, lineJoin: .round))
                path.stroke(Color.wandrRoute, style: StrokeStyle(lineWidth: 4, lineCap: .round, lineJoin: .round))
                marker(points[0], color: .green)
                marker(points[points.count - 1], color: .red)
            } else if let only = points.first {
                marker(only, color: .wandrRoute)
            }
        }
    }

    private func marker(_ point: CGPoint, color: Color) -> some View {
        ZStack {
            Circle().fill(Color.white).frame(width: 15, height: 15)
            Circle().fill(color).frame(width: 11, height: 11)
        }
        .position(point)
    }
}

/// One OSM tile, loaded through the tile cache (see `OsmTileCache`).
private struct OsmTileView: View {
    let url: String
    @State private var image: UIImage?

    var body: some View {
        Group {
            if let image {
                Image(uiImage: image).resizable()
            } else {
                Color.clear
            }
        }
        .task(id: url) {
            guard let tileUrl = URL(string: url), let data = await OsmTileCache.data(for: tileUrl) else {
                return
            }
            image = UIImage(data: data)
        }
    }
}

extension Activity {
    /// The route to draw: the simplified polyline of the activity, empty without GPS data.
    var route: [GpsTrackpoint] {
        polyline.map {
            PolylineCodec.shared.decode(encoded: $0)
        } ?? []
    }
}

#Preview("Light Mode") {
    TrackMapView(trackpoints: [
        GpsTrackpoint(latitude: 47.3769, longitude: 8.5417, altitudeMeters: 400.0, timestamp: 0, speedMetersPerSecond: 0.0),
        GpsTrackpoint(latitude: 47.3779, longitude: 8.5437, altitudeMeters: 410.0, timestamp: 0, speedMetersPerSecond: 0.0),
        GpsTrackpoint(latitude: 47.3801, longitude: 8.5460, altitudeMeters: 420.0, timestamp: 0, speedMetersPerSecond: 0.0)
    ])
    .preferredColorScheme(.light)
}

#Preview("Dark Mode") {
    TrackMapView(trackpoints: [
        GpsTrackpoint(latitude: 47.3769, longitude: 8.5417, altitudeMeters: 400.0, timestamp: 0, speedMetersPerSecond: 0.0),
        GpsTrackpoint(latitude: 47.3779, longitude: 8.5437, altitudeMeters: 410.0, timestamp: 0, speedMetersPerSecond: 0.0),
        GpsTrackpoint(latitude: 47.3801, longitude: 8.5460, altitudeMeters: 420.0, timestamp: 0, speedMetersPerSecond: 0.0)
    ])
    .preferredColorScheme(.dark)
}
