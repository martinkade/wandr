import SwiftUI

/// Init → (Login | Main).
struct RootView: View {
    @StateObject private var viewModel = RootViewModel()

    var body: some View {
        Group {
            switch viewModel.destination {
            case .initializing:
                InitView()
            case .failed(let message):
                InitView(errorMessage: message, onRetry: viewModel.retry)
            case .login:
                LoginView(
                    error: viewModel.loginState.error,
                    isLoading: viewModel.loginState.isLoading,
                    onLogin: viewModel.login,
                    onNavigateToRegister: { viewModel.destination = .register }
                )
            case .register:
                RegisterView(
                    error: viewModel.loginState.error,
                    isLoading: viewModel.loginState.isLoading,
                    needsEmailConfirmation: viewModel.loginState.needsEmailConfirmation,
                    onContinueToLogin: viewModel.continueToLogin,
                    onRegister: viewModel.register,
                    onNavigateToLogin: { viewModel.destination = .login }
                )
            case .main:
                MainView(onLoggedOut: { viewModel.destination = .login })
            }
        }
        .animation(.default, value: String(describing: viewModel.destination))
        .task { viewModel.start() }
    }
}
