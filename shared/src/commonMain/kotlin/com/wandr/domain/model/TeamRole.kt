package com.wandr.domain.model

/** Role of a user inside one team (`team_members.role`, Postgres enum `team_role`). */
enum class TeamRole(val value: String) {
    ADMIN("admin"),
    MEMBER("member");

    companion object {
        /** Unknown values fall back to the least privileged role. */
        fun fromValue(value: String?): TeamRole = entries.firstOrNull { it.value == value } ?: MEMBER
    }
}
