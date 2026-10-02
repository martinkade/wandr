import SwiftUI
@preconcurrency import shared

/// Conflict resolution wizard: lists the overlapping activities and lets the user merge, trim or discard. The choice
/// is only applied after confirming, because merging replaces the existing activities.
struct ActivityConflictView: View {
    let conflicting: [(title: String, range: String)]
    let canTrim: Bool
    let isSaving: Bool
    let onResolve: (ConflictResolution) -> Void
    /// nil when the conflict cannot be dismissed (the data would be lost).
    var onCancel: (() -> Void)?

    @State private var selected: ConflictResolution?

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    Text(LocalizedStringKey("activity_conflict_message"))
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }
                Section {
                    ForEach(Array(conflicting.enumerated()), id: \.offset) { _, item in
                        VStack(alignment: .leading, spacing: 2) {
                            Text(item.title).font(.subheadline.weight(.semibold))
                            Text(item.range).font(.caption).foregroundStyle(.secondary)
                        }
                    }
                }
                Section {
                    option(.merge, "activity_conflict_merge_title", "activity_conflict_merge_description", enabled: true)
                    option(
                        .trim, "activity_conflict_trim_title",
                        canTrim ? "activity_conflict_trim_description" : "activity_conflict_trim_unavailable",
                        enabled: canTrim
                    )
                    option(.discard, "activity_conflict_discard_title", "activity_conflict_discard_description", enabled: true)
                }
            }
            .navigationTitle(LocalizedStringKey("activity_conflict_title"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                if let onCancel {
                    ToolbarItem(placement: .cancellationAction) {
                        Button(LocalizedStringKey("cancel_button"), action: onCancel).disabled(isSaving)
                    }
                }
                ToolbarItem(placement: .confirmationAction) {
                    if isSaving {
                        ProgressView()
                    } else {
                        Button(LocalizedStringKey("activity_conflict_apply_button")) {
                            if let selected { onResolve(selected) }
                        }
                        .disabled(selected == nil)
                    }
                }
            }
        }
        .interactiveDismissDisabled(onCancel == nil || isSaving)
    }

    private func option(_ value: ConflictResolution, _ title: LocalizedStringKey, _ description: LocalizedStringKey, enabled: Bool) -> some View {
        Button {
            selected = value
        } label: {
            HStack(alignment: .top, spacing: 12) {
                Image(systemName: selected == value ? "largecircle.fill.circle" : "circle")
                    .foregroundStyle(enabled ? Color.accentColor : Color.secondary)
                VStack(alignment: .leading, spacing: 2) {
                    Text(title).font(.body)
                    Text(description).font(.caption).foregroundStyle(.secondary)
                }
                Spacer()
            }
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .disabled(!enabled || isSaving)
    }
}

extension ActivityConflictView {
    /// Builds the wizard from the shared conflict model.
    init(conflict: ActivityConflict, isSaving: Bool, onResolve: @escaping (ConflictResolution) -> Void) {
        let formatter = DateFormatter()
        formatter.dateStyle = .medium
        formatter.timeStyle = .short
        func text(_ millis: Int64) -> String { formatter.string(from: Date(timeIntervalSince1970: Double(millis) / 1000)) }
        self.init(
            conflicting: conflict.conflicting.map { (title: $0.title, range: "\(text($0.startTime)) – \(text($0.endTime))") },
            canTrim: conflict.canTrim,
            isSaving: isSaving,
            onResolve: onResolve,
            onCancel: nil
        )
    }
}

#Preview("Light Mode") {
    ActivityConflictView(
        conflicting: [("Evening run", "Jan 15, 2026, 6:00 PM – 7:00 PM")],
        canTrim: true, isSaving: false, onResolve: { _ in }, onCancel: {}
    )
}

#Preview("Dark, not trimmable") {
    ActivityConflictView(
        conflicting: [("Evening run", "Jan 15, 2026, 6:00 PM – 7:00 PM"), ("Hike", "Jan 15, 2026, 6:30 PM – 9:00 PM")],
        canTrim: false, isSaving: false, onResolve: { _ in }
    )
    .preferredColorScheme(.dark)
}
