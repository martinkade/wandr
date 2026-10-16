import SwiftUI
@preconcurrency import shared

/// The image area at the top of a challenge: the cover photo, or without one the brand gradient with white elevation
/// lines (a different pattern per seed, like Android's CoverHero). It fills whatever frame it gets.
struct ChallengeCoverView: View {
    let coverUrl: String?
    let seed: Int32
    var isBusy: Bool = false
    /// A dark fade at the top keeps toolbar buttons readable on bright photos.
    var showsTopScrim: Bool = false

    var body: some View {
        ZStack {
            LinearGradient(colors: [Color.wandrPrimary, Color.wandrSecondary], startPoint: .topLeading, endPoint: .bottomTrailing)
            contours
            if let coverUrl, let url = URL(string: coverUrl) {
                AsyncImage(url: url) { image in
                    image.resizable().scaledToFill()
                } placeholder: {
                    Color.clear
                }
                .accessibilityLabel(Text(LocalizedStringKey("team_cover_content_description")))
            }
            if showsTopScrim {
                VStack {
                    LinearGradient(colors: [.black.opacity(0.35), .clear], startPoint: .top, endPoint: .bottom).frame(height: 120)
                    Spacer(minLength: 0)
                }
            }
            if isBusy {
                Color.black.opacity(0.4)
                ProgressView().tint(.white).controlSize(.large)
            }
        }
        .clipped()
    }

    private var contours: some View {
        GeometryReader { geo in
            // Rounded aspect ratio, so ContourCache can reuse the lines across similar sizes.
            let aspect = max(0.2, (geo.size.width / max(geo.size.height, 1) * 10).rounded() / 10)
            let paths = ContourCache.shared.paths(aspect: aspect, seed: seed)
            Canvas { context, size in
                let scale = CGAffineTransform(scaleX: size.width, y: size.height)
                context.stroke(paths.regular.applying(scale), with: .color(.white.opacity(0.25)), lineWidth: 0.8)
                context.stroke(paths.index.applying(scale), with: .color(.white.opacity(0.5)), lineWidth: 1.4)
            }
        }
        .allowsHitTesting(false)
        .accessibilityHidden(true)
    }
}

#Preview("Light Mode") {
    ChallengeCoverView(coverUrl: nil, seed: 42).frame(height: 128).preferredColorScheme(.light)
}

#Preview("Dark Mode") {
    ChallengeCoverView(coverUrl: nil, seed: 7, showsTopScrim: true).frame(height: 260).preferredColorScheme(.dark)
}

#Preview("Busy iPad", traits: .fixedLayout(width: 800, height: 300)) {
    ChallengeCoverView(coverUrl: nil, seed: 3, isBusy: true)
}
