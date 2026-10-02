import Foundation
import HealthKit
@preconcurrency import shared

/// Reads workouts from Apple Health (read-only: nothing is ever written) and hands them to the shared inbox, which
/// imports them and opens the conflict wizard when they overlap existing activities. Workouts written by WANDR itself
/// (the watch app) are skipped.
@MainActor
final class HealthKitImporter: ObservableObject {
    @Published var showDisclosure = false
    @Published var message: LocalizedStringResource?
    @Published var isImporting = false

    private let store = HKHealthStore()
    private let inbox = IosDependencies.shared.watchWorkoutInbox()
    private let defaults = UserDefaults.standard
    private let lastReadKey = "healthkit_last_read"
    private let disclosedKey = "healthkit_disclosure_shown"
    private let initialLookback: TimeInterval = 30 * 24 * 3600

    private var readTypes: Set<HKObjectType> {
        [
            HKObjectType.workoutType(),
            HKQuantityType(.distanceWalkingRunning),
            HKQuantityType(.distanceCycling)
        ]
    }

    /// The button action: explains why access is needed first (once), then asks and imports.
    func start() {
        guard HKHealthStore.isHealthDataAvailable() else {
            message = "healthkit_unavailable"
            return
        }
        if defaults.bool(forKey: disclosedKey) {
            Task { await runImport() }
        } else {
            showDisclosure = true
        }
    }

    /// Continues after the disclosure alert was confirmed.
    func disclosureConfirmed() {
        defaults.set(true, forKey: disclosedKey)
        Task { await runImport() }
    }

    private func runImport() async {
        isImporting = true
        defer { isImporting = false }
        do {
            // Read access cannot be queried; the system only asks the first time.
            try await store.requestAuthorization(toShare: [], read: readTypes)
            let workouts = try await readWorkouts()
            workouts.forEach { _ = inbox.offerJson(json: $0) }
            defaults.set(Date().timeIntervalSince1970, forKey: lastReadKey)
            message = workouts.isEmpty ? "healthkit_nothing_new" : "healthkit_found \(workouts.count)"
        } catch {
            message = "healthkit_failed"
        }
    }

    private func readWorkouts() async throws -> [String] {
        let now = Date()
        let last = defaults.double(forKey: lastReadKey)
        // Late-synced workouts are caught by reading a day back; the import skips ones it already knows.
        let since = last > 0 ? Date(timeIntervalSince1970: last).addingTimeInterval(-24 * 3600) : now.addingTimeInterval(-initialLookback)

        let predicate = HKQuery.predicateForSamples(withStart: since, end: now, options: .strictStartDate)
        let descriptor = HKSampleQueryDescriptor(
            predicates: [.workout(predicate)],
            sortDescriptors: [SortDescriptor(\.startDate)]
        )
        let workouts = try await descriptor.result(for: store)
        return workouts.compactMap { HealthKitWorkoutMapper.json(for: $0) }
    }
}

/// Turns an `HKWorkout` into the JSON wire format of the shared `WatchWorkout` (see `WatchWorkoutCodec`).
enum HealthKitWorkoutMapper {
    static let ownBundlePrefix = "com.mediabeam.fitness"

    static func activityType(for type: HKWorkoutActivityType) -> String? {
        switch type {
        case .hiking, .walking: return "hiking"
        case .running: return "running"
        case .cycling: return "cycling"
        default: return nil
        }
    }

    static func json(for workout: HKWorkout) -> String? {
        guard let type = activityType(for: workout.workoutActivityType),
              !workout.sourceRevision.source.bundleIdentifier.hasPrefix(ownBundlePrefix),
              workout.endDate > workout.startDate else { return nil }

        let distanceType = type == "cycling" ? HKQuantityType(.distanceCycling) : HKQuantityType(.distanceWalkingRunning)
        let distance = workout.statistics(for: distanceType)?.sumQuantity()?.doubleValue(for: .meter()) ?? 0
        let ascent = (workout.metadata?[HKMetadataKeyElevationAscended] as? HKQuantity)?.doubleValue(for: .meter()) ?? 0

        let payload: [String: Any] = [
            "id": workout.uuid.uuidString.lowercased(),
            "activity_type": type,
            "start_time": Int64(workout.startDate.timeIntervalSince1970 * 1000),
            "end_time": Int64(workout.endDate.timeIntervalSince1970 * 1000),
            "distance_meters": distance,
            "elevation_gain_meters": ascent,
            "source": "apple_health",
            "version": 1
        ]
        guard let data = try? JSONSerialization.data(withJSONObject: payload) else { return nil }
        return String(data: data, encoding: .utf8)
    }
}
