@preconcurrency import shared
import SwiftUI

/// The team details without state: a cover header that stretches when pulled down and moves with parallax when
/// scrolled, a sheet overlapping it with the avatar on its edge, name, description, the Invite button and the members.
/// Owners and admins get a toolbar menu (edit, change cover, change team photo) and can tap the avatar.
struct TeamDetailContentView: View {
    let teamId: String
    let name: String
    var teamDescription: String = ""
    var avatarUrl: String? = nil
    var coverUrl: String? = nil
    var members: [TeamMember] = []
    var canEdit: Bool = false
    var isImageUpdating: Bool = false
    var onInvite: () -> Void = {}
    var onEdit: () -> Void = {}
    var onChangeCover: () -> Void = {}
    var onChangePhoto: () -> Void = {}

    /// Once the name has scrolled under the navigation bar, the bar shows it.
    @State private var showsBarTitle = false

    private let headerHeight: CGFloat = 280
    private let sheetOverlap: CGFloat = 28
    private let avatarSize: CGFloat = 96

    var body: some View {
        ScrollView {
            VStack(spacing: 0) {
                header
                sheet
            }
        }
        .ignoresSafeArea(edges: .top)
        .background(Color(.systemGroupedBackground).ignoresSafeArea())
        .navigationTitle(showsBarTitle ? name : "")
        .navigationBarTitleDisplayMode(.inline)
        .toolbarBackground(showsBarTitle ? .automatic : .hidden, for: .navigationBar)
        .toolbar {
            if canEdit {
                ToolbarItem(placement: .topBarTrailing) {
                    Menu {
                        Button(LocalizedStringKey("profile_edit_button"), systemImage: "pencil", action: onEdit)
                        Button(LocalizedStringKey("team_change_cover"), systemImage: "photo", action: onChangeCover)
                        Button(LocalizedStringKey("team_change_photo"), systemImage: "person.crop.circle", action: onChangePhoto)
                    } label: {
                        Image(systemName: "ellipsis")
                    }
                    .accessibilityLabel(Text(LocalizedStringKey("team_more_actions")))
                }
            }
        }
        .animation(.easeInOut(duration: 0.2), value: showsBarTitle)
    }

    private var header: some View {
        let height = headerHeight
        return TeamCoverView(coverUrl: coverUrl, seed: TeamCoverView.seed(for: teamId), isBusy: isImageUpdating)
            .frame(height: height)
            .visualEffect { content, proxy in
                let minY = proxy.frame(in: .scrollView(axis: .vertical)).minY
                // Pulled down: grow from the bottom edge to fill the gap. Scrolled up: follow at half speed.
                return content
                    .scaleEffect(minY > 0 ? 1 + minY / height : 1, anchor: .bottom)
                    .offset(y: minY > 0 ? 0 : -minY * 0.5)
            }
            .onGeometryChange(for: Bool.self) { proxy in
                proxy.frame(in: .scrollView(axis: .vertical)).minY < -(height - 120)
            } action: { scrolledAway in
                showsBarTitle = scrolledAway
            }
    }

    private var sheet: some View {
        VStack(spacing: 12) {
            Text(name)
                .font(.title2.weight(.bold))
                .multilineTextAlignment(.center)

            if !teamDescription.isEmpty {
                Text(teamDescription)
                    .font(.body)
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.center)
            }

            Button(action: onInvite) {
                Label(LocalizedStringKey("team_invite_button"), systemImage: "qrcode")
                    .font(.headline)
                    .padding(.horizontal, 8)
            }
            .wandrGlassButton()
            .controlSize(.large)
            .padding(.top, 4)

            memberList
                .padding(.top, 16)
        }
        .padding(.top, avatarSize / 2 + 12)
        .padding(.horizontal, 16)
        .padding(.bottom, 32)
        .frame(maxWidth: .infinity)
        .background(
            Color(.systemGroupedBackground),
            in: UnevenRoundedRectangle(topLeadingRadius: sheetOverlap, topTrailingRadius: sheetOverlap, style: .continuous)
        )
        .overlay(alignment: .top) {
            // The avatar sits on the edge between cover and sheet.
            AvatarPickerView(
                displayName: name,
                avatarUrl: avatarUrl,
                isBusy: isImageUpdating,
                isEnabled: canEdit,
                size: avatarSize,
                onPick: onChangePhoto
            )
            .padding(4)
            .background(Color(.systemGroupedBackground), in: Circle())
            .offset(y: -avatarSize / 2 - 4)
        }
        .padding(.top, -sheetOverlap)
    }

    private var memberList: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(String.localizedStringWithFormat(NSLocalizedString("team_members_count", comment: ""), members.count))
                .font(.headline)
                .padding(.horizontal, 8)
                .padding(.bottom, 8)

            if members.isEmpty {
                Text(LocalizedStringKey("team_no_members"))
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .padding(.horizontal, 8)
            } else {
                ForEach(Array(members.enumerated()), id: \.element.id) { index, member in
                    TeamMemberRowView(
                        displayName: member.displayName,
                        username: member.username,
                        avatarUrl: member.avatarUrl,
                        isAdmin: member.role == .admin,
                        isFirst: index == 0,
                        isLast: index == members.count - 1
                    )
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

#Preview("Light Mode") {
    NavigationStack {
        TeamDetailContentView(
            teamId: "t1", name: "Alpine Trail Blazers", teamDescription: "Hiking team for weekend trips.",
            members: TeamPreviewData.members, canEdit: true
        )
    }
    .preferredColorScheme(.light)
}

#Preview("Dark Mode") {
    NavigationStack {
        TeamDetailContentView(teamId: "t2", name: "City Runners", members: TeamPreviewData.members, isImageUpdating: true)
    }
    .preferredColorScheme(.dark)
}

#Preview("Large text, no members") {
    NavigationStack {
        TeamDetailContentView(teamId: "t3", name: "Weekend Cyclists", teamDescription: "Long rides, good coffee.", canEdit: true)
    }
    .dynamicTypeSize(.accessibility2)
}
