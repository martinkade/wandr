import SwiftUI

/// A round avatar: the photo, or the first letter of the name on a brand gradient while there is none (or it loads).
struct AvatarView: View {
    let url: String?
    let name: String
    var size: CGFloat = 40

    var body: some View {
        ZStack {
            LinearGradient(colors: [Color.wandrPrimary, Color.wandrPrimary.opacity(0.55)], startPoint: .topLeading, endPoint: .bottomTrailing)
            Text(name.prefix(1).uppercased().isEmpty ? "–" : name.prefix(1).uppercased())
                .font(.system(size: size * 0.42, weight: .bold, design: .rounded))
                .foregroundStyle(Color.wandrOnPrimary)
            if let url, let link = URL(string: url) {
                AsyncImage(url: link) { image in
                    image.resizable().scaledToFill()
                } placeholder: {
                    Color.clear
                }
            }
        }
        .frame(width: size, height: size)
        .clipShape(Circle())
        .accessibilityHidden(true)
    }
}

#Preview("Light Mode") {
    HStack { AvatarView(url: nil, name: "Alex"); AvatarView(url: nil, name: "Martin", size: 96) }.padding().preferredColorScheme(.light)
}

#Preview("Dark Mode") {
    AvatarView(url: nil, name: "Sophie", size: 64).padding().preferredColorScheme(.dark)
}
