import SwiftUI

struct ReactionChipView: View {
    let emoji: String
    let count: Int
    let isSelected: Bool
    var onTap: () -> Void = {}

    var body: some View {
        Button(action: onTap) {
            Text("\(emoji) \(count)")
                .font(.caption)
                .padding(.horizontal, 10)
                .padding(.vertical, 5)
                .background(isSelected ? Color.wandrPrimary.opacity(0.35) : Color.secondary.opacity(0.15), in: Capsule())
                .overlay(Capsule().stroke(isSelected ? Color.wandrAccentText : .clear, lineWidth: 1))
        }
        .buttonStyle(.plain)
    }
}

#Preview("Light Mode") {
    HStack { ReactionChipView(emoji: "👍", count: 3, isSelected: true); ReactionChipView(emoji: "🔥", count: 1, isSelected: false) }
        .padding().preferredColorScheme(.light)
}

#Preview("Dark Mode") {
    HStack { ReactionChipView(emoji: "👍", count: 3, isSelected: true); ReactionChipView(emoji: "🔥", count: 1, isSelected: false) }
        .padding().preferredColorScheme(.dark)
}
