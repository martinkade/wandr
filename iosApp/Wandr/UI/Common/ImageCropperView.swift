@preconcurrency import shared
import SwiftUI

/// Drives the shared `ImageCropper` and keeps the decoded display image.
@MainActor
final class ImageCropperModel: ObservableObject {
    @Published private(set) var cropState: CropState
    @Published private(set) var isProcessing = false

    let displayImage: UIImage
    private let cropper: ImageCropper
    private let originalWidth: CGFloat

    private init(display: UIImage, original: CGSize, aspectRatio: Float) {
        displayImage = display
        originalWidth = original.width
        cropper = ImageCropper(
            imageWidth: Int32(original.width), imageHeight: Int32(original.height),
            aspectRatio: aspectRatio, maxZoom: 8, windowInset: 24
        )
        cropState = cropper.state
    }

    /// Decodes off the main thread, then builds the model. Returns nil when the data is not a decodable image.
    static func load(data: Data, aspectRatio: Float) async -> ImageCropperModel? {
        let decoded = await Task.detached(priority: .userInitiated) { () -> (UIImage, CGSize)? in
            guard let original = ImagePipeline.orientedSize(of: data),
                  let display = ImagePipeline.displayImage(from: data) else { return nil }
            return (display, original)
        }.value
        guard let (display, original) = decoded else { return nil }
        return ImageCropperModel(display: display, original: original, aspectRatio: aspectRatio)
    }

    /// Container points per displayed (downsampled) image pixel.
    var displayScale: CGFloat {
        guard let cg = displayImage.cgImage else { return 1 }
        return CGFloat(cropState.scale) * originalWidth / CGFloat(cg.width)
    }

    func layout(_ size: CGSize) {
        cropState = cropper.layout(containerWidth: Float(size.width), containerHeight: Float(size.height))
    }

    func transform(pan: CGSize, zoom: CGFloat, around point: CGPoint) {
        cropState = cropper.transform(
            panX: Float(pan.width), panY: Float(pan.height), zoom: Float(zoom),
            centroidX: Float(point.x), centroidY: Float(point.y)
        )
    }

    func reset() { cropState = cropper.reset() }

    func render(data: Data, maxEdge: Int, quality: Int) async -> Data? {
        isProcessing = true
        defer { isProcessing = false }
        let rect = cropper.cropRect()
        let output = cropper.outputSize(maxEdge: Int32(maxEdge))
        return await Task.detached(priority: .userInitiated) {
            ImagePipeline.cropAndScale(data: data, rect: rect, output: output, quality: quality)
        }.value
    }
}

/// Reusable image cropper with a fixed aspect ratio (width / height). Pan and pinch to position the photo
/// under the crop window; "Done" returns a JPEG scaled down to at most `outputMaxEdge` pixels.
struct ImageCropperView: View {
    let imageData: Data
    let aspectRatio: Float
    let outputMaxEdge: Int
    let jpegQuality: Int
    var onCropped: (Data) -> Void
    var onCancel: () -> Void
    var onFailure: () -> Void = {}

    @State private var model: ImageCropperModel?
    @State private var lastTranslation: CGSize = .zero
    @State private var lastMagnification: CGFloat = 1

    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()
            if let model {
                CropperCanvas(
                    model: model,
                    lastTranslation: $lastTranslation,
                    lastMagnification: $lastMagnification,
                    onCancel: onCancel,
                    onDone: {
                        Task {
                            if let jpeg = await model.render(data: imageData, maxEdge: outputMaxEdge, quality: jpegQuality) {
                                onCropped(jpeg)
                            } else { onFailure() }
                        }
                    }
                )
            } else {
                ProgressView().tint(.white)
            }
        }
        .task {
            if let loaded = await ImageCropperModel.load(data: imageData, aspectRatio: aspectRatio) {
                model = loaded
            } else { onFailure() }
        }
    }
}

private struct CropperCanvas: View {
    @ObservedObject var model: ImageCropperModel
    @Binding var lastTranslation: CGSize
    @Binding var lastMagnification: CGFloat
    var onCancel: () -> Void
    var onDone: () -> Void

