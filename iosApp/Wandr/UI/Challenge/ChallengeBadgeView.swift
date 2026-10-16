import SwiftUI
@preconcurrency import shared

/// The emblem of a challenge: a golden hexagon with the symbol of what is measured (distance, elevation, time).
struct ChallengeBadgeView: View {
    let type: ChallengeType
    var size: CGFloat = 88

    var body: some View {
        ZStack {
            HexagonShape().fill(
                LinearGradient(colors: [Color.wandrPrimary, Color.wandrPrimary.opacity(0.8)], startPoint: .top, endPoint: .bottom)
            )
            HexagonShape()
                .fill(Color.wandrOnPrimary)
                .frame(width: size * 0.86, height: size * 0.86)
            Image(systemName: Self.symbol(type))
                .resizable()
                .scaledToFit()
                .fontWeight(.semibold)
                .foregroundStyle(Color.wandrPrimary)
                .frame(width: size * 0.42, height: size * 0.42)
        }
        .frame(width: size, height: size)
        .shadow(color: .black.opacity(0.18), radius: size * 0.06, y: size * 0.03)
        .accessibilityElement()
        .accessibilityLabel(Text(ChallengeText.typeLabel(type)))
    }

    static func symbol(_ type: ChallengeType) -> String {
        switch type {
        case .elevation: return "mountain.2.fill"
        case .time: return "clock.fill"
        default: return "point.topleft.down.to.point.bottomright.curvepath"
        }
    }
}

#Preview("Light Mode") {
    HStack(spacing: 16) {
        ChallengeBadgeView(type: .distance, size: 64)
        ChallengeBadgeView(type: .elevation, size: 64)
        ChallengeBadgeView(type: .time, size: 64)
    }
    .padding()
    .preferredColorScheme(.light)
}

#Preview("Dark Mode") {
    ChallengeBadgeView(type: .elevation).padding().preferredColorScheme(.dark)
}

#Preview("iPhone SE", traits: .fixedLayout(width: 320, height: 200)) {
    ChallengeBadgeView(type: .time, size: 120).padding()
}
