import SwiftUI
@preconcurrency import shared

/// Stateless activity details: the route as a stretchy parallax header (a gradient hero without a route) and a sheet
/// sliding over its lower edge with owner, title, description, figures, source, hints and the social bar.
struct ActivityDetailContentView<SocialBar: View>: View {
    let activity: Activity
    let authorName: String
    let authorAvatarUrl: String?
    /// The full track from this device's FIT file, otherwise the simplified route of the activity.
    let route: [GpsTrackpoint]
    let isOwn: Bool
    /// Becomes true once the header has scrolled under the navigation bar (the bar then shows the title).
    var isHeaderCollapsed: Binding<Bool> = .constant(false)
    @ViewBuilder var socialBar: () -> SocialBar

    private let headerHeight: CGFloat = 340
    private let sheetOverlap: CGFloat = 28
    private var hasMap: Bool { route.count >= 2 }

    var body: some View {
        ScrollView {
            VStack(spacing: 0) {
                header
                sheet
                    .padding(.top, -sheetOverlap)
                    .zIndex(1)
            }
        }
        .ignoresSafeArea(edges: .top)
        .background(Color(.systemGroupedBackground))
    }

    private var header: some View {
        GeometryReader { geo in
            let minY = geo.frame(in: .scrollView).minY
            headerContent
                .frame(width: geo.size.width, height: headerHeight)
                // Pulled down: grows from the bottom edge; scrolled up: moves at half speed (parallax).
                .scaleEffect(minY > 0 ? (headerHeight + minY) / headerHeight : 1, anchor: .bottom)
                .offset(y: minY > 0 ? 0 : -minY * 0.5)
                .onChange(of: minY < -(headerHeight - 140)) { _, collapsed in
                    if isHeaderCollapsed.wrappedValue != collapsed { isHeaderCollapsed.wrappedValue = collapsed }
                }
        }
        .frame(height: headerHeight)
    }

    @ViewBuilder
    private var headerContent: some View {
        if hasMap {
            // The route stays clear of the navigation bar and of the sheet over the lower edge.
            TrackMapView(trackpoints: route, height: headerHeight, topPadding: 110, bottomPadding: 32 + sheetOverlap, cornerRadius: 0)
                .overlay(alignment: .top) {
                    // Keeps the glass buttons of the bar readable on any map.
                    LinearGradient(colors: [.black.opacity(0.3), .clear], startPoint: .top, endPoint: .bottom)
                        .frame(height: 110)
                        .allowsHitTesting(false)
                }
        } else {
            ZStack {
                LinearGradient(
                    colors: [Color.wandrPrimary, Color.wandrPrimary.opacity(0.45), Color.wandrSecondary.opacity(0.5)],
                    startPoint: .topLeading, endPoint: .bottomTrailing
                )
                Image(systemName: ActivityFormat.typeSymbol(activity.activityType))
                    .font(.system(size: 96, weight: .semibold))
                    .foregroundStyle(Color.wandrOnPrimary.opacity(0.25))
                    .offset(y: -sheetOverlap / 2)
            }
        }
    }

    private var sheet: some View {
        VStack(alignment: .leading, spacing: 20) {
            ActivityOwnerView(
                authorName: authorName, authorAvatarUrl: authorAvatarUrl,
                activityType: activity.activityType, startTime: activity.startTime
            )

            VStack(alignment: .leading, spacing: 8) {
                Text(activity.title)
                    .font(.title.weight(.bold))
                if let description = activity.description_, !description.isEmpty {
                    Text(description)
                        .font(.body)
                        .foregroundStyle(.secondary)
                }
            }

            VStack(alignment: .leading, spacing: 16) {
                ActivityStatsView(
                    activityType: activity.activityType, distanceMeters: activity.distanceMeters,
                    durationSeconds: activity.durationSeconds, elevationGainMeters: activity.elevationGainMeters
                )
                Divider()
                LabeledContent {
                    Label(
                        LocalizedStringKey(activity.isManualEntry ? "manual_entry" : "gps_tracked"),
                        systemImage: activity.isManualEntry ? "square.and.pencil" : "location.fill"
                    )
                    .foregroundStyle(Color.wandrAccentText)
                } label: {
                    Text(LocalizedStringKey("activity_source_label")).foregroundStyle(.secondary)
                }
                .font(.subheadline)
            }
            .padding(16)
            .wandrCard()

            if let hint {
                Label(hint, systemImage: hasMap ? "eye.slash" : "iphone")
                    .font(.footnote)
                    .foregroundStyle(.secondary)
            }

            socialBar()
        }
        .padding(20)
        .padding(.bottom, 24)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            Color(.systemGroupedBackground),
            in: UnevenRoundedRectangle(topLeadingRadius: sheetOverlap, topTrailingRadius: sheetOverlap, style: .continuous)
        )
    }

    private var hint: LocalizedStringKey? {
        if hasMap {
            return isOwn && !activity.showMap ? "activity_map_hidden_hint" : nil
        }
        // No route: recorded on another device, or its owner keeps the map private.
        return activity.isManualEntry ? nil : "activity_track_local_hint"
    }
}

#Preview("Light Mode") {
    NavigationStack {
        ActivityDetailContentView(activity: .sample(), authorName: "Alex Kim", authorAvatarUrl: nil, route: Activity.sample().route, isOwn: true) {
            SocialBarView(likeCount: 12, likedByMe: true, commentCount: 3)
        }
    }
    .preferredColorScheme(.light)
}

#Preview("Dark Mode, no route") {
    NavigationStack {
        ActivityDetailContentView(activity: .sample(polyline: nil), authorName: "", authorAvatarUrl: nil, route: [], isOwn: false) {
            SocialBarView(likeCount: 0, likedByMe: false, commentCount: 0)
        }
    }
    .preferredColorScheme(.dark)
}

#Preview("Large Text") {
    NavigationStack {
        ActivityDetailContentView(activity: .sample(showMap: false), authorName: "Sophie", authorAvatarUrl: nil, route: Activity.sample().route, isOwn: true) {
            SocialBarView(likeCount: 4, likedByMe: false, commentCount: 1)
        }
    }
    .environment(\.dynamicTypeSize, .accessibility2)
}
