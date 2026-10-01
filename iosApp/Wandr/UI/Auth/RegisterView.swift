import SwiftUI

struct RegisterView: View {
    @State private var emailInput: String = ""
    @State private var passwordInput: String = ""
    var onRegister: (String, String) -> Void = { _, _ in }
    var onNavigateToLogin: () -> Void = {}

    var body: some View {
        VStack(spacing: 20) {
            Spacer()
            
            Text(LocalizedStringKey("join_wandr_title"))
                .font(.system(size: 36, weight: .bold, design: .rounded))
                .foregroundColor(.primary)
            
            Text(LocalizedStringKey("join_wandr_subtitle"))
                .font(.subheadline)
                .foregroundColor(.secondary)

            VStack(spacing: 16) {
                TextField(LocalizedStringKey("email_address_label"), text: $emailInput)
                    .textFieldStyle(.plain)
                    .padding()
                    .background(.ultraThinMaterial)
                    .cornerRadius(12)
                    .autocapitalization(.none)

                SecureField(LocalizedStringKey("password_hint"), text: $passwordInput)
                    .textFieldStyle(.plain)
                    .padding()
                    .background(.ultraThinMaterial)
                    .cornerRadius(12)
            }
            .padding(.top, 24)

            Button(action: {
                onRegister(emailInput, passwordInput)
            }) {
                Text(LocalizedStringKey("create_account_button"))
                    .font(.headline)
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .frame(height: 50)
                    .background(Color.blue)
                    .cornerRadius(12)
            }
            .padding(.top, 12)

            Button(action: onNavigateToLogin) {
                Text(LocalizedStringKey("already_have_account"))
                    .font(.footnote)
                    .foregroundColor(.blue)
            }

            Spacer()
        }
        .padding(24)
        .background(
            LinearGradient(
                colors: [Color.purple.opacity(0.1), Color.blue.opacity(0.05)],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            .ignoresSafeArea()
        )
    }
}

#Preview("Light Mode") {
    RegisterView()
        .preferredColorScheme(.light)
}

#Preview("Dark Mode") {
    RegisterView()
        .preferredColorScheme(.dark)
}
