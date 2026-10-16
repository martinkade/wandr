import SwiftUI

/// The floating glass button of the feed: a menu to record an activity or to log one manually. While a recording runs it
/// turns red and opens the running recording directly.
struct ActivityFabMenu: View {
    var isRecording = false
    var onRecord: () -> Void = {}
    var onCreate: () -> Void = {}

    var body: some View {
        if isRecording {
            Button(action: onRecord) {
                fabLabel(systemImage: "record.circle.fill", tint: .red, foreground: .white)
                    .symbolEffect(.pulse)
            }
            .buttonStyle(.plain)
            .accessibilityLabel(Text(LocalizedStringKey("record_activity")))
        } else {
            Menu {
                Button(action: onRecord) { Label(LocalizedStringKey("record_activity"), systemImage: "record.circle") }
                Button(action: onCreate) { Label(LocalizedStringKey("activity_fab_create"), systemImage: "square.and.pencil") }
            } label: {
                fabLabel(systemImage: "plus", tint: .wandrPrimary, foreground: .wandrOnPrimary)
            }
            .menuStyle(.button)
            .buttonStyle(.plain)
            .accessibilityLabel(Text(LocalizedStringKey("activity_fab_open")))
        }
    }

    @ViewBuilder
    private func fabLabel(systemImage: String, tint: Color, foreground: Color) -> some View {
        let icon = Image(systemName: systemImage)
            .font(.title2.weight(.semibold))
            .foregroundStyle(foreground)
            .frame(width: 60, height: 60)
        if #available(iOS 26.0, *) {
            icon.glassEffect(.regular.tint(tint).interactive(), in: .circle)
        } else {
            icon.background(tint, in: Circle()).shadow(color: .black.opacity(0.2), radius: 8, y: 4)
        }
    }
}

#Preview("Light Mode") {
    ZStack(alignment: .bottomTrailing) {
        Color(.systemGroupedBackground).ignoresSafeArea()
        ActivityFabMenu().padding()
    }
    .preferredColorScheme(.light)
}

#Preview("Dark Mode, recording") {
    ZStack(alignment: .bottomTrailing) {
        Color(.systemGroupedBackground).ignoresSafeArea()
        ActivityFabMenu(isRecording: true).padding()
    }
    .preferredColorScheme(.dark)
}

#Preview("Large Text") {
    ActivityFabMenu().padding().environment(\.dynamicTypeSize, .accessibility3)
}
