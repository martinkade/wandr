import ActivityKit
import AppIntents
import Foundation

/// The Live Activity of a running recording (lock screen and Dynamic Island). Compiled into the app and into the widget
/// extension: the app starts and updates it, the extension draws it.
struct RecordingActivityAttributes: ActivityAttributes {
    struct ContentState: Codable, Hashable {
        var distanceMeters: Double
        var isPaused: Bool
        /// While running: the moment the recording would have started without pauses. The system counts the time up from
        /// it by itself, so the app does not have to update every second.
        var timerStart: Date
        /// While paused: the frozen recording time.
        var elapsedSeconds: Double
    }
}

/// What the buttons of the Live Activity ask the recording to do.
enum RecordingCommand: String {
    case pause, resume
}

extension Notification.Name {
    /// Posted by the intents below; the recording screen listens (`userInfo["command"]` is a `RecordingCommand`).
    static let recordingCommand = Notification.Name("com.mediabeam.fitness.recordingCommand")
}

/// Live Activity intents run in the app's process, also while it is in the background, so the notification reaches it.
struct PauseRecordingIntent: LiveActivityIntent {
    static let title: LocalizedStringResource = "pause_button"

    func perform() async throws -> some IntentResult {
        NotificationCenter.default.post(name: .recordingCommand, object: nil, userInfo: ["command": RecordingCommand.pause.rawValue])
        return .result()
    }
}

struct ResumeRecordingIntent: LiveActivityIntent {
    static let title: LocalizedStringResource = "resume_button"

    func perform() async throws -> some IntentResult {
        NotificationCenter.default.post(name: .recordingCommand, object: nil, userInfo: ["command": RecordingCommand.resume.rawValue])
        return .result()
    }
}
