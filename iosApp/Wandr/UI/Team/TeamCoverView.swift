@preconcurrency import shared
import SwiftUI

/// A team's cover: the photo, or elevation lines on the brand gradient while there is none (or it loads).
/// It fills whatever frame it gets (scaled to fill, clipped), so cards and the details header can size it freely.
struct TeamCoverView: View {
    var coverUrl: String? = nil
    /// Varies the elevation lines per team (see `seed(for:)`).
    var seed: Int32 = 7
    var isBusy: Bool = false

    var body: some View {
        GeometryReader { geo in
            ZStack {
                LinearGradient(
                    colors: [Color.wandrPrimary, Color.wandrPrimary.opacity(0.6)],
                    startPoint: .topLeading, endPoint: .bottomTrailing
                )
                contours(in: geo.size)

                if let coverUrl, let url = URL(string: coverUrl) {
                    AsyncImage(url: url) { image in
                        image.resizable().scaledToFill()
                    } placeholder: {
                        Color.clear
                    }
                    .frame(width: geo.size.width, height: geo.size.height)
                    .clipped()
                    .accessibilityLabel(Text(LocalizedStringKey("team_cover_content_description")))
                }

                if isBusy {
                    Color.black.opacity(0.35)
                    ProgressView().tint(.white)
                }
            }
            .frame(width: geo.size.width, height: geo.size.height)
        }
        .clipped()
    }

    private func contours(in size: CGSize) -> some View {
        // Rounded aspect keeps the cache small while the header stretches.
        let aspect = max(0.2, (size.width / max(size.height, 1) * 10).rounded() / 10)
        let paths = ContourCache.shared.paths(aspect: aspect, seed: seed)
        return Canvas { context, canvasSize in
            let scale = CGAffineTransform(scaleX: canvasSize.width, y: canvasSize.height)
            context.stroke(paths.regular.applying(scale), with: .color(.white.opacity(0.18)), lineWidth: 0.8)
            context.stroke(paths.index.applying(scale), with: .color(.white.opacity(0.32)), lineWidth: 1.4)
        }
        .allowsHitTesting(false)
        .accessibilityHidden(true)
    }

    /// A stable seed per team id, the same as Java's `String.hashCode()` that Android uses (Swift's `hashValue`
    /// changes with every launch).
    static func seed(for id: String) -> Int32 {
        id.utf16.reduce(Int32(0)) { $0 &* 31 &+ Int32($1) }
    }
}

#Preview("Light Mode") {
    TeamCoverView(seed: TeamCoverView.seed(for: "t1")).frame(height: 160).preferredColorScheme(.light)
}

#Preview("Dark Mode, busy") {
    TeamCoverView(seed: 3, isBusy: true).frame(height: 160).preferredColorScheme(.dark)
}

#Preview("iPad", traits: .fixedLayout(width: 820, height: 300)) {
    TeamCoverView(seed: 11)
}
