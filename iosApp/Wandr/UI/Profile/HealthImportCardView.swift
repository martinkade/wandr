import SwiftUI

/// Card that connects Apple Health (read-only). Workouts sync automatically when the app becomes active; once connected,
/// the settings symbol opens the settings where the access can be changed.
struct HealthImportCardView: View {
    var onSettings: () -> Void = {}

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Image("AppleHealthLogo")
                .resizable()
                .frame(width: 56, height: 56)
                .clipShape(RoundedRectangle(cornerRadius: 16))
                .accessibilityLabel(Text(LocalizedStringKey("health_card_title_apple")))
            Text(LocalizedStringKey("health_card_description"))
                .font(.subheadline)
                .foregroundStyle(.secondary)
            Spacer(minLength: 0)
            Button(action: onSettings) {
                Image(systemName: "gearshape")
                    .font(.title3)
                    .frame(width: 44, height: 44)
            }
            .accessibilityLabel(Text(LocalizedStringKey("health_settings_button")))
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
        .padding(16)
        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 16))
    }
}

#Preview("Light Mode") {
    HealthImportCardView().frame(width: 300, height: 240).padding().preferredColorScheme(.light)
}

#Preview("Dark Mode") {
    HealthImportCardView().frame(width: 300, height: 240).padding().preferredColorScheme(.dark)
}

#Preview("Large text") {
    HealthImportCardView().frame(width: 300, height: 240).padding().dynamicTypeSize(.accessibility2)
}
