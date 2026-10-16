import SwiftUI
@preconcurrency import shared

/// The participation action of a challenge: prominent glass for joining / enrolling, clear glass for leaving /
/// withdrawing (the quieter style keeps the user from leaving by accident).
struct ChallengeActionButtonView: View {
    let action: ChallengeAction
    var onTap: () -> Void = {}

    private var isPositive: Bool { action == .join || action == .enrollTeam }

    private var label: LocalizedStringKey {
        switch action {
        case .leave: return "leave_challenge_button"
        case .enrollTeam: return "challenge_enroll_team_button"
        case .withdrawTeam: return "challenge_withdraw_team_button"
        default: return "join_challenge_button"
        }
    }

    private var systemImage: String {
        switch action {
        case .leave: return "rectangle.portrait.and.arrow.right"
        case .enrollTeam: return "person.3.fill"
        case .withdrawTeam: return "person.3"
        default: return "flag.checkered"
        }
    }

    var body: some View {
        if isPositive {
            button.wandrProminentButton()
        } else {
            button.wandrGlassButton()
        }
    }

    private var button: some View {
        Button(action: onTap) {
            Label(label, systemImage: systemImage)
                .font(.headline)
                .frame(maxWidth: .infinity)
        }
        .controlSize(.large)
        .sensoryFeedback(.impact, trigger: action)
    }
}

#Preview("Light Mode") {
    VStack(spacing: 12) {
        ForEach(ChallengeAction.entries, id: \.self) { ChallengeActionButtonView(action: $0) }
    }
    .padding()
    .preferredColorScheme(.light)
}

#Preview("Dark Mode") {
    VStack(spacing: 12) {
        ChallengeActionButtonView(action: .join)
        ChallengeActionButtonView(action: .withdrawTeam)
    }
    .padding()
    .preferredColorScheme(.dark)
}

#Preview("Large Text, iPhone SE", traits: .fixedLayout(width: 320, height: 300)) {
    ChallengeActionButtonView(action: .enrollTeam)
        .padding()
        .environment(\.dynamicTypeSize, .accessibility2)
}
