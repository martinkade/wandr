@preconcurrency import shared
import SwiftUI

struct TeamMemberRowView: View {
    let displayName: String
    let username: String
    var avatarUrl: String? = nil
    let isAdmin: Bool

    var body: some View {
        let name = displayName.isEmpty ? username : displayName
        HStack(spacing: 12) {
            ZStack {
                Circle().fill(Color.wandrPrimary.opacity(0.25))
                if let avatarUrl, let url = URL(string: avatarUrl) {
                    AsyncImage(url: url) { image in
                        image.resizable().scaledToFill()
                    } placeholder: {
                        ProgressView()
                    }
                    .clipShape(Circle())
                } else {
                    Text(String(name.prefix(2)).uppercased())
                        .font(.subheadline.weight(.semibold))
                        .foregroundColor(.primary)
                }
            }
            .frame(width: 44, height: 44)

            VStack(alignment: .leading, spacing: 2) {
                Text(name).font(.body).foregroundColor(.primary)
                if !username.isEmpty {
                    Text("@\(username)").font(.caption).foregroundColor(.secondary)
                }
            }

            Spacer()

            Text(LocalizedStringKey(isAdmin ? "role_admin" : "role_member"))
                .font(.caption.weight(.semibold))
                .foregroundColor(isAdmin ? .wandrOnPrimary : .secondary)
                .padding(.horizontal, 10)
                .padding(.vertical, 4)
                .background(isAdmin ? Color.wandrPrimary : Color.secondary.opacity(0.15))
                .clipShape(Capsule())
        }
        .padding(.vertical, 6)
    }
}

#Preview("Light Mode") {
    VStack {
        TeamMemberRowView(displayName: "Martin Kade", username: "martinkade", isAdmin: true)
        TeamMemberRowView(displayName: "Alex Runner", username: "runner99", isAdmin: false)
    }
    .padding()
}

#Preview("Dark Mode") {
    VStack {
        TeamMemberRowView(displayName: "Martin Kade", username: "martinkade", isAdmin: true)
        TeamMemberRowView(displayName: "Alex Runner", username: "runner99", isAdmin: false)
    }
    .padding()
    .preferredColorScheme(.dark)
}
