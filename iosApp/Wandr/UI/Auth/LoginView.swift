import SwiftUI
@preconcurrency import shared

struct LoginView: View {
    @State private var emailInput: String = ""
    @State private var passwordInput: String = ""
    var error: AppError? = nil
    var isLoading: Bool = false
    var onLogin: (String, String) -> Void = { _, _ in }
    var onNavigateToRegister: () -> Void = {}

    var body: some View {
        ContourBackground {
        VStack(spacing: 20) {
            Spacer()

            AppLogoView()

            Text(LocalizedStringKey("app_name"))
                .font(.system(size: 36, weight: .bold, design: .rounded))
                .foregroundColor(.primary)
            
            Text(LocalizedStringKey("app_tagline"))
                .font(.subheadline)
                .foregroundColor(.secondary)

            Text(LocalizedStringKey("login_title"))
                .font(.title2.weight(.semibold))
                .padding(.top, 12)

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
            .padding(.top, 4)

            if let error {
                AuthErrorView(error: error)
            }

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
        }
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
