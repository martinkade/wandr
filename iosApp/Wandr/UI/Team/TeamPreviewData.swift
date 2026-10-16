#if DEBUG
@preconcurrency import shared

/// Sample teams and members for previews (plain Kotlin data classes, no Koin or network).
enum TeamPreviewData {
    static let teams: [Team] = [
        Team(id: "t1", name: "Alpine Trail Blazers", description: "Hiking team for weekend trips.", avatarUrl: nil,
             coverUrl: nil, inviteCode: "X7K9P2W1", createdBy: "u1", createdAt: 0, updatedAt: 0),
        Team(id: "t2", name: "City Runners", description: nil, avatarUrl: nil,
             coverUrl: nil, inviteCode: "Q3M8D5LA", createdBy: "u1", createdAt: 0, updatedAt: 0),
        Team(id: "t3", name: "Weekend Cyclists", description: "Long rides, good coffee.", avatarUrl: nil,
             coverUrl: nil, inviteCode: "A1B2C3D4", createdBy: "u2", createdAt: 0, updatedAt: 0)
    ]

    static let members: [TeamMember] = [
        TeamMember(id: "1", teamId: "t1", userId: "u1", role: .admin, username: "martinkade", displayName: "Martin Kade", avatarUrl: nil, joinedAt: 0),
        TeamMember(id: "2", teamId: "t1", userId: "u2", role: .member, username: "runner99", displayName: "Alex Runner", avatarUrl: nil, joinedAt: 0),
        TeamMember(id: "3", teamId: "t1", userId: "u3", role: .member, username: "sophie", displayName: "Sophie Hiker", avatarUrl: nil, joinedAt: 0)
    ]
}
#endif
