import CoreImage
import CoreImage.CIFilterBuiltins
import SwiftUI

/// A QR code of `content` (a team's invite code; "Join a team" reads it back as the code). Always black on white with
/// a quiet zone, also in dark mode: scanners need that contrast. `size` is the whole square, quiet zone included.
struct InviteQrCodeView: View {
    let content: String
    var size: CGFloat = 200

    private let quietZone: CGFloat = 12

    var body: some View {
        ZStack {
            Color.white
            if let image = Self.makeImage(content) {
                Image(decorative: image, scale: 1)
                    .interpolation(.none) // crisp modules instead of a blurry upscale
                    .resizable()
                    .scaledToFit()
                    .padding(quietZone)
            }
        }
        .frame(width: size, height: size)
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
        .accessibilityElement()
        .accessibilityLabel(Text(content))
    }

    /// One pixel per module; the view scales it up without interpolation.
    private static func makeImage(_ content: String) -> CGImage? {
        let filter = CIFilter.qrCodeGenerator()
        filter.message = Data(content.utf8)
        filter.correctionLevel = "M"
        guard let output = filter.outputImage else { return nil }
        return CIContext().createCGImage(output, from: output.extent)
    }
}

#Preview("Light Mode") {
    InviteQrCodeView(content: "X7K9P2W1").padding().preferredColorScheme(.light)
}

#Preview("Dark Mode") {
    InviteQrCodeView(content: "X7K9P2W1").padding().background(Color.black).preferredColorScheme(.dark)
}

#Preview("Small", traits: .fixedLayout(width: 200, height: 200)) {
    InviteQrCodeView(content: "Q3M8D5LA", size: 120)
}
