import SwiftUI

/// One fact about a challenge: a symbol, a line of text and optionally a smaller line below.
struct ChallengeInfoRowView: View {
    let systemImage: String
    let text: String
    var secondaryText: String? = nil

    var body: some View {
        HStack(alignment: .firstTextBaseline, spacing: 14) {
            Image(systemName: systemImage)
                .font(.body.weight(.semibold))
                .foregroundStyle(Color.wandrAccentText)
                .frame(width: 26)
            VStack(alignment: .leading, spacing: 2) {
                Text(text).font(.body)
                if let secondaryText {
                    Text(secondaryText).font(.footnote).foregroundStyle(.secondary)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .accessibilityElement(children: .combine)
    }
}

#Preview("Light Mode") {
    VStack(spacing: 16) {
        ChallengeInfoRowView(systemImage: "calendar", text: "30 days · Ends in 25 days", secondaryText: "1 Oct 2026, 10:00 – 31 Oct 2026, 10:00")
        ChallengeInfoRowView(systemImage: "flag.fill", text: "Distance: 100 km")
    }
    .padding()
    .preferredColorScheme(.light)
}

#Preview("Dark Mode") {
    ChallengeInfoRowView(systemImage: "trophy.fill", text: "Team (team vs. team)", secondaryText: "Every member of a team must complete the target")
        .padding()
        .preferredColorScheme(.dark)
}

#Preview("Large Text") {
    ChallengeInfoRowView(systemImage: "figure.run", text: "Hiking, Running")
        .padding()
        .environment(\.dynamicTypeSize, .accessibility3)
}
