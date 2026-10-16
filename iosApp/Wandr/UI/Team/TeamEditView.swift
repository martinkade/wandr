import SwiftUI

/// Name and description of a team in a sheet, used to create a team and to edit one (cover and photo are changed
/// from the details menu). Cancel / save live in the sheet's toolbar.
struct TeamEditView: View {
    var title: LocalizedStringKey = "team_edit_title"
    var saveTitle: LocalizedStringKey = "save_profile_button"
    @Binding var name: String
    @Binding var teamDescription: String
    var isSaving: Bool = false
    @Binding var snackbar: SnackbarMessage?
    var onSave: () -> Void = {}
    var onCancel: () -> Void = {}

    @FocusState private var nameFocused: Bool

    var body: some View {
        NavigationStack {
            Form {
                Section(LocalizedStringKey("team_name_label")) {
                    TextField(LocalizedStringKey("team_name_label"), text: $name)
                        .focused($nameFocused)
                        .submitLabel(.next)
                }
                Section(LocalizedStringKey("team_description_label")) {
                    TextField(LocalizedStringKey("team_description_label"), text: $teamDescription, axis: .vertical)
                        .lineLimit(3...8)
                }
            }
            .disabled(isSaving)
            .navigationTitle(title)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(LocalizedStringKey("cancel_button"), action: onCancel)
                        .disabled(isSaving)
                }
                ToolbarItem(placement: .confirmationAction) {
                    if isSaving {
                        ProgressView()
                    } else {
                        Button(saveTitle, action: onSave)
                            .fontWeight(.semibold)
                            .disabled(name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                    }
                }
            }
        }
        .snackbar($snackbar) // messages while the sheet is open (errors)
        .onAppear { if name.isEmpty { nameFocused = true } }
    }
}

#Preview("Edit, Light") {
    TeamEditView(name: .constant("Alpine Trail Blazers"), teamDescription: .constant("Hiking team for weekend trips."),
                 snackbar: .constant(nil))
        .preferredColorScheme(.light)
}

#Preview("Create, Dark") {
    TeamEditView(title: "create_team_title", saveTitle: "create_team_button",
                 name: .constant(""), teamDescription: .constant(""), snackbar: .constant(nil))
        .preferredColorScheme(.dark)
}

#Preview("Saving, large text") {
    TeamEditView(name: .constant("Alpine Trail Blazers"), teamDescription: .constant(""), isSaving: true,
                 snackbar: .constant(SnackbarMessage(text: "Save failed", isError: true)))
        .dynamicTypeSize(.accessibility2)
}
