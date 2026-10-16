import SwiftUI
import VisionKit

/// Sheet to join a team by typing its invite code or scanning its QR code. Scanning explains the camera use before
/// iOS asks for it; without a scanner (or camera) the code field stays the way in.
struct JoinTeamSheetView: View {
    @Binding var code: String
    var isJoining: Bool = false
    var errorMessage: String? = nil
    var onJoin: () -> Void = {}
    /// The raw QR content; the shared use case unwraps `wandr://invite/{code}`.
    var onScanned: (String) -> Void = { _ in }

    @State private var showScanner = false
    @State private var showDisclosure = false
    @State private var showDenied = false
    @State private var scanUnavailable = false
    @FocusState private var codeFocused: Bool

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                VStack(alignment: .leading, spacing: 4) {
                    Text(LocalizedStringKey("join_team_title"))
                        .font(.title2.weight(.bold))
                    Text(LocalizedStringKey("join_team_hint"))
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }

                VStack(alignment: .leading, spacing: 6) {
                    TextField(LocalizedStringKey("join_team_code_label"), text: $code)
                        .font(.system(.title3, design: .monospaced))
                        .textInputAutocapitalization(.characters)
                        .autocorrectionDisabled()
                        .submitLabel(.join)
                        .onSubmit(join)
                        .focused($codeFocused)
                        .disabled(isJoining)
                        .padding(.horizontal, 16)
                        .frame(minHeight: 52)
                        .wandrCard(cornerRadius: 14)
                        .overlay {
                            if errorMessage != nil {
                                RoundedRectangle(cornerRadius: 14, style: .continuous).stroke(Color.red, lineWidth: 1)
                            }
                        }
                    if let errorMessage {
                        Text(errorMessage)
                            .font(.footnote)
                            .foregroundStyle(.red)
                    }
                }

                Button(action: join) {
                    Group {
                        if isJoining {
                            ProgressView()
                        } else {
                            Text(LocalizedStringKey("join_team_button"))
                        }
                    }
                    .font(.headline)
                    .frame(maxWidth: .infinity, minHeight: 36)
                }
                .wandrProminentButton()
                .controlSize(.large)
                .disabled(isCodeBlank || isJoining)

                HStack(spacing: 12) {
                    VStack { Divider() }
                    Text(LocalizedStringKey("join_team_or"))
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                    VStack { Divider() }
                }

                Button(action: startScan) {
                    Label(LocalizedStringKey("join_team_scan"), systemImage: "qrcode.viewfinder")
                        .font(.headline)
                        .frame(maxWidth: .infinity, minHeight: 36)
                }
                .wandrGlassButton()
                .controlSize(.large)
                .disabled(isJoining)

                if scanUnavailable {
                    Text(LocalizedStringKey("join_team_scan_unavailable"))
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                }
            }
            .padding(24)
        }
        .scrollBounceBehavior(.basedOnSize)
        .permissionDisclosure(
            isPresented: $showDisclosure,
            title: "permission_camera_title",
            message: "permission_camera_qr_message"
        ) {
            Task {
                if await CameraPermission.request() { showScanner = true } else { showDenied = true }
            }
        }
        .permissionDeniedAlert(isPresented: $showDenied, message: "permission_camera_qr_denied")
        .fullScreenCover(isPresented: $showScanner) {
            InviteQrScannerView(
                onScanned: { raw in
                    showScanner = false
                    onScanned(raw)
                },
                onClose: { showScanner = false }
            )
        }
    }

    private var isCodeBlank: Bool { code.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty }

    private func join() {
        guard !isCodeBlank, !isJoining else { return }
        onJoin()
    }

    /// Disclosure first (only when iOS has not asked yet), then the system prompt; denied access leads to Settings.
    private func startScan() {
        codeFocused = false
        guard DataScannerViewController.isSupported else {
            scanUnavailable = true
            return
        }
        switch CameraPermission.status {
        case .authorized:
            if DataScannerViewController.isAvailable { showScanner = true } else { scanUnavailable = true }
        case .notDetermined:
            showDisclosure = true
        default:
            showDenied = true
        }
    }
}

#Preview("Light Mode") {
    Color.clear.sheet(isPresented: .constant(true)) {
        JoinTeamSheetView(code: .constant("")).presentationDetents([.medium])
    }
    .preferredColorScheme(.light)
}

#Preview("Dark Mode, error") {
    Color.clear.sheet(isPresented: .constant(true)) {
        JoinTeamSheetView(code: .constant("X7K9"), errorMessage: "This is not a valid invite code.")
            .presentationDetents([.medium])
    }
    .preferredColorScheme(.dark)
}

#Preview("Large text, joining") {
    JoinTeamSheetView(code: .constant("X7K9P2W1"), isJoining: true)
        .dynamicTypeSize(.accessibility2)
}
