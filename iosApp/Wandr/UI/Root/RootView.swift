import SwiftUI

/// Init → (Login | Dashboard).
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
                    onLogin: viewModel.login,
                    onNavigateToRegister: { viewModel.destination = .register }
                )
            case .register:
                RegisterView(
                    onRegister: viewModel.register,
                    onNavigateToLogin: { viewModel.destination = .login }
                )
            case .dashboard:
                DashboardView(onLoggedOut: { viewModel.destination = .login })
            }
        }
        .animation(.default, value: String(describing: viewModel.destination))
        .task { viewModel.start() }
    }
}
