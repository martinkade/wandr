import SwiftUI
import VisionKit

/// Full-screen camera that reads a team's invite QR code with VisionKit's DataScanner. `onScanned` gets the raw content
/// once (the bare code or `wandr://invite/{code}`). Call it only after camera access was granted; without a supported
/// scanner it shows a hint instead.
struct InviteQrScannerView: View {
    var onScanned: (String) -> Void = { _ in }
    var onClose: () -> Void = {}

    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()
            if DataScannerViewController.isSupported && DataScannerViewController.isAvailable {
                QrDataScanner(onScanned: onScanned).ignoresSafeArea()
            } else {
                Text(LocalizedStringKey("join_team_scan_unavailable"))
                    .font(.body)
                    .foregroundStyle(.white)
                    .multilineTextAlignment(.center)
                    .padding(32)
            }
        }
        .overlay(alignment: .bottom) {
            Text(LocalizedStringKey("join_team_scan_hint"))
                .font(.subheadline.weight(.medium))
                .multilineTextAlignment(.center)
                .padding(.horizontal, 16)
                .padding(.vertical, 10)
                .wandrGlassCapsule()
                .padding(24)
        }
        .overlay(alignment: .topLeading) {
            Button(action: onClose) {
                Image(systemName: "xmark")
                    .font(.headline)
                    .frame(width: 44, height: 44)
            }
            .buttonStyle(.plain)
            .wandrGlassCircle()
            .padding(16)
            .accessibilityLabel(Text(LocalizedStringKey("close_button")))
        }
    }
}

/// Wraps `DataScannerViewController` for QR codes; reports the first payload only.
private struct QrDataScanner: UIViewControllerRepresentable {
    let onScanned: (String) -> Void

    func makeCoordinator() -> Coordinator { Coordinator(onScanned: onScanned) }

    func makeUIViewController(context: Context) -> DataScannerViewController {
        let scanner = DataScannerViewController(
            recognizedDataTypes: [.barcode(symbologies: [.qr])],
            qualityLevel: .balanced,
            recognizesMultipleItems: false,
            isHighFrameRateTrackingEnabled: false,
            isHighlightingEnabled: true
        )
        scanner.delegate = context.coordinator
        return scanner
    }

    func updateUIViewController(_ scanner: DataScannerViewController, context: Context) {
        // Scanning can only start once the controller is part of the hierarchy.
        if !scanner.isScanning { try? scanner.startScanning() }
    }

    static func dismantleUIViewController(_ scanner: DataScannerViewController, coordinator: Coordinator) {
        scanner.stopScanning()
    }

    @MainActor
    final class Coordinator: NSObject, DataScannerViewControllerDelegate {
        private let onScanned: (String) -> Void
        private var delivered = false

        init(onScanned: @escaping (String) -> Void) { self.onScanned = onScanned }

        func dataScanner(_ dataScanner: DataScannerViewController, didAdd addedItems: [RecognizedItem], allItems: [RecognizedItem]) {
            deliver(addedItems)
        }

        func dataScanner(_ dataScanner: DataScannerViewController, didTapOn item: RecognizedItem) {
            deliver([item])
        }

        private func deliver(_ items: [RecognizedItem]) {
            guard !delivered else { return }
            for item in items {
                if case .barcode(let barcode) = item, let payload = barcode.payloadStringValue, !payload.isEmpty {
                    delivered = true
                    onScanned(payload)
                    return
                }
            }
        }
    }
}

#Preview("Light Mode") { InviteQrScannerView().preferredColorScheme(.light) }
#Preview("Dark Mode") { InviteQrScannerView().preferredColorScheme(.dark) }
#Preview("Large text") { InviteQrScannerView().dynamicTypeSize(.accessibility2) }
