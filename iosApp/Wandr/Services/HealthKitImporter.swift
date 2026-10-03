import Foundation
import HealthKit
import UIKit
@preconcurrency import shared

/// Reads workouts from Apple Health (read-only: nothing is ever written) and hands them to the shared inbox, which
/// imports them and opens the conflict wizard when they overlap existing activities. Workouts written by WANDR itself
/// (the watch app) are skipped.
/// Today's steps and floors climbed.
struct DailyHealthSummary: Equatable {
    var steps: Int
    var floors: Int
}

@MainActor
final class HealthKitImporter: ObservableObject {
    @Published var showDisclosure = false
    @Published var message: LocalizedStringResource?
    @Published var isImporting = false
    /// Only set once access was requested; Apple Health does not reveal whether reading was allowed, so denied reads show 0.
    @Published var summary: DailyHealthSummary?

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
            HKQuantityType(.distanceCycling),
            HKQuantityType(.stepCount),
            HKQuantityType(.flightsClimbed)
        ]
    }

    /// True once the user went through the disclosure and the system access request, i.e. the connection is enabled.
    /// Apple Health does not reveal whether reading was allowed, so this is the best available signal.
    var isEnabled: Bool { HKHealthStore.isHealthDataAvailable() && defaults.bool(forKey: disclosedKey) }

    /// Called whenever the app becomes active: reads new workouts and today's steps/floors, silently, if the connection is
    /// enabled. Overlaps with existing activities open the conflict wizard; nothing is shown otherwise.
    func syncIfEnabled() async {
        guard isEnabled, !isImporting else { return }
        await refreshSummary()
        // Quick app switches should not query Apple Health every time.
        let last = defaults.double(forKey: lastReadKey)
        guard Date().timeIntervalSince1970 - last > minSyncInterval else { return }
        _ = try? await readAndOffer()
    }

    /// The settings button: connects Apple Health the first time, afterwards opens the app's settings, where the
    /// Health access can be changed.
    func openSettingsOrConnect() {
        if isEnabled {
            if let url = URL(string: UIApplication.openSettingsURLString) { UIApplication.shared.open(url) }
        } else {
            start()
        }
    }

    private let minSyncInterval: TimeInterval = 60

    private func readAndOffer() async throws -> Int {
        let workouts = try await readWorkouts()
        workouts.forEach { _ = inbox.offerJson(json: $0) }
        defaults.set(Date().timeIntervalSince1970, forKey: lastReadKey)
        await refreshSummary()
        return workouts.count
    }

    private func refreshSummary() async {
        let start = Calendar.current.startOfDay(for: Date())
        let predicate = HKQuery.predicateForSamples(withStart: start, end: Date(), options: .strictStartDate)

        func total(_ type: HKQuantityType, _ unit: HKUnit) async -> Int {
            let descriptor = HKStatisticsQueryDescriptor(predicate: .quantitySample(type: type, predicate: predicate), options: .cumulativeSum)
            let value = try? await descriptor.result(for: store)?.sumQuantity()?.doubleValue(for: unit)
            return Int(value ?? 0)
        }
        summary = DailyHealthSummary(
            steps: await total(HKQuantityType(.stepCount), .count()),
            floors: await total(HKQuantityType(.flightsClimbed), .count())
        )
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
            let count = try await readAndOffer()
            message = count == 0 ? "healthkit_nothing_new" : "healthkit_found \(count)"
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
