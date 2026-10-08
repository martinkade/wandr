import SwiftUI
@preconcurrency import shared

/// An OpenStreetMap the user can move: drag with one finger, pinch with two. It follows the current position with the
/// route so far drawn on it, until the user moves or zooms it; then a button brings it back to the position and the
/// default zoom. The viewport math is the shared `MapViewport` (the same as on Android).
struct InteractiveMapView: View {
    let route: [GpsTrackpoint]
    let position: GpsTrackpoint?
    var height: CGFloat = 280

    @State private var centerLatitude: Double = 0
    @State private var centerLongitude: Double = 0
    @State private var zoom: Double = Self.defaultZoom
    @State private var following = true
    @State private var lastDrag: CGSize = .zero
    @State private var pinchStartZoom: Double?

    /// Street level: about 1 km of the surroundings on a phone.
    private static let defaultZoom = 16.5

    var body: some View {
        GeometryReader { geo in
            let viewport = MapViewport(
                centerLatitude: centerLatitude, centerLongitude: centerLongitude, zoom: zoom,
                width: Double(max(geo.size.width, 1)), height: Double(max(geo.size.height, 1))
            )
            ZStack(alignment: .bottomTrailing) {
                Color(.secondarySystemBackground)
                if position != nil || !route.isEmpty {
                    ForEach(viewport.tiles, id: \.self) { tile in
                        let size = CGFloat(viewport.tileSizePx) + 1 // one point more: no seams between scaled tiles
                        OsmTileView(url: tile.url)
                            .frame(width: size, height: size)
                            .position(x: CGFloat(tile.left) + size / 2, y: CGFloat(tile.top) + size / 2)
                    }
                    RouteShape(points: route.map { point in screenPoint(viewport, point) })
                    if let position {
                        let p = screenPoint(viewport, position)
                        ZStack {
                            Circle().fill(Color.white).frame(width: 18, height: 18)
                            Circle().fill(Color.wandrRoute).frame(width: 13, height: 13)
                        }
                        .position(p)
                    }
                    Text("map_attribution")
                        .font(.system(size: 10))
                        .foregroundColor(.black.opacity(0.7))
                        .padding(.horizontal, 4)
                        .background(Color.white.opacity(0.7))
                }
            }
            .frame(width: geo.size.width, height: geo.size.height)
            .clipped()
            .contentShape(Rectangle())
            .gesture(
                DragGesture()
                    .onChanged { value in
                        following = false
                        let next = viewport.panned(
                            dx: Double(value.translation.width - lastDrag.width),
                            dy: Double(value.translation.height - lastDrag.height)
                        )
                        lastDrag = value.translation
                        apply(next)
                    }
                    .onEnded { _ in lastDrag = .zero }
            )
            .simultaneousGesture(
                MagnifyGesture()
                    .onChanged { value in
                        following = false
                        let base = pinchStartZoom ?? zoom
                        pinchStartZoom = base
                        let target = MapViewport.companion.zoomAfterPinch(zoom: base, pinchFactor: Double(value.magnification))
                        apply(viewport.zoomedAround(newZoom: target, x: Double(value.startLocation.x), y: Double(value.startLocation.y)))
                    }
                    .onEnded { _ in pinchStartZoom = nil }
            )
            .overlay(alignment: .topTrailing) {
                if !following || abs(zoom - Self.defaultZoom) > 0.05 {
                    Button(action: recenter) {
                        Image(systemName: "location.fill")
                            .padding(10)
                            .background(.regularMaterial, in: Circle())
                    }
                    .padding(8)
                    .accessibilityLabel(Text(LocalizedStringKey("recording_recenter")))
                }
            }
        }
        .frame(height: height)
        .cornerRadius(12)
        // While following, the map is centered on the position (every new fix moves it).
        .onChange(of: position?.timestamp, initial: true) { _, _ in
            guard following, let position else { return }
            centerLatitude = position.latitude
            centerLongitude = position.longitude
        }
    }

    private func screenPoint(_ viewport: MapViewport, _ point: GpsTrackpoint) -> CGPoint {
        let p = viewport.project(latitude: point.latitude, longitude: point.longitude)
        return CGPoint(x: p.first?.doubleValue ?? 0, y: p.second?.doubleValue ?? 0)
    }

    private func apply(_ viewport: MapViewport) {
        centerLatitude = viewport.centerLatitude
        centerLongitude = viewport.centerLongitude
        zoom = viewport.zoom
    }

    private func recenter() {
        following = true
        zoom = Self.defaultZoom
        if let position {
            centerLatitude = position.latitude
            centerLongitude = position.longitude
        }
    }
}
