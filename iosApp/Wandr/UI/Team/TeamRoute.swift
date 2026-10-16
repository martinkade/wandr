import Foundation

/// Navigation value for a team's details. A dedicated type (not a bare String) so it never collides with other
/// `navigationDestination(for:)` registrations in the same NavigationStack.
struct TeamRoute: Hashable {
    let teamId: String
}
