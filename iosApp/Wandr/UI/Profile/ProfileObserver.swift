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
    @Published var errorMessage: String?
    @Published var successMessage: String?

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
        errorMessage = state.errorMessage
        successMessage = state.successMessage
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
