import SwiftUI

struct CreateChallengeView: View {
    @State private var titleInput: String = ""
    @State private var descriptionInput: String = ""
    @State private var requireAllMembers: Bool = false
    var onCreateChallenge: (String, String, Bool) -> Void = { _, _, _ in }

    var body: some View {
        VStack(spacing: 24) {
            Text(LocalizedStringKey("create_challenge_title"))
                .font(.system(size: 28, weight: .bold, design: .rounded))
                .foregroundColor(.primary)

            VStack(spacing: 16) {
                TextField(LocalizedStringKey("challenge_title_label"), text: $titleInput)
                    .textFieldStyle(.plain)
                    .padding()
                    .background(.ultraThinMaterial)
                    .cornerRadius(12)

                TextEditor(text: $descriptionInput)
                    .frame(height: 100)
                    .padding(8)
                    .background(.ultraThinMaterial)
                    .cornerRadius(12)

                Toggle(LocalizedStringKey("require_all_members_label"), isOn: $requireAllMembers)
                    .padding()
                    .background(.ultraThinMaterial)
                    .cornerRadius(12)
            }

            Button(action: {
                onCreateChallenge(titleInput, descriptionInput, requireAllMembers)
            }) {
                Text(LocalizedStringKey("create_challenge_title"))
                    .font(.headline)
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .frame(height: 50)
                    .background(Color.blue)
                    .cornerRadius(12)
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
    CreateChallengeView()
        .preferredColorScheme(.light)
}

#Preview("Dark Mode") {
    CreateChallengeView()
        .preferredColorScheme(.dark)
}
