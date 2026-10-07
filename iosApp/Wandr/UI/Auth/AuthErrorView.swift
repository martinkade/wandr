import SwiftUI
@preconcurrency import shared

/// The error under the sign-in / sign-up form.
struct AuthErrorView: View {
    let error: AppError

    var body: some View {
        Text(error.userMessage)
            .font(.footnote)
            .foregroundStyle(error.isInformational ? Color.wandrAccentText : Color.red)
            .multilineTextAlignment(.center)
            .frame(maxWidth: .infinity)
    }
}

#Preview("Error") {
    AuthErrorView(error: AppError.InvalidCredentials(cause: nil)).padding()
}
#Preview("Info Dark") {
    AuthErrorView(error: AppError.EmailConfirmationRequired()).padding().preferredColorScheme(.dark)
}
