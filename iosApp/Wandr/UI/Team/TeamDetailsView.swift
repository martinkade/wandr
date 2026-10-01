import SwiftUI

struct TeamDetailsView: View {
    let teamName: String
    let description: String?
    let coverUrl: String?
    let inviteCode: String
    var onShowQRCode: () -> Void = {}
    var onViewMembers: () -> Void = {}

    var body: some View {
        VStack(spacing: 24) {
            // Team Cover Header Banner
            ZStack {
                RoundedRectangle(cornerRadius: 16)
                    .fill(LinearGradient(colors: [Color.blue, Color.purple], startPoint: .topLeading, endPoint: .bottomTrailing))
                    .frame(height: 140)

                Text(teamName)
                    .font(.system(size: 32, weight: .bold, design: .rounded))
                    .foregroundColor(.white)
                    .multilineTextAlignment(.center)
                    .padding()
            }

            VStack(alignment: .leading, spacing: 12) {
                if let desc = description {
                    Text(desc)
                        .font(.body)
                        .foregroundColor(.secondary)
                }

                Text("Invite Code: \(inviteCode)")
                    .font(.headline)
                    .foregroundColor(.blue)
            }
            .padding(20)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(.ultraThinMaterial)
            .cornerRadius(16)

            Button(action: onShowQRCode) {
                Text(LocalizedStringKey("qr_invite_title"))
                    .font(.headline)
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .frame(height: 50)
                    .background(Color.blue)
                    .cornerRadius(12)
            }

            Button(action: onViewMembers) {
                Text(LocalizedStringKey("team_members_title"))
                    .font(.headline)
                    .foregroundColor(.blue)
                    .frame(maxWidth: .infinity)
                    .frame(height: 50)
                    .background(.ultraThinMaterial)
                    .cornerRadius(12)
            }

            Spacer()
        }
        .padding(24)
        .background(
            LinearGradient(
                colors: [Color.purple.opacity(0.08), Color.blue.opacity(0.05)],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            .ignoresSafeArea()
        )
    }
}

#Preview("Light Mode") {
    TeamDetailsView(teamName: "Alpine Trail Blazers", description: "Weekend hiking group.", coverUrl: nil, inviteCode: "X7K9P2W1")
        .preferredColorScheme(.light)
}

#Preview("Dark Mode") {
    TeamDetailsView(teamName: "Alpine Trail Blazers", description: "Weekend hiking group.", coverUrl: nil, inviteCode: "X7K9P2W1")
        .preferredColorScheme(.dark)
}
