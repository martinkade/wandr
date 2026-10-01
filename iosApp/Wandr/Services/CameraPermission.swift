import AVFoundation

enum CameraPermission {
    static var status: AVAuthorizationStatus { AVCaptureDevice.authorizationStatus(for: .video) }

    /// Triggers the system prompt. Call only after the disclosure was confirmed.
    static func request() async -> Bool {
        await AVCaptureDevice.requestAccess(for: .video)
    }
}
