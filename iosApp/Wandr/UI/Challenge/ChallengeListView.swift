import SwiftUI

struct ChallengeListView: View {
    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            ScrollView {
                VStack(spacing: 12) {
                    ChallengeCardView(
                        title: "30-Day 100km Hike",
                        description: "Hike 100km total within 30 days.",
                        status: "active",
                        requireAllMembersCompletion: true
                    )

                    ChallengeCardView(
                        title: "5000m Altitude Climb",
                        description: "Climb 5000m total within 30 days.",
                        status: "active",
                        requireAllMembersCompletion: false
                    )
                }
            }

            Spacer()
        }
        .padding(24)
        .background(
            LinearGradient(
                colors: [Color.wandrSecondary.opacity(0.08), Color.wandrPrimary.opacity(0.05)],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            .ignoresSafeArea()
        )
    }
}

#Preview("Light Mode") {
    ChallengeListView()
        .preferredColorScheme(.light)
}

#Preview("Dark Mode") {
    ChallengeListView()
        .preferredColorScheme(.dark)
}
