import SwiftUI
@preconcurrency import shared

/// Create / edit form shown in a sheet. Targets are entered in friendly units (km, m, hours) and converted to base
/// units by the shared `ChallengeUnits`. Every change goes to the view model as an intent; title, description and
/// target keep a local text so typing is never interrupted by the round trip.
struct ChallengeEditView: View {
    let form: ChallengeForm
    var isSaving: Bool = false
    var onIntent: (any ChallengeIntent) -> Void = { _ in }
    var onSave: () -> Void = {}
    var onCancel: () -> Void = {}
    var snackbar: Binding<SnackbarMessage?> = .constant(nil)

    @State private var title = ""
    @State private var details = ""
    @State private var targetText = ""
    @FocusState private var focusedField: Field?

    private enum Field { case title, description, target }

    /// Resets the local texts when another challenge or another type (unit) is shown.
    private var resetKey: String { "\(form.challengeId ?? "new")|\(form.type.name)" }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    TextField(LocalizedStringKey("challenge_title_label"), text: $title)
                        .focused($focusedField, equals: .title)
                        .submitLabel(.next)
                        .onSubmit { focusedField = .description }
                    TextField(LocalizedStringKey("challenge_description_label"), text: $details, axis: .vertical)
                        .lineLimit(3...6)
                        .focused($focusedField, equals: .description)
                }

                Section(LocalizedStringKey("challenge_type_label")) {
                    Picker(LocalizedStringKey("challenge_type_label"), selection: typeBinding) {
                        ForEach(ChallengeType.entries, id: \.self) { type in
                            Label(ChallengeText.typeLabel(type), systemImage: ChallengeBadgeView.symbol(type)).tag(type)
                        }
                    }
                    .pickerStyle(.segmented)
                    .labelsHidden()
                    HStack {
                        Text(LocalizedStringKey("challenge_target_label"))
                        TextField("0", text: $targetText)
                            .keyboardType(.decimalPad)
                            .multilineTextAlignment(.trailing)
                            .focused($focusedField, equals: .target)
                        Text(ChallengeText.unit(form.type)).foregroundStyle(.secondary)
                    }
                }

                // Challenges can be planned for the future: start and end with date and time.
                Section(LocalizedStringKey("challenge_period_label")) {
                    DatePicker(LocalizedStringKey("challenge_start_label"), selection: dateBinding(\.startDate) { ChallengeIntentStartDateChanged(millis: $0) })
                    DatePicker(
                        LocalizedStringKey("challenge_end_label"),
                        selection: dateBinding(\.endDate) { ChallengeIntentEndDateChanged(millis: $0) },
                        in: ChallengeText.date(form.startDate)...
                    )
                }

                Section {
                    ForEach(ActivityTypes.shared.all, id: \.self) { type in
                        Button { onIntent(ChallengeIntentActivityTypeToggled(type: type)) } label: {
                            HStack {
                                Text(ChallengeText.activityType(type)).foregroundStyle(Color.primary)
                                Spacer()
                                if form.activityTypes.contains(type) {
                                    Image(systemName: "checkmark").fontWeight(.semibold).foregroundStyle(Color.wandrAccentText)
                                }
                            }
                            .contentShape(Rectangle())
                        }
                        .accessibilityAddTraits(form.activityTypes.contains(type) ? .isSelected : [])
                    }
                } header: {
                    Text(LocalizedStringKey("challenge_activity_types_label"))
                } footer: {
                    Text(LocalizedStringKey("challenge_activity_types_hint"))
                }

                Section {
                    Picker(LocalizedStringKey("challenge_scope_label"), selection: scopeBinding) {
                        Text(LocalizedStringKey("challenge_scope_individual")).tag(ChallengeScope.individual)
                        Text(LocalizedStringKey("challenge_scope_team")).tag(ChallengeScope.team)
                    }
                    .pickerStyle(.inline)
                    .labelsHidden()
                    if form.scope == .team {
                        Toggle(LocalizedStringKey("require_all_members_label"), isOn: Binding(
                            get: { form.requireAllMembersCompletion },
                            set: { onIntent(ChallengeIntentRequireAllMembersCompletionChanged(requireAll: $0)) }
                        ))
                    }
                } header: {
                    Text(LocalizedStringKey("challenge_scope_label"))
                } footer: {
                    if form.scope == .team { Text(LocalizedStringKey("challenge_scope_team_hint")) }
                }

                Section {
                    Toggle(LocalizedStringKey("challenge_active_label"), isOn: Binding(
                        get: { form.isActive },
                        set: { onIntent(ChallengeIntentActiveChanged(isActive: $0)) }
                    ))
                } footer: {
                    Text(LocalizedStringKey("challenge_active_hint"))
                }
            }
            .scrollDismissesKeyboard(.interactively)
            .disabled(isSaving)
            .navigationTitle(LocalizedStringKey(form.isEditing ? "challenge_edit_title" : "create_challenge_title"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(LocalizedStringKey("cancel_button"), action: onCancel).disabled(isSaving)
                }
                ToolbarItem(placement: .confirmationAction) {
                    if isSaving {
                        ProgressView()
                    } else {
                        Button(LocalizedStringKey("save_challenge_button"), action: onSave)
                            .fontWeight(.semibold)
                            .disabled(title.trimmingCharacters(in: .whitespaces).isEmpty)
                    }
                }
                ToolbarItemGroup(placement: .keyboard) {
                    Spacer()
                    Button(LocalizedStringKey("crop_done")) { focusedField = nil }
                }
            }
            // While the sheet is open, messages (e.g. validation errors) show here.
            .snackbar(snackbar)
        }
        .onAppear(perform: resetTexts)
        .onChange(of: resetKey) { _, _ in resetTexts() }
        .onChange(of: title) { _, value in if value != form.title { onIntent(ChallengeIntentTitleChanged(title: value)) } }
        .onChange(of: details) { _, value in if value != form.description_ { onIntent(ChallengeIntentDescriptionChanged(description: value)) } }
        .onChange(of: targetText) { _, text in
            if let number = Self.parse(text) {
                onIntent(ChallengeIntentTargetValueChanged(value: ChallengeUnits.shared.toBase(type: form.type, displayValue: number)))
            }
        }
    }

    private var typeBinding: Binding<ChallengeType> {
        Binding(get: { form.type }, set: { onIntent(ChallengeIntentTypeChanged(type: $0)) })
    }

    private var scopeBinding: Binding<ChallengeScope> {
        Binding(get: { form.scope }, set: { onIntent(ChallengeIntentScopeChanged(scope: $0)) })
    }

    private func dateBinding(_ keyPath: KeyPath<ChallengeForm, Int64>, _ intent: @escaping (Int64) -> any ChallengeIntent) -> Binding<Date> {
        Binding(
            get: { ChallengeText.date(form[keyPath: keyPath]) },
            set: { onIntent(intent(ChallengeText.millis($0))) }
        )
    }

    private func resetTexts() {
        title = form.title
        details = form.description_
        targetText = ChallengeUnits.shared.toDisplay(type: form.type, baseValue: form.targetValue)
            .formatted(.number.precision(.fractionLength(0...2)).grouping(.never))
    }

    /// Accepts the locale's decimal separator as well as a dot, so "12,5" and "12.5" both work.
    private static func parse(_ text: String) -> Double? {
        let normalized = text.trimmingCharacters(in: .whitespaces).replacingOccurrences(of: ",", with: ".")
        return Double(normalized)
    }
}

private func previewForm(editing: Bool = false, scope: ChallengeScope = .team) -> ChallengeForm {
    ChallengeForm(
        challengeId: editing ? "c1" : nil, title: editing ? "30-Day 100 km Hike" : "", description: "", type: .distance,
        targetValue: 100_000, startDate: ChallengePreviewData.start, endDate: ChallengePreviewData.end, scope: scope,
        requireAllMembersCompletion: true, activityTypes: ["hiking"], isActive: false
    )
}

#Preview("Light Mode") {
    ChallengeEditView(form: previewForm()).preferredColorScheme(.light)
}

#Preview("Dark Mode, editing") {
    ChallengeEditView(form: previewForm(editing: true, scope: .individual), isSaving: true).preferredColorScheme(.dark)
}

#Preview("Large Text, iPhone SE", traits: .fixedLayout(width: 320, height: 568)) {
    ChallengeEditView(form: previewForm(editing: true)).environment(\.dynamicTypeSize, .accessibility1)
}
