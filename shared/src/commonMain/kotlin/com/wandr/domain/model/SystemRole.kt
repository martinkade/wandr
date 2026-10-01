package com.wandr.domain.model

/** Application-wide role of a user (`profiles.system_role`). */
enum class SystemRole(val value: String) {
    USER("user"),
    MANAGER("manager");

    companion object {
        /** Unknown values fall back to the least privileged role. */
        fun fromValue(value: String?): SystemRole = entries.firstOrNull { it.value == value } ?: USER
    }
}
