import SwiftUI

// WANDR's Liquid Glass design system.
//
// Glass belongs to the layer ABOVE the content: floating controls (buttons over a map or a cover, action bars, chips,
// the record button). The system adds it to tab bars, toolbars, sheets and menus by itself on iOS 26+. Content
// (cards, lists) stays on solid grouped surfaces (`wandrCard`), so text keeps its contrast. Before iOS 26 the same
// helpers fall back to materials.

extension View {
    /// A Liquid Glass surface in a rounded rectangle (iOS 26+), a thin material before.
    @ViewBuilder
    func wandrGlass(cornerRadius: CGFloat = 20, tint: Color? = nil, interactive: Bool = false) -> some View {
        if #available(iOS 26.0, *) {
            self.glassEffect(Self.glass(tint: tint, interactive: interactive), in: .rect(cornerRadius: cornerRadius))
        } else {
            self.background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: cornerRadius, style: .continuous))
        }
    }

    /// A Liquid Glass capsule (chips, small floating labels).
    @ViewBuilder
    func wandrGlassCapsule(tint: Color? = nil, interactive: Bool = false) -> some View {
        if #available(iOS 26.0, *) {
            self.glassEffect(Self.glass(tint: tint, interactive: interactive), in: .capsule)
        } else {
            self.background(.ultraThinMaterial, in: Capsule())
        }
    }

    /// A Liquid Glass circle (round icon buttons over content).
    @ViewBuilder
    func wandrGlassCircle(interactive: Bool = true) -> some View {
        if #available(iOS 26.0, *) {
            self.glassEffect(Self.glass(tint: nil, interactive: interactive), in: .circle)
        } else {
            self.background(.ultraThinMaterial, in: Circle())
        }
    }

    /// A content card: a solid surface of the grouped background, continuous corners.
    func wandrCard(cornerRadius: CGFloat = 20) -> some View {
        background(Color(.secondarySystemGroupedBackground), in: RoundedRectangle(cornerRadius: cornerRadius, style: .continuous))
    }

    /// One card of a list of connected cards (like Android's grouped lists): big corners at the ends of the group only.
    func wandrSectionItem(isFirst: Bool, isLast: Bool) -> some View {
        background(
            Color(.secondarySystemGroupedBackground),
            in: UnevenRoundedRectangle(
                topLeadingRadius: isFirst ? 20 : 6, bottomLeadingRadius: isLast ? 20 : 6,
                bottomTrailingRadius: isLast ? 20 : 6, topTrailingRadius: isFirst ? 20 : 6, style: .continuous
            )
        )
    }

    /// The primary action style: prominent glass (iOS 26+), a filled brand button before.
    @ViewBuilder
    func wandrProminentButton() -> some View {
        if #available(iOS 26.0, *) {
            self.buttonStyle(.glassProminent).tint(Color.wandrPrimary)
        } else {
            self.buttonStyle(.borderedProminent).tint(Color.wandrPrimary)
        }
    }

    /// A secondary action style: clear glass (iOS 26+), a bordered button before.
    @ViewBuilder
    func wandrGlassButton() -> some View {
        if #available(iOS 26.0, *) {
            self.buttonStyle(.glass)
        } else {
            self.buttonStyle(.bordered)
        }
    }

    @available(iOS 26.0, *)
    private static func glass(tint: Color?, interactive: Bool) -> Glass {
        var glass = Glass.regular
        if let tint { glass = glass.tint(tint) }
        if interactive { glass = glass.interactive() }
        return glass
    }
}

/// Groups glass shapes so they blend and morph into each other (iOS 26+); a plain container before.
struct WandrGlassContainer<Content: View>: View {
    var spacing: CGFloat = 12
    @ViewBuilder var content: () -> Content

    var body: some View {
        if #available(iOS 26.0, *) {
            GlassEffectContainer(spacing: spacing) { content() }
        } else {
            content()
        }
    }
}

#Preview("Glass Light") { GlassPreview().preferredColorScheme(.light) }
#Preview("Glass Dark") { GlassPreview().preferredColorScheme(.dark) }

private struct GlassPreview: View {
    var body: some View {
        ZStack {
            LinearGradient(colors: [.wandrPrimary, .blue], startPoint: .topLeading, endPoint: .bottomTrailing).ignoresSafeArea()
            VStack(spacing: 16) {
                Text("Card").padding().frame(maxWidth: .infinity).wandrCard()
                Text("Glass").padding().frame(maxWidth: .infinity).wandrGlass()
                HStack {
                    Image(systemName: "location.fill").padding(12).wandrGlassCircle()
                    Text("Chip").padding(.horizontal, 12).padding(.vertical, 6).wandrGlassCapsule()
                }
                Button("Join") {}.wandrProminentButton()
                Button("Invite") {}.wandrGlassButton()
            }
            .padding()
        }
    }
}
