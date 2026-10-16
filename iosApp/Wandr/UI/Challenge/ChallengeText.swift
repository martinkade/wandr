import Foundation
import SwiftUI
@preconcurrency import shared

/// Texts of a challenge, shared by card, details, rows and form; the same wording and rounding as on Android
/// (ChallengeValueText.kt).
enum ChallengeText {
    static func typeLabel(_ type: ChallengeType) -> LocalizedStringKey {
        switch type {
        case .elevation: return "challenge_type_elevation"
        case .time: return "challenge_type_time"
        default: return "challenge_type_distance"
        }
    }

    static func typeName(_ type: ChallengeType) -> String {
        switch type {
        case .elevation: return String(localized: "challenge_type_elevation")
        case .time: return String(localized: "challenge_type_time")
        default: return String(localized: "challenge_type_distance")
        }
    }

    static func unit(_ type: ChallengeType) -> String {
        switch type {
        case .elevation: return String(localized: "challenge_unit_elevation")
        case .time: return String(localized: "challenge_unit_time")
        default: return String(localized: "challenge_unit_distance")
        }
    }

    static func statusLabel(_ status: ChallengeStatus) -> LocalizedStringKey {
        switch status {
        case .draft: return "challenge_status_draft"
        case .planned: return "challenge_status_planned"
        case .completed: return "challenge_status_completed"
        case .expired: return "challenge_status_expired"
        default: return "challenge_status_active"
        }
    }

    static func scopeLabel(isTeam: Bool) -> LocalizedStringKey {
        isTeam ? "challenge_scope_team" : "challenge_scope_individual"
    }

    /// A stored value (meters / seconds) in friendly units, e.g. "100 km" or "5,000 m".
    static func value(_ type: ChallengeType, _ baseValue: Double) -> String {
        let number = ChallengeUnits.shared.toDisplay(type: type, baseValue: baseValue)
        return "\(number.formatted(.number.precision(.fractionLength(0...2)))) \(unit(type))"
    }

    /// "Distance: 100 km".
    static func goal(_ challenge: Challenge) -> String {
        let type = ChallengeType.companion.fromValue(value: challenge.type)
        return String(format: String(localized: "challenge_goal_text"), typeName(type), value(type, challenge.targetValue))
    }

    /// "30 days" / "5 hours": the length of the challenge.
    static func length(_ challenge: Challenge) -> String {
        let duration = challenge.duration
        let key = duration.unit == .hours ? "challenge_duration_hours" : "challenge_duration_days"
        return plural(key, Int(duration.value))
    }

    /// "Ends in 3 days", "Starts in 2 hours" or "Ended"; nil for a draft (it has no schedule yet).
    static func remaining(_ challenge: Challenge, status: ChallengeStatus, now: Date = .now) -> String? {
        let nowMillis = Int64(now.timeIntervalSince1970 * 1000)
        switch status {
        case .draft: return nil
        case .completed, .expired: return String(localized: "challenge_ended")
        case .planned:
            let time = RemainingTime(millis: challenge.startDate - nowMillis)
            return plural(time.isDays ? "challenge_starts_in_days" : "challenge_starts_in_hours", time.value)
        default:
            let time = RemainingTime(millis: challenge.endDate - nowMillis)
            return plural(time.isDays ? "challenge_ends_in_days" : "challenge_ends_in_hours", time.value)
        }
    }

    /// "30 days · Ends in 3 days".
    static func lengthAndRemaining(_ challenge: Challenge, status: ChallengeStatus) -> String {
        [length(challenge), remaining(challenge, status: status)].compactMap { $0 }.joined(separator: " · ")
    }

    /// "1 Oct 2026, 10:00 – 31 Oct 2026, 10:00".
    static func period(_ challenge: Challenge) -> String {
        "\(dateTime(challenge.startDate)) – \(dateTime(challenge.endDate))"
    }

    static func activityTypes(_ types: [String]) -> String {
        types.isEmpty ? String(localized: "challenge_activity_types_all") : types.map(activityType).joined(separator: ", ")
    }

    static func activityType(_ type: String) -> String {
        switch type {
        case "hiking", "running", "cycling": return NSLocalizedString("activity_type_\(type)", comment: "")
        default: return type.prefix(1).uppercased() + type.dropFirst()
        }
    }

