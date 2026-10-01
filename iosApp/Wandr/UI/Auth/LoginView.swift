import SwiftUI

struct LoginView: View {
    @State private var emailInput: String = ""
    @State private var passwordInput: String = ""
    var onLogin: (String, String) -> Void = { _, _ in }
    var onNavigateToRegister: () -> Void = {}

    var body: some View {
        VStack(spacing: 20) {
            Spacer()
            
            Text(LocalizedStringKey("app_name"))
                .font(.system(size: 36, weight: .bold, design: .rounded))
                .foregroundColor(.primary)
            
            Text(LocalizedStringKey("app_tagline"))
                .font(.subheadline)
                .foregroundColor(.secondary)

            VStack(spacing: 16) {
                TextField(LocalizedStringKey("email_label"), text: $emailInput)
                    .textFieldStyle(.plain)
                    .padding()
                    .background(.ultraThinMaterial)
                    .cornerRadius(12)
                    .autocapitalization(.none)

                SecureField(LocalizedStringKey("password_label"), text: $passwordInput)
                    .textFieldStyle(.plain)
                    .padding()
                    .background(.ultraThinMaterial)
                    .cornerRadius(12)
            }
            .padding(.top, 24)

            Button(action: {
                onLogin(emailInput, passwordInput)
            }) {
                Text(LocalizedStringKey("sign_in_button"))
                    .font(.headline)
                    .foregroundColor(.wandrOnPrimary)
                    .frame(maxWidth: .infinity)
                    .frame(height: 50)
                    .background(Color.wandrPrimary)
                    .cornerRadius(12)
            }
            .padding(.top, 12)

            Button(action: onNavigateToRegister) {
                Text(LocalizedStringKey("dont_have_account"))
                    .font(.footnote)
                    .foregroundColor(.wandrAccentText)
            }

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
    LoginView()
        .preferredColorScheme(.light)
}

#Preview("Dark Mode") {
    LoginView()
        .preferredColorScheme(.dark)
}
