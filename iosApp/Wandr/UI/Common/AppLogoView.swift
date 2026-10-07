import SwiftUI

/// The launch logo as a rounded square.
struct AppLogoView: View {
    var size: CGFloat = 88

    var body: some View {
        Image("LaunchIcon")
            .resizable()
            .scaledToFill()
            .frame(width: size, height: size)
            .clipShape(RoundedRectangle(cornerRadius: size * 0.24, style: .continuous))
            .accessibilityHidden(true)
    }
}

#Preview("Light Mode") {
    AppLogoView().padding()
}
#Preview("Dark Mode") {
    AppLogoView().padding().preferredColorScheme(.dark)
}
