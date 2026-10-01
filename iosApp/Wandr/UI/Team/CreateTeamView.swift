import SwiftUI

struct CreateTeamView: View {
    @State private var nameInput: String = ""
    @State private var descriptionInput: String = ""
    var onCreateTeam: (String, String) -> Void = { _, _ in }

    var body: some View {
        VStack(spacing: 24) {
            Text(LocalizedStringKey("create_team_title"))
                .font(.system(size: 28, weight: .bold, design: .rounded))
                .foregroundColor(.primary)

            VStack(spacing: 16) {
                TextField(LocalizedStringKey("team_name_label"), text: $nameInput)
                    .textFieldStyle(.plain)
                    .padding()
                    .background(.ultraThinMaterial)
                    .cornerRadius(12)

                TextEditor(text: $descriptionInput)
                    .frame(height: 100)
                    .padding(8)
                    .background(.ultraThinMaterial)
                    .cornerRadius(12)
            }

            Button(action: {
                onCreateTeam(nameInput, descriptionInput)
            }) {
                Text(LocalizedStringKey("create_team_button"))
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
    CreateTeamView()
        .preferredColorScheme(.light)
}

#Preview("Dark Mode") {
    CreateTeamView()
        .preferredColorScheme(.dark)
}
