import SwiftUI
@preconcurrency import shared

/// The create / edit form of an activity, shown in a sheet and driven by `ActivityState.form`. For recorded activities
/// only title, description, type and the route's visibility can change; the measured values are shown but locked.
struct ActivityEditView: View {
    let form: ActivityForm
    let isSaving: Bool
    /// An error of the last save, shown in the form (the snackbar of the screen is covered by the sheet).
    var errorMessage: String?
    var onIntent: (ActivityIntent) -> Void = { _ in }
    var onSave: () -> Void = {}
    var onCancel: () -> Void = {}

    // Local text so partial input like "12," is not rewritten while typing.
    @State private var distanceText = ""
    @State private var durationText = ""
    @State private var elevationText = ""

    private var locked: Bool { form.isMeasured }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    TextField(LocalizedStringKey("activity_title_label"), text: binding(form.title) { ActivityIntentTitleChanged(title: $0) })
                    TextField(
                        LocalizedStringKey("activity_description_label"),
                        text: binding(form.description_) { ActivityIntentDescriptionChanged(description: $0) },
                        axis: .vertical
                    )
                    .lineLimit(3...6)
                }

                Section(LocalizedStringKey("activity_type_label")) {
                    Picker(selection: binding(form.activityType) { ActivityIntentTypeChanged(type: $0) }) {
                        ForEach(ActivityTypes.shared.all, id: \.self) { type in
                            Text(ActivityFormat.typeName(type)).tag(type)
                        }
                    } label: { EmptyView() }
                    .pickerStyle(.segmented)
                    .listRowBackground(Color.clear)
                    .listRowInsets(EdgeInsets())
                }

                Section {
                    numberRow("distance_label", text: $distanceText) { ActivityIntentDistanceChanged(km: $0) }
                    numberRow("duration_label", text: $durationText) { ActivityIntentDurationChanged(minutes: $0) }
                    numberRow("elevation_label", text: $elevationText) { ActivityIntentElevationChanged(meters: $0) }
                    DatePicker(
                        LocalizedStringKey("activity_start_label"),
                        selection: Binding(
                            get: { ActivityFormat.date(millis: form.startTime) },
                            set: { onIntent(ActivityIntentStartTimeChanged(millis: Int64($0.timeIntervalSince1970 * 1000))) }
                        ),
                        in: ...Date()
                    )
                    .disabled(locked)
                } footer: {
                    if locked { Text(LocalizedStringKey("activity_measured_hint")) }
                }

                if form.hasRoute {
                    Section {
                        Toggle(
                            LocalizedStringKey("activity_show_map_label"),
                            isOn: Binding(get: { form.showMap }, set: { onIntent(ActivityIntentShowMapChanged(show: $0)) })
                        )
                    } footer: {
                        Text(LocalizedStringKey("activity_show_map_hint"))
                    }
                }

                if let errorMessage {
                    Section {
                        Label(errorMessage, systemImage: "exclamationmark.triangle.fill")
                            .foregroundStyle(.red)
                            .font(.subheadline)
                    }
                }
            }
            .navigationTitle(LocalizedStringKey(form.isEditing ? "activity_edit_title" : "activity_new_title"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(LocalizedStringKey("cancel_button"), action: onCancel).disabled(isSaving)
                }
                ToolbarItem(placement: .confirmationAction) {
                    if isSaving {
                        ProgressView()
                    } else {
                        Button(LocalizedStringKey("save_activity_button"), action: onSave)
                            .fontWeight(.semibold)
                    }
                }
            }
        }
        .interactiveDismissDisabled(isSaving)
        // Reset the texts when another activity is shown.
        .task(id: form.activityId ?? "new") {
            distanceText = Self.format(form.distanceKm)
            durationText = Self.format(form.durationMinutes)
            elevationText = Self.format(form.elevationMeters)
        }
    }

    private func binding(_ value: String, _ intent: @escaping (String) -> ActivityIntent) -> Binding<String> {
        Binding(get: { value }, set: { onIntent(intent($0)) })
    }

    private func numberRow(_ label: LocalizedStringKey, text: Binding<String>, intent: @escaping (Double) -> ActivityIntent) -> some View {
        LabeledContent(label) {
            TextField(label, text: Binding(
                get: { text.wrappedValue },
                set: { newValue in
                    text.wrappedValue = newValue
                    if let number = Double(newValue.replacingOccurrences(of: ",", with: ".")) { onIntent(intent(number)) }
                }
            ))
            .keyboardType(.decimalPad)
            .multilineTextAlignment(.trailing)
            .monospacedDigit()
        }
        .disabled(locked)
        .foregroundStyle(locked ? .secondary : .primary)
    }

    /// "5" instead of "5.0", up to two decimals otherwise.
    private static func format(_ value: Double) -> String {
        if value.rounded() == value { return String(Int64(value)) }
        var text = String(format: "%.2f", value)
        while text.hasSuffix("0") { text.removeLast() }
        return text.hasSuffix(".") ? String(text.dropLast()) : text
    }
}

#Preview("Light Mode") {
    ActivityEditView(form: .sample(), isSaving: false).preferredColorScheme(.light)
}

#Preview("Dark Mode, recorded") {
    ActivityEditView(form: .sample(editing: true, measured: true), isSaving: false, errorMessage: "No connection.")
        .preferredColorScheme(.dark)
}

#Preview("Large Text") {
    ActivityEditView(form: .sample(editing: true), isSaving: true).environment(\.dynamicTypeSize, .accessibility2)
}
