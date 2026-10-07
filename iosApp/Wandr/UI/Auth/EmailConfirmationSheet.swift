import SwiftUI

/// Shown after a sign-up that needs the email address to be confirmed first; leads on to the sign-in screen.
struct EmailConfirmationSheet: View {
    var onContinue: () -> Void = {
    }

    var body: some View {
        VStack(spacing: 16) {
            Text(LocalizedStringKey("registration_success_title"))
                .font(.title2.weight(.semibold))
            Text(LocalizedStringKey("registration_success_confirm_email"))
                .font(.body)
                .multilineTextAlignment(.center)
                .foregroundStyle(.secondary)
            Button(action: onContinue) {
                Text(LocalizedStringKey("registration_sign_in_now"))
                    .font(.headline)
                    .foregroundColor(.wandrOnPrimary)
                    .frame(maxWidth: .infinity)
                    .frame(height: 50)
                    .background(Color.wandrPrimary)
                    .cornerRadius(12)
            }
            .padding(.top, 8)
        }
        .padding(24)
    }
}

#Preview("Light Mode") {
    EmailConfirmationSheet()
}
#Preview("Dark Mode") {
    EmailConfirmationSheet().preferredColorScheme(.dark)
}
