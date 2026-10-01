import SwiftUI

struct InitView: View {
    var errorMessage: String? = nil
    var onRetry: () -> Void = {}

    var body: some View {
        VStack(spacing: 16) {
            if let errorMessage {
                Text(LocalizedStringKey("init_failed_title"))
                    .font(.title2)
                    .bold()
                Text(errorMessage)
                    .font(.subheadline)
                    .foregroundColor(.red)
                    .multilineTextAlignment(.center)
                Button(action: onRetry) {
                    Text(LocalizedStringKey("retry_button"))
                }
                .buttonStyle(.borderedProminent)
            } else {
                ProgressView()
                    .controlSize(.large)
                Text(LocalizedStringKey("init_loading"))
                    .font(.subheadline)
                    .foregroundColor(.secondary)
            }
        }
        .padding(24)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color(.systemBackground))
    }
}

#Preview("Loading") { InitView() }
#Preview("Loading Dark") { InitView().preferredColorScheme(.dark) }
#Preview("Failed iPad", traits: .fixedLayout(width: 820, height: 1180)) {
    InitView(errorMessage: "Database could not be opened")
}
#Preview("Failed Dark") { InitView(errorMessage: "Database could not be opened").preferredColorScheme(.dark) }
