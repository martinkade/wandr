import Foundation

/// Persistent queue of workouts (as JSON strings, keyed by workout id) that were not yet confirmed as delivered.
struct WorkoutOutbox: Sendable {
    let fileURL: URL

    init(fileURL: URL) {
        self.fileURL = fileURL
    }

    static var `default`: WorkoutOutbox {
        let dir = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0]
        try? FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        return WorkoutOutbox(fileURL: dir.appendingPathComponent("workout-outbox.json"))
    }

    /// Pending JSON strings by workout id.
    func pending() -> [String: String] {
        guard let data = try? Data(contentsOf: fileURL),
              let map = try? JSONDecoder().decode([String: String].self, from: data) else { return [:] }
        return map
    }

    func add(id: String, json: String) {
        var map = pending()
        map[id] = json
        write(map)
    }

    func remove(id: String) {
        var map = pending()
        guard map.removeValue(forKey: id) != nil else { return }
        write(map)
    }

    private func write(_ map: [String: String]) {
        guard let data = try? JSONEncoder().encode(map) else { return }
        try? data.write(to: fileURL, options: .atomic)
    }
}
