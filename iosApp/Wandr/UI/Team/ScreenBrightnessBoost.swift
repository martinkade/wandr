import SwiftUI
import UIKit

/// Raises the screen brightness while the view is shown (so another phone can scan a QR code from it) and restores
/// the previous brightness when it disappears.
struct ScreenBrightnessBoost: ViewModifier {
    var amount: CGFloat = 0.3

    @State private var previous: CGFloat?

    func body(content: Content) -> some View {
        content
            .onAppear {
                guard let screen = Self.screen, previous == nil else { return }
                previous = screen.brightness
                screen.brightness = min(1, screen.brightness + amount)
            }
            .onDisappear {
                guard let screen = Self.screen, let previous else { return }
                screen.brightness = previous
                self.previous = nil
            }
    }

    private static var screen: UIScreen? {
        (UIApplication.shared.connectedScenes.first { $0.activationState == .foregroundActive } as? UIWindowScene)?.screen
    }
}

extension View {
    func screenBrightnessBoost(_ amount: CGFloat = 0.3) -> some View {
        modifier(ScreenBrightnessBoost(amount: amount))
    }
}

#Preview("Light Mode") {
    Text("Brighter while shown").screenBrightnessBoost().preferredColorScheme(.light)
}

#Preview("Dark Mode") {
    Text("Brighter while shown").screenBrightnessBoost().preferredColorScheme(.dark)
}

#Preview("Large text") {
    Text("Brighter while shown").screenBrightnessBoost().dynamicTypeSize(.accessibility3)
}
