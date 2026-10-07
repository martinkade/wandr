import Foundation
@preconcurrency import shared

extension AppError {
    /// The localized text for the user: what happened and, where it helps, what to do. Raw server texts (URLs, headers,
    /// error codes) are never shown; they stay in the error's cause for logs.
    var userMessage: String {
        switch self {
        case is AppError.InvalidEmail: return String(localized: "error_invalid_email")
        case let e as AppError.PasswordTooShort: return String(localized: "error_password_too_short \(Int(e.minLength))")
        case is AppError.UsernameRequired: return String(localized: "error_username_required")
        case let e as AppError.InvalidInput: return e.inputMessage
        case is AppError.InvalidCredentials: return String(localized: "error_invalid_credentials")
        case is AppError.EmailNotConfirmed: return String(localized: "error_email_not_confirmed")
        case is AppError.EmailConfirmationRequired: return String(localized: "error_email_confirmation_required")
        case is AppError.UserAlreadyExists: return String(localized: "error_user_already_exists")
        case is AppError.WeakPassword: return String(localized: "error_weak_password")
        case is AppError.SignupDisabled: return String(localized: "error_signup_disabled")
        case is AppError.UserBanned: return String(localized: "error_user_banned")
        case is AppError.SessionExpired: return String(localized: "error_session_expired")
        case is AppError.Network: return String(localized: "error_network")
        case is AppError.Timeout: return String(localized: "error_timeout")
        case is AppError.RateLimited: return String(localized: "error_rate_limited")
        case is AppError.PermissionDenied: return String(localized: "error_permission_denied")
        case is AppError.NotFound: return String(localized: "error_not_found")
        case is AppError.Conflict: return String(localized: "error_conflict")
        case is AppError.Validation: return String(localized: "error_validation")
        case is AppError.Server: return String(localized: "error_server")
        case is AppError.LocalStorage: return String(localized: "error_local_storage")
        default: return String(localized: "error_unknown")
        }
    }

    /// "Confirm your email" is good news, not an error.
    var isInformational: Bool {
        self is AppError.EmailConfirmationRequired
    }
}

private extension AppError.InvalidInput {
    var inputMessage: String {
        switch problem {
        case .titleRequired: return String(localized: "error_input_title_required")
        case .nameRequired: return String(localized: "error_input_name_required")
        case .displayNameRequired: return String(localized: "error_input_display_name_required")
        case .targetNotPositive: return String(localized: "error_input_target_not_positive")
        case .endBeforeStart: return String(localized: "error_input_end_before_start")
        case .negativeValues: return String(localized: "error_input_negative_values")
        case .commentEmpty: return String(localized: "error_input_comment_empty")
        case .commentTooLong: return String(localized: "error_input_comment_too_long \(Int(truncating: limit ?? 0))")
        case .imageEmpty: return String(localized: "error_input_image_empty")
            // The limit is in bytes; people think in megabytes.
        case .imageTooLarge: return String(localized: "error_input_image_too_large \((Int(truncating: limit ?? 0)) / (1024 * 1024))")
        case .inviteCodeRequired: return String(localized: "error_input_invite_code_required")
        case .inviteCodeInvalid: return String(localized: "error_input_invite_code_invalid")
        case .unsupportedReaction: return String(localized: "error_input_unsupported_reaction")
        case .nothingToOrder: return String(localized: "error_input_nothing_to_order")
        case .nothingLeftToTrim: return String(localized: "error_input_nothing_left_to_trim")
        case .wrongChallengeScope: return String(localized: "error_input_wrong_challenge_scope")
            // Kotlin enums are classes in Swift, so the switch cannot be checked for completeness.
        default: return String(localized: "error_input_generic")
        }
    }
}
