import SwiftUI
@preconcurrency import shared

struct RegisterView: View {
    @State private var emailInput: String = ""
    @State private var passwordInput: String = ""
    var error: AppError? = nil
    var isLoading: Bool = false
    var needsEmailConfirmation: Bool = false
    var onContinueToLogin: () -> Void = {
    }
    var onRegister: (String, String) -> Void = { _, _ in }
    var onNavigateToLogin: () -> Void = {}

    var body: some View {
        ContourBackground {
        VStack(spacing: 20) {
            Spacer()

            AppLogoView()

            Text(LocalizedStringKey("join_wandr_title"))
                .font(.system(size: 36, weight: .bold, design: .rounded))
                .foregroundColor(.primary)
            
            Text(LocalizedStringKey("join_wandr_subtitle"))
                .font(.subheadline)
                .foregroundColor(.secondary)

            Text(LocalizedStringKey("register_title"))
                .font(.title2.weight(.semibold))
                .padding(.top, 12)

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
            .padding(.top, 4)

            if let error {
                AuthErrorView(error: error)
            }

            Button(action: {
                onRegister(emailInput, passwordInput)
            }) {
                Text(LocalizedStringKey("create_account_button"))
                    .font(.headline)
                    .foregroundColor(.wandrOnPrimary)
                    .frame(maxWidth: .infinity)
                    .frame(height: 50)
                    .background(Color.wandrPrimary)
                    .cornerRadius(12)
            }
            .padding(.top, 12)

            Button(action: onNavigateToLogin) {
                Text(LocalizedStringKey("already_have_account"))
                    .font(.footnote)
                    .foregroundColor(.wandrAccentText)
            }

            Spacer()
        }
        .padding(24)
        }
        .sheet(isPresented: Binding(get: { needsEmailConfirmation }, set: { if !$0 {
            onContinueToLogin()
        } })) {
            EmailConfirmationSheet(onContinue: onContinueToLogin)
                .presentationDetents([.height(300)])
                .presentationDragIndicator(.visible)
        }
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

#Preview("Confirm email") {
    RegisterView(needsEmailConfirmation: true)
}
