import SwiftUI

struct ChallengeCardView: View {
    let title: String
    let description: String?
    let status: String
    let requireAllMembersCompletion: Bool
    var onSelect: () -> Void = {}
    var onJoin: () -> Void = {}

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text(title)
                    .font(.headline)
                    .foregroundColor(.primary)
                Spacer()
                Text(status.uppercased())
                    .font(.caption)
                    .fontWeight(.bold)
                    .foregroundColor(.wandrAccentText)
            }

            if let desc = description {
                Text(desc)
                    .font(.subheadline)
                    .foregroundColor(.secondary)
            }

            if requireAllMembersCompletion {
                HStack(spacing: 6) {
                    Image(systemName: "lock.fill")
                        .font(.caption)
                        .foregroundColor(.wandrSecondary)
                    Text(LocalizedStringKey("require_all_members_label"))
                        .font(.caption)
                        .foregroundColor(.wandrSecondary)
                }
            }

            Button(action: onJoin) {
                Text(LocalizedStringKey("join_challenge_button"))
                    .font(.subheadline)
                    .fontWeight(.semibold)
                    .foregroundColor(.wandrOnPrimary)
                    .frame(maxWidth: .infinity)
                    .frame(height: 44)
                    .background(Color.wandrPrimary)
                    .cornerRadius(10)
            }
        }
        .padding(16)
        .background(.ultraThinMaterial)
        .cornerRadius(16)
        .onTapGesture {
            onSelect()
        }
    }
}

#Preview("Light Mode") {
    ChallengeCardView(
        title: "30-Day 100km Hike",
        description: "Hike 100km total within 30 days.",
        status: "active",
        requireAllMembersCompletion: true
    )
    .preferredColorScheme(.light)
}

#Preview("Dark Mode") {
    ChallengeCardView(
        title: "30-Day 100km Hike",
        description: "Hike 100km total within 30 days.",
        status: "active",
        requireAllMembersCompletion: true
    )
    .preferredColorScheme(.dark)
}
