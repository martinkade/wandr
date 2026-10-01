import SwiftUI

struct LeaderboardRowView: View {
    let rank: Int
    let displayName: String
    let progressPercentage: Int

    var body: some View {
        HStack {
            Text("#\(rank)")
                .font(.headline)
                .foregroundColor(.wandrAccentText)
                .frame(width: 36, alignment: .leading)

            Text(displayName)
                .font(.body)
                .foregroundColor(.primary)

            Spacer()

            Text("\(progressPercentage)%")
                .font(.headline)
                .foregroundColor(.wandrSecondary)
        }
        .padding()
        .background(.ultraThinMaterial)
        .cornerRadius(12)
    }
}

#Preview("Light Mode") {
    LeaderboardRowView(rank: 1, displayName: "Martin Kade", progressPercentage: 75)
        .preferredColorScheme(.light)
}

#Preview("Dark Mode") {
    LeaderboardRowView(rank: 1, displayName: "Martin Kade", progressPercentage: 75)
        .preferredColorScheme(.dark)
}
