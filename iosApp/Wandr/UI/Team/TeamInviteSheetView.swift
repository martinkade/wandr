import SwiftUI

/// Full-screen invite with the QR code of a team's invite code, large enough for another phone to scan. The screen is
/// brighter while it is shown and returns to its brightness when it is closed.
struct TeamInviteSheetView: View {
    let teamName: String
    let inviteCode: String
    var onClose: () -> Void = {}

    var body: some View {
        ScrollView {
            VStack(spacing: 8) {
                Text(LocalizedStringKey("qr_invite_title"))
                    .font(.headline)
                    .foregroundStyle(.secondary)
                Text(teamName)
                    .font(.largeTitle.weight(.bold))
                    .foregroundStyle(Color.wandrAccentText)
                    .multilineTextAlignment(.center)

                VStack(spacing: 16) {
                    InviteQrCodeView(content: inviteCode, size: 240)
                    Text(inviteCode)
                        .font(.system(.title2, design: .monospaced).weight(.semibold))
                        .textSelection(.enabled)
                        .kerning(2)
                }
                .padding(24)
                .wandrCard(cornerRadius: 28)
                .padding(.top, 24)

                Text(LocalizedStringKey("scan_qr_to_join"))
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.center)
                    .padding(.top, 16)
            }
            .padding(.horizontal, 24)
            .padding(.top, 72)
            .padding(.bottom, 32)
            .frame(maxWidth: .infinity)
        }
        .background(Color(.systemGroupedBackground).ignoresSafeArea())
        .overlay(alignment: .topLeading) {
            Button(action: onClose) {
                Image(systemName: "xmark")
                    .font(.headline)
                    .frame(width: 44, height: 44)
            }
            .buttonStyle(.plain)
            .wandrGlassCircle()
            .padding(16)
            .accessibilityLabel(Text(LocalizedStringKey("close_button")))
        }
        .screenBrightnessBoost()
    }
}

#Preview("Light Mode") {
    TeamInviteSheetView(teamName: "Alpine Trail Blazers", inviteCode: "X7K9P2W1").preferredColorScheme(.light)
}

#Preview("Dark Mode") {
    TeamInviteSheetView(teamName: "Alpine Trail Blazers", inviteCode: "X7K9P2W1").preferredColorScheme(.dark)
}

#Preview("Large text") {
    TeamInviteSheetView(teamName: "City Runners", inviteCode: "Q3M8D5LA").dynamicTypeSize(.accessibility2)
}
