import SwiftUI

/// Read-only field: small label above the value (a dash when there is no value).
struct LabeledValueView: View {
    let label: LocalizedStringKey
    let value: String?

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label).font(.caption).foregroundColor(.secondary)
            if let value, !value.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                Text(value).font(.body).foregroundColor(.primary)
            } else {
                Text(LocalizedStringKey("empty_value_placeholder")).font(.body).foregroundColor(.secondary)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.vertical, 6)
    }
}

#Preview("Light Mode") {
    VStack {
        LabeledValueView(label: "display_name_label", value: "Martin Kade")
        LabeledValueView(label: "bio_label", value: nil)
    }
    .padding()
}

#Preview("Dark Mode") {
    VStack {
        LabeledValueView(label: "display_name_label", value: "Martin Kade")
        LabeledValueView(label: "bio_label", value: nil)
    }
    .padding()
    .preferredColorScheme(.dark)
}
