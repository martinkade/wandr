import SwiftUI
@preconcurrency import shared

/// The runtime status of a challenge as a small glass chip floating over the cover.
struct ChallengeStatusChipView: View {
    let status: ChallengeStatus

    var body: some View {
        Text(ChallengeText.statusLabel(status))
            .textCase(.uppercase)
            .font(.caption2.weight(.bold))
            .tracking(0.6)
            .foregroundStyle(.white)
            .padding(.horizontal, 10)
            .padding(.vertical, 5)
            // Dark tint: the white label must stay readable on bright photos and on the gold fallback.
            .wandrGlassCapsule(tint: .black.opacity(0.35))
    }
}

#Preview("Light Mode") {
    ZStack {
        ChallengeCoverView(coverUrl: nil, seed: 1)
        HStack {
            ChallengeStatusChipView(status: .active)
            ChallengeStatusChipView(status: .draft)
            ChallengeStatusChipView(status: .planned)
        }
    }
    .frame(height: 128)
    .preferredColorScheme(.light)
}

#Preview("Dark Mode") {
    ZStack {
        ChallengeCoverView(coverUrl: nil, seed: 2)
        ChallengeStatusChipView(status: .expired)
    }
    .frame(height: 128)
    .preferredColorScheme(.dark)
}

#Preview("Large Text") {
    ZStack {
        ChallengeCoverView(coverUrl: nil, seed: 3)
        ChallengeStatusChipView(status: .completed)
    }
    .frame(height: 128)
    .environment(\.dynamicTypeSize, .accessibility2)
}
