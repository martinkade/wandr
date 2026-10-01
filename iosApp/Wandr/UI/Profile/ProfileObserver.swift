@preconcurrency import shared
import SwiftUI

/// Bridges the shared ProfileViewModel into SwiftUI.
@MainActor
final class ProfileObserver: ObservableObject {
    @Published var displayName = ""
    @Published var bio = ""
    @Published var avatarUrl: String?
    @Published var isLoading = true
    @Published var isSaving = false
    @Published var isAvatarUpdating = false
    @Published var snackbar: SnackbarMessage?
    /// Increments after each successful save so the view can leave edit mode.
    @Published var saveCompletedCount = 0

    private let viewModel = IosDependencies.shared.profileViewModel()
    nonisolated(unsafe) private var job: (any Kotlinx_coroutines_coreJob)?
    private var userId: String?

    func load(userId: String) {
        guard self.userId != userId else { return }
        self.userId = userId
        job?.cancel(cause: nil)
        job = FlowObserverKt.watch(viewModel.uiState) { [weak self] value in
            guard let state = value as? ProfileState else { return }
            Task { @MainActor in self?.apply(state) }
        }
        viewModel.processIntent(intent: ProfileIntentLoadProfile(userId: userId))
    }

    func setDisplayName(_ name: String) {
        displayName = name
        viewModel.processIntent(intent: ProfileIntentDisplayNameChanged(name: name))
    }

    func setBio(_ text: String) {
        bio = text
        viewModel.processIntent(intent: ProfileIntentBioChanged(bio: text))
    }

    func save() { viewModel.processIntent(intent: ProfileIntentSaveProfile.shared) }

    /// Drops unsaved edits and shows the stored profile again.
    func discard() {
        // The next state (no unsaved changes) restores displayName / bio from the stored profile.
        viewModel.processIntent(intent: ProfileIntentDiscardChanges.shared)
    }

    func uploadAvatar(_ jpeg: Data) {
        guard let userId else { return }
        viewModel.processIntent(intent: ProfileIntentUploadAvatar(userId: userId, jpegBytes: KotlinByteArray.from(data: jpeg)))
    }

    func removeAvatar() {
        guard let userId else { return }
        viewModel.processIntent(intent: ProfileIntentRemoveAvatar(userId: userId))
    }

    private func apply(_ state: ProfileState) {
        avatarUrl = state.profile?.avatarUrl
        isLoading = state.isLoading
        isSaving = state.isSaving
        isAvatarUpdating = state.isAvatarUpdating
        if let error = state.errorMessage {
            snackbar = SnackbarMessage(text: error, isError: true)
            viewModel.processIntent(intent: ProfileIntentClearMessages.shared)
        } else if let success = state.success {
            switch success {
            case .profileSaved:
                snackbar = SnackbarMessage(text: String(localized: "profile_saved_message"))
                saveCompletedCount += 1
            case .avatarUpdated:
                snackbar = SnackbarMessage(text: String(localized: "profile_avatar_updated_message"))
            default: break
            }
            viewModel.processIntent(intent: ProfileIntentClearMessages.shared)
        }
        // While the user is editing, the local text fields are the source of truth.
        if !state.hasUnsavedChanges, let profile = state.profile {
            displayName = profile.displayName
            bio = profile.bio ?? ""
        }
    }

    deinit { job?.cancel(cause: nil) }
}

extension KotlinByteArray {
    static func from(data: Data) -> KotlinByteArray {
        let array = KotlinByteArray(size: Int32(data.count))
        data.enumerated().forEach { array.set(index: Int32($0.offset), value: Int8(bitPattern: $0.element)) }
        return array
    }
}
