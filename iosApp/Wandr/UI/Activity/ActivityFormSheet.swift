import SwiftUI
@preconcurrency import shared

/// Hosts the create / edit form of an activity screen: the form sheet (with the conflict wizard above it), closing it
/// after a successful save, and the messages of saves and deletes (snackbar; errors inside the open form instead).
private struct ActivityFormSheet: ViewModifier {
    @ObservedObject var observer: ActivityObserver
    @Binding var isPresented: Bool
    @Binding var message: SnackbarMessage?
    let userId: String
    let teamId: String?
    let onSuccess: (ActivitySuccess) -> Void

    // Only a save started from the form may close it.
    @State private var saveRequested = false
    @State private var formError: String?

    private var state: ActivityState { observer.state }
    /// Changes once per new message; repeated emissions of the same state must not show it twice.
    private var messageKey: String { "\(state.success.map { String(describing: $0) } ?? "")|\(state.error?.userMessage ?? "")" }

    func body(content: Content) -> some View {
        content
            .sheet(isPresented: Binding(get: { isPresented && state.form != nil }, set: { if !$0 { close() } })) {
                if let form = state.form {
                    ActivityEditView(
                        form: form,
                        isSaving: state.isSaving,
                        errorMessage: formError,
                        onIntent: { observer.send($0) },
                        onSave: {
                            formError = nil
                            saveRequested = true
                            observer.send(ActivityIntentSubmitForm(userId: userId, teamId: teamId))
                        },
                        onCancel: close
                    )
                    // Presented from the form: a sheet cannot be shown by a view that already presents one.
                    .sheet(isPresented: Binding(get: { state.conflict != nil }, set: { _ in })) {
                        if let conflict = state.conflict {
                            ActivityConflictView(
                                conflict: conflict, isSaving: state.isSaving,
                                onCancel: { observer.send(ActivityIntentDismissConflict.shared) },
                                onResolve: { observer.send(ActivityIntentResolveConflict(resolution: $0)) }
                            )
                        }
                    }
                }
            }
            .onChange(of: messageKey) { _, _ in handleMessage() }
    }

    private func handleMessage() {
        guard state.success != nil || state.error != nil else { return }
        if let success = state.success {
            if success != .deleted {
                if saveRequested && isPresented { close() }
                message = SnackbarMessage(text: success.message)
            }
            onSuccess(success)
        } else if let error = state.error {
            if isPresented {
                formError = error.userMessage
            } else {
                message = SnackbarMessage(text: error.userMessage, isError: true)
            }
        }
        saveRequested = false
        observer.send(ActivityIntentClearMessages.shared)
    }

    private func close() {
        isPresented = false
        formError = nil
        observer.send(ActivityIntentDiscardForm.shared)
    }
}

extension View {
    /// The form sheet of [observer] while [isPresented] and the view model has a form; see `ActivityFormSheet`.
    func activityFormSheet(
        observer: ActivityObserver, isPresented: Binding<Bool>, message: Binding<SnackbarMessage?>,
        userId: String, teamId: String?, onSuccess: @escaping (ActivitySuccess) -> Void = { _ in }
    ) -> some View {
        modifier(ActivityFormSheet(
            observer: observer, isPresented: isPresented, message: message, userId: userId, teamId: teamId, onSuccess: onSuccess
        ))
    }
}
