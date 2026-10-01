@preconcurrency import shared
import PhotosUI
import SwiftUI

/// Avatar with its whole edit flow: source selection (library / camera / remove) -> 1:1 crop -> `onAvatarReady`
/// with a JPEG already scaled to `AvatarImageSpec.MAX_EDGE_PX`.
struct AvatarEditorView: View {
    let displayName: String
    var avatarUrl: String? = nil
    var isBusy: Bool = false
    var onAvatarReady: (Data) -> Void = { _ in }
    var onRemoveAvatar: () -> Void = {}

    private struct CropSource: Identifiable {
        let id = UUID()
        let data: Data
    }

    @State private var showSources = false
    @State private var showLibrary = false
    @State private var showCamera = false
    @State private var showCameraDisclosure = false
    @State private var showCameraDenied = false
    @State private var libraryItem: PhotosPickerItem?
    @State private var cropSource: CropSource?
    @State private var loadFailed = false

    var body: some View {
        AvatarPickerView(displayName: displayName, avatarUrl: avatarUrl, isBusy: isBusy) { showSources = true }
            .confirmationDialog(LocalizedStringKey("avatar_change_title"), isPresented: $showSources, titleVisibility: .visible) {
                Button(LocalizedStringKey("avatar_choose_library")) { showLibrary = true }
                if CameraPickerView.isAvailable {
                    Button(LocalizedStringKey("avatar_take_photo"), action: openCameraIfPermitted)
                }
                if avatarUrl != nil {
                    Button(LocalizedStringKey("avatar_remove"), role: .destructive, action: onRemoveAvatar)
                }
                Button(LocalizedStringKey("cancel_button"), role: .cancel) {}
            }
            .permissionDisclosure(
                isPresented: $showCameraDisclosure,
                title: "permission_camera_title",
                message: "permission_camera_message"
            ) {
                Task {
                    if await CameraPermission.request() { showCamera = true } else { showCameraDenied = true }
                }
            }
            .permissionDeniedAlert(isPresented: $showCameraDenied, message: "permission_camera_denied")
            .photosPicker(isPresented: $showLibrary, selection: $libraryItem, matching: .images)
            .onChange(of: libraryItem) { _, item in
                guard let item else { return }
                libraryItem = nil
                Task {
                    if let data = try? await item.loadTransferable(type: Data.self), !data.isEmpty {
                        cropSource = CropSource(data: data)
                    } else { loadFailed = true }
                }
            }
            .fullScreenCover(isPresented: $showCamera) {
                CameraPickerView(
                    onCapture: { image in
                        showCamera = false
                        if let data = image.jpegData(compressionQuality: 0.95) { cropSource = CropSource(data: data) }
                        else { loadFailed = true }
                    },
                    onCancel: { showCamera = false }
                )
                .ignoresSafeArea()
            }
            .fullScreenCover(item: $cropSource) { source in
                ImageCropperView(
                    imageData: source.data,
                    aspectRatio: AvatarImageSpec.shared.ASPECT_RATIO,
                    outputMaxEdge: Int(AvatarImageSpec.shared.MAX_EDGE_PX),
                    jpegQuality: Int(AvatarImageSpec.shared.JPEG_QUALITY),
                    onCropped: { jpeg in
                        cropSource = nil
                        onAvatarReady(jpeg)
                    },
                    onCancel: { cropSource = nil },
                    onFailure: {
                        cropSource = nil
                        loadFailed = true
                    }
                )
            }
            .alert(LocalizedStringKey("image_load_failed"), isPresented: $loadFailed) {
                Button("OK", role: .cancel) {}
            }
    }
}

private extension AvatarEditorView {
    /// Disclosure first (only when iOS has not asked yet), then the system prompt; denied access leads to Settings.
    func openCameraIfPermitted() {
        switch CameraPermission.status {
        case .authorized: showCamera = true
        case .notDetermined: showCameraDisclosure = true
        default: showCameraDenied = true
        }
    }
}

#Preview("With initials") { AvatarEditorView(displayName: "Martin Kade") }
#Preview("Dark, busy") { AvatarEditorView(displayName: "Martin Kade", isBusy: true).preferredColorScheme(.dark) }
