@preconcurrency import shared
import SwiftUI

/// 4:3 cover image (or a brand gradient placeholder). In edit mode it is tappable to change the cover.
struct TeamCoverView: View {
    var coverUrl: String? = nil
    var isEditing: Bool = false
    var isBusy: Bool = false
    var onImageReady: (Data) -> Void = { _ in }
    var onRemove: () -> Void = {}

    @State private var showSources = false

    var body: some View {
        ZStack(alignment: .bottomTrailing) {
            LinearGradient(
                colors: [Color.wandrPrimary, Color.wandrPrimary.opacity(0.55)],
                startPoint: .topLeading, endPoint: .bottomTrailing
            )

            if let coverUrl, let url = URL(string: coverUrl) {
                AsyncImage(url: url) { image in
                    image.resizable().scaledToFill()
                } placeholder: {
                    Color.clear
                }
                .accessibilityLabel(Text(LocalizedStringKey("team_cover_content_description")))
            }

            if isEditing && !isBusy {
                Text(LocalizedStringKey("team_cover_change"))
                    .font(.footnote.weight(.semibold))
                    .foregroundColor(.white)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 6)
                    .background(Color.black.opacity(0.6))
                    .clipShape(Capsule())
                    .padding(12)
            }

            if isBusy {
                Color.black.opacity(0.4)
                ProgressView().tint(.white).frame(maxWidth: .infinity, maxHeight: .infinity)
            }
        }
        .aspectRatio(CGFloat(CoverImageSpec.shared.ASPECT_RATIO), contentMode: .fit)
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .contentShape(Rectangle())
        .onTapGesture { if isEditing && !isBusy { showSources = true } }
        .imagePickerFlow(
            isPresented: $showSources,
            title: "team_cover_title",
            aspectRatio: CoverImageSpec.shared.ASPECT_RATIO,
            maxEdge: Int(CoverImageSpec.shared.MAX_EDGE_PX),
            jpegQuality: Int(CoverImageSpec.shared.JPEG_QUALITY),
            canRemove: coverUrl != nil,
            onImageReady: onImageReady,
            onRemove: onRemove
        )
    }
}

#Preview("Placeholder") { TeamCoverView().padding() }
#Preview("Editing Dark") { TeamCoverView(isEditing: true).padding().preferredColorScheme(.dark) }
#Preview("Busy iPad", traits: .fixedLayout(width: 700, height: 500)) { TeamCoverView(isEditing: true, isBusy: true).padding() }
