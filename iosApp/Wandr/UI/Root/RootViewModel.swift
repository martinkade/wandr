import SwiftUI
@preconcurrency import shared

enum RootDestination {
    case initializing
    case failed(String)
    case login
    case register
    case main
}

/// Bridges the shared StartupViewModel / LoginViewModel StateFlows into SwiftUI.
@MainActor
final class RootViewModel: ObservableObject {
    @Published var destination: RootDestination = .initializing
    @Published var loginState = LoginState(emailInput: "", passwordInput: "", isLoading: false, errorMessage: nil, isAuthenticated: false)

    private let startup = IosDependencies.shared.startupViewModel()
    private let login = IosDependencies.shared.loginViewModel()
    nonisolated(unsafe) private var jobs: [any Kotlinx_coroutines_coreJob] = []

    func start() {
        jobs.append(FlowObserverKt.watch(startup.uiState) { [weak self] value in
            guard let state = value as? StartupState else { return }
            Task { @MainActor in self?.handle(state) }
        })
        jobs.append(FlowObserverKt.watch(login.uiState) { [weak self] value in
            guard let state = value as? LoginState else { return }
            Task { @MainActor in
                guard let self else { return }
                self.loginState = state
                if state.isAuthenticated { self.destination = .main }
            }
        })
        startup.processIntent(intent: StartupIntentStart.shared)
    }

    func retry() { startup.processIntent(intent: StartupIntentRetry.shared) }

    func login(email: String, password: String) {
        login.processIntent(intent: LoginIntentEmailChanged(email: email))
        login.processIntent(intent: LoginIntentPasswordChanged(password: password))
        login.processIntent(intent: LoginIntentSubmitLogin.shared)
    }

    func register(email: String, password: String) {
        login.processIntent(intent: LoginIntentEmailChanged(email: email))
        login.processIntent(intent: LoginIntentPasswordChanged(password: password))
        login.processIntent(intent: LoginIntentSubmitRegister.shared)
    }

    private func handle(_ state: StartupState) {
        switch state {
        case is StartupStateAuthenticated: destination = .main
        case is StartupStateUnauthenticated: destination = .login
        case let failed as StartupStateFailed: destination = .failed(failed.message)
        default: destination = .initializing
        }
    }

    deinit { jobs.forEach { $0.cancel(cause: nil) } }
}
