@preconcurrency import shared
import PhotosUI
import SwiftUI

/// Reusable image flow for any picture with a fixed aspect ratio (avatar 1:1, cover 4:3, ...):
/// source selection (library / camera / remove) -> crop -> `onImageReady` with a JPEG already scaled to `maxEdge`.
/// Set `isPresented` to true to start it.
struct ImagePickerFlowModifier: ViewModifier {
    @Binding var isPresented: Bool
    let title: LocalizedStringKey
    let aspectRatio: Float
    let maxEdge: Int
    let jpegQuality: Int
    let canRemove: Bool
    let onImageReady: (Data) -> Void
    let onRemove: () -> Void

    private struct CropSource: Identifiable {
        let id = UUID()
        let data: Data
    }

    @State private var showLibrary = false
    @State private var showCamera = false
    @State private var showCameraDisclosure = false
    @State private var showCameraDenied = false
    @State private var libraryItem: PhotosPickerItem?
    @State private var cropSource: CropSource?
    @State private var loadFailed = false

    func body(content: Content) -> some View {
        content
            .confirmationDialog(title, isPresented: $isPresented, titleVisibility: .visible) {
                Button(LocalizedStringKey("avatar_choose_library")) { showLibrary = true }
                if CameraPickerView.isAvailable {
                    Button(LocalizedStringKey("avatar_take_photo"), action: openCameraIfPermitted)
                }
                if canRemove {
                    Button(LocalizedStringKey("avatar_remove"), role: .destructive, action: onRemove)
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
                    aspectRatio: aspectRatio,
                    outputMaxEdge: maxEdge,
                    jpegQuality: jpegQuality,
                    onCropped: { jpeg in
                        cropSource = nil
                        onImageReady(jpeg)
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

    /// Disclosure first (only when iOS has not asked yet), then the system prompt; denied access leads to Settings.
    private func openCameraIfPermitted() {
        switch CameraPermission.status {
        case .authorized: showCamera = true
        case .notDetermined: showCameraDisclosure = true
        default: showCameraDenied = true
        }
    }
}

extension View {
    func imagePickerFlow(
        isPresented: Binding<Bool>,
        title: LocalizedStringKey,
        aspectRatio: Float,
        maxEdge: Int,
        jpegQuality: Int,
        canRemove: Bool,
        onImageReady: @escaping (Data) -> Void,
        onRemove: @escaping () -> Void
    ) -> some View {
        modifier(ImagePickerFlowModifier(
            isPresented: isPresented, title: title, aspectRatio: aspectRatio, maxEdge: maxEdge,
            jpegQuality: jpegQuality, canRemove: canRemove, onImageReady: onImageReady, onRemove: onRemove
        ))
    }
}
