import SwiftUI

struct TeamInviteQRCodeView: View {
    let teamName: String
    let inviteCode: String

    var body: some View {
        VStack(spacing: 24) {
            Text(LocalizedStringKey("qr_invite_title"))
                .font(.system(size: 28, weight: .bold, design: .rounded))
                .foregroundColor(.primary)

            Text(teamName)
                .font(.title2)
                .foregroundColor(.secondary)

            ZStack {
                RoundedRectangle(cornerRadius: 20)
                    .fill(.ultraThinMaterial)
                    .frame(width: 260, height: 260)
                    .shadow(radius: 6)

                VStack(spacing: 16) {
                    RoundedRectangle(cornerRadius: 12)
                        .fill(Color.wandrPrimary)
                        .frame(width: 140, height: 140)

                    Text(inviteCode)
                        .font(.system(size: 24, weight: .bold, design: .monospaced))
                        .foregroundColor(.primary)
                }
            }

            Text(LocalizedStringKey("scan_qr_to_join"))
                .font(.footnote)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal)

            Spacer()
        }
        .padding(24)
        .background(
            LinearGradient(
                colors: [Color.wandrPrimary.opacity(0.1), Color.wandrSecondary.opacity(0.05)],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            .ignoresSafeArea()
        )
    }
}

#Preview("Light Mode") {
    TeamInviteQRCodeView(teamName: "Alpine Trail Blazers", inviteCode: "X7K9P2W1")
        .preferredColorScheme(.light)
}

#Preview("Dark Mode") {
    TeamInviteQRCodeView(teamName: "Alpine Trail Blazers", inviteCode: "X7K9P2W1")
        .preferredColorScheme(.dark)
}
