import SwiftUI

/// Brand colors (light / dark variants live in Assets.xcassets).
///
/// - `wandrPrimary`: brand gold, for FILLS (buttons, banners). Put `wandrOnPrimary` (black) on top.
/// - `Color.accentColor` / `wandrAccentText`: readable accent for TEXT, icons and links
///   (deep gold in light mode, because gold on white has too little contrast; gold in dark mode).
/// - `wandrSecondary`: secondary accent (blue in light mode as in the legacy app, soft yellow in dark mode).
extension Color {
    static let wandrPrimary = Color("BrandPrimary")
    static let wandrOnPrimary = Color("OnBrandPrimary")
    static let wandrSecondary = Color("BrandSecondary")
    static let wandrAccentText = Color.accentColor
    /// The route line on maps: a strong royal blue, the complement of the brand gold (same value as on Android).
    static let wandrRoute = Color(red: 0x1F / 255, green: 0x5F / 255, blue: 0xD6 / 255)
}

#Preview("Palette Light") { WandrPalettePreview().preferredColorScheme(.light) }
#Preview("Palette Dark") { WandrPalettePreview().preferredColorScheme(.dark) }

private struct WandrPalettePreview: View {
    var body: some View {
        VStack(spacing: 12) {
            Text("Primary").padding().frame(maxWidth: .infinity).background(Color.wandrPrimary).foregroundColor(.wandrOnPrimary)
            Text("Accent text").foregroundColor(.wandrAccentText)
            Text("Secondary").foregroundColor(.wandrSecondary)
        }
        .padding()
    }
}
