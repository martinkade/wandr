import SwiftUI

struct MemberListView: View {
    let members: [String] = ["Martin Kade (Admin)", "Alex Runner (Member)", "Sophie Hiker (Member)"]

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text(LocalizedStringKey("team_members_title"))
                .font(.system(size: 28, weight: .bold, design: .rounded))
                .foregroundColor(.primary)

            ScrollView {
                VStack(spacing: 12) {
                    ForEach(members, id: \.self) { member in
                        HStack {
                            Text(member)
                                .font(.headline)
                                .foregroundColor(.primary)
                            Spacer()
                        }
                        .padding()
                        .background(.ultraThinMaterial)
                        .cornerRadius(12)
                    }
                }
            }

            Spacer()
        }
        .padding(24)
        .background(
            LinearGradient(
                colors: [Color.blue.opacity(0.08), Color.purple.opacity(0.05)],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            .ignoresSafeArea()
        )
    }
}

#Preview("Light Mode") {
    MemberListView()
        .preferredColorScheme(.light)
}

#Preview("Dark Mode") {
    MemberListView()
        .preferredColorScheme(.dark)
}