    static func date(_ millis: Int64) -> Date { Date(timeIntervalSince1970: TimeInterval(millis) / 1000) }

    static func millis(_ date: Date) -> Int64 { Int64((date.timeIntervalSince1970 * 1000).rounded()) }

    private static func dateTime(_ millis: Int64) -> String {
        date(millis).formatted(date: .abbreviated, time: .shortened)
    }

    /// Plural entries live in Localizable.stringsdict (one / other).
    private static func plural(_ key: String, _ count: Int) -> String {
        String.localizedStringWithFormat(NSLocalizedString(key, comment: ""), count)
    }
}

/// How long until something happens, in the largest sensible unit: days from a day on, otherwise hours (at least 1).
struct RemainingTime: Equatable {
    let value: Int
    let isDays: Bool

    init(millis: Int64) {
        let hour: Int64 = 3_600_000
        let day = 24 * hour
        let positive = max(0, millis)
        if positive >= day {
            value = Int((positive + day - 1) / day)
            isDays = true
            return
        }
        let hours = max(1, Int((positive + hour - 1) / hour))
        // Just under a day rounds up to "1 day", not to "24 hours".
        value = hours >= 24 ? 1 : hours
        isDays = hours >= 24
    }
}

extension Challenge {
    var challengeType: ChallengeType { ChallengeType.companion.fromValue(value: type) }
    var isTeamChallenge: Bool { ChallengeScope.companion.fromValue(value: scope) == .team }

    /// Same as Java's String.hashCode (Android's seed), so a challenge gets the same contour pattern on both platforms.
    var contourSeed: Int32 {
        id.utf16.reduce(Int32(0)) { $0 &* 31 &+ Int32($1) }
    }
}

/// Sample challenges for previews (no network, no Koin).
enum ChallengePreviewData {
    static let start: Int64 = ChallengeText.millis(.now) - 5 * 86_400_000
    static let end: Int64 = ChallengeText.millis(.now) + 25 * 86_400_000

    static func challenge(
        id: String = "c1",
        title: String = "30-Day 100 km Hike",
        description: String? = "Hike 100 km in total within 30 days. Any hike counts!",
        scope: String = "group",
        type: String = "distance",
        target: Double = 100_000,
        requireAll: Bool = true,
        isActive: Bool = true,
        createdBy: String = "u1",
        activityTypes: [String] = []
    ) -> Challenge {
        Challenge(
            id: id, title: title, description: description, coverUrl: nil, scope: scope, type: type,
            targetValue: target, requireAllMembersCompletion: requireAll, startDate: start, endDate: end,
            createdBy: createdBy, createdAt: 0, updatedAt: 0, isActive: isActive, activityTypes: activityTypes
        )
    }

    static func team(_ id: String, _ name: String) -> Team {
        Team(id: id, name: name, description: nil, avatarUrl: nil, coverUrl: nil, inviteCode: "X7K9P2W1", createdBy: "u1", createdAt: 0, updatedAt: 0)
    }

    static let standings: [TeamStanding] = [
        TeamStanding(rank: 1, teamId: "t2", teamName: "City Runners", avatarUrl: nil, totalProgress: 3_800, progressPercentage: 76, memberCount: 4, completedMemberCount: 1, isCompleted: false),
        TeamStanding(rank: 2, teamId: "t1", teamName: "Alpine Trail Blazers", avatarUrl: nil, totalProgress: 2_500, progressPercentage: 50, memberCount: 3, completedMemberCount: 0, isCompleted: false),
    ]

    static let leaderboard: [LeaderboardEntry] = [
        LeaderboardEntry(rank: 1, userId: "u2", displayName: "Alex Kim", avatarUrl: nil, progressValue: 82_000, progressPercentage: 82, isCompleted: false),
        LeaderboardEntry(rank: 2, userId: "u1", displayName: "Martin Kade", avatarUrl: nil, progressValue: 75_000, progressPercentage: 75, isCompleted: false),
        LeaderboardEntry(rank: 2, userId: "u3", displayName: "Sam Lee", avatarUrl: nil, progressValue: 75_000, progressPercentage: 75, isCompleted: false),
        LeaderboardEntry(rank: 4, userId: "u4", displayName: "", avatarUrl: nil, progressValue: 100_000, progressPercentage: 100, isCompleted: true),
    ]
}