    var body: some View {
        GeometryReader { geo in
            let state = model.cropState
            let scale = model.displayScale
            let imageSize = model.displayImage.size
            ZStack {
                if state.isLaidOut {
                    Image(uiImage: model.displayImage)
                        .resizable()
                        .frame(width: imageSize.width * scale, height: imageSize.height * scale)
                        .position(x: CGFloat(state.imageCenterX), y: CGFloat(state.imageCenterY))
                    CropOverlayView(state: state)
                }
            }
            .frame(width: geo.size.width, height: geo.size.height)
            .clipped()
            .contentShape(Rectangle())
            .gesture(
                DragGesture()
                    .onChanged { value in
                        let delta = CGSize(width: value.translation.width - lastTranslation.width,
                                           height: value.translation.height - lastTranslation.height)
                        lastTranslation = value.translation
                        model.transform(pan: delta, zoom: 1, around: value.location)
                    }
                    .onEnded { _ in lastTranslation = .zero }
            )
            .simultaneousGesture(
                MagnifyGesture()
                    .onChanged { value in
                        let factor = value.magnification / lastMagnification
                        lastMagnification = value.magnification
                        model.transform(pan: .zero, zoom: factor, around: value.startLocation)
                    }
                    .onEnded { _ in lastMagnification = 1 }
            )
            .onAppear { model.layout(geo.size) }
            .onChange(of: geo.size) { _, size in model.layout(size) }
            .overlay(alignment: .top) { topBar }
            .overlay(alignment: .bottom) { bottomBar }
        }
        .ignoresSafeArea()
    }

    private var topBar: some View {
        HStack {
            Button(LocalizedStringKey("cancel_button"), action: onCancel).disabled(model.isProcessing)
            Spacer()
            Text(LocalizedStringKey("crop_title")).font(.headline)
            Spacer()
            if model.isProcessing {
                ProgressView().tint(.white)
            } else {
                Button(LocalizedStringKey("crop_done"), action: onDone).bold()
            }
        }
        .foregroundColor(.white)
        .padding()
        .padding(.top, 44)
    }

    private var bottomBar: some View {
        HStack {
            Text(LocalizedStringKey("crop_hint")).font(.footnote).foregroundColor(.white.opacity(0.7))
            Spacer()
            Button(LocalizedStringKey("crop_reset")) { model.reset() }.disabled(model.isProcessing)
        }
        .foregroundColor(.white)
        .padding()
        .padding(.bottom, 24)
    }
}

/// Dims everything outside the crop window and draws its border and thirds grid.
struct CropOverlayView: View {
    let state: CropState

    var body: some View {
        let window = CGRect(x: CGFloat(state.windowLeft), y: CGFloat(state.windowTop),
                            width: CGFloat(state.windowWidth), height: CGFloat(state.windowHeight))
        ZStack {
            Path { path in
                path.addRect(CGRect(x: 0, y: 0, width: CGFloat(state.containerWidth), height: CGFloat(state.containerHeight)))
                path.addRect(window)
            }
            .fill(Color.black.opacity(0.6), style: FillStyle(eoFill: true))

            Path { path in
                for i in 1...2 {
                    let x = window.minX + window.width * CGFloat(i) / 3
                    let y = window.minY + window.height * CGFloat(i) / 3
                    path.move(to: CGPoint(x: x, y: window.minY)); path.addLine(to: CGPoint(x: x, y: window.maxY))
                    path.move(to: CGPoint(x: window.minX, y: y)); path.addLine(to: CGPoint(x: window.maxX, y: y))
                }
            }
            .stroke(Color.white.opacity(0.35), lineWidth: 0.5)

            Rectangle().path(in: window).stroke(Color.white, lineWidth: 2)
        }
        .allowsHitTesting(false)
    }
}

#Preview("Overlay Square") {
    let cropper = ImageCropper(imageWidth: 2000, imageHeight: 1500, aspectRatio: 1, maxZoom: 8, windowInset: 24)
    return CropOverlayView(state: cropper.layout(containerWidth: 390, containerHeight: 780))
        .background(Color.gray).preferredColorScheme(.dark)
}

#Preview("Overlay 16:9 iPad", traits: .fixedLayout(width: 820, height: 600)) {
    let cropper = ImageCropper(imageWidth: 2000, imageHeight: 1500, aspectRatio: 16.0 / 9.0, maxZoom: 8, windowInset: 24)
    return CropOverlayView(state: cropper.layout(containerWidth: 820, containerHeight: 600))
        .background(Color.gray)
}
