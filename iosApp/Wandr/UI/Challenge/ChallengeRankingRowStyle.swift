import SwiftUI

extension View {
    /// The background of a ranking row: one card of a group of connected cards (like `wandrSectionItem`), tinted in
    /// the brand color for the user's own row so it is easy to compare with the others.
    func challengeRankingRow(isFirst: Bool, isLast: Bool, isHighlighted: Bool) -> some View {
        let shape = UnevenRoundedRectangle(
            topLeadingRadius: isFirst ? 20 : 6, bottomLeadingRadius: isLast ? 20 : 6,
            bottomTrailingRadius: isLast ? 20 : 6, topTrailingRadius: isFirst ? 20 : 6, style: .continuous
        )
        return padding(.horizontal, 16)
            .padding(.vertical, 14)
            .background(Color(.secondarySystemGroupedBackground), in: shape)
            .overlay {
                if isHighlighted {
                    shape.fill(Color.wandrPrimary.opacity(0.16)).allowsHitTesting(false)
                    shape.strokeBorder(Color.wandrPrimary.opacity(0.6), lineWidth: 1.5)
                }
            }
    }
}

#Preview("Light Mode") {
    VStack(spacing: 2) {
        Text("First").frame(maxWidth: .infinity, alignment: .leading).challengeRankingRow(isFirst: true, isLast: false, isHighlighted: false)
        Text("Mine").frame(maxWidth: .infinity, alignment: .leading).challengeRankingRow(isFirst: false, isLast: true, isHighlighted: true)
    }
    .padding()
    .background(Color(.systemGroupedBackground))
    .preferredColorScheme(.light)
}

#Preview("Dark Mode") {
    Text("Only").frame(maxWidth: .infinity, alignment: .leading).challengeRankingRow(isFirst: true, isLast: true, isHighlighted: true)
        .padding()
        .background(Color(.systemGroupedBackground))
        .preferredColorScheme(.dark)
}

#Preview("Large Text") {
    Text("Large").frame(maxWidth: .infinity, alignment: .leading).challengeRankingRow(isFirst: true, isLast: true, isHighlighted: false)
        .padding()
        .environment(\.dynamicTypeSize, .accessibility2)
}
