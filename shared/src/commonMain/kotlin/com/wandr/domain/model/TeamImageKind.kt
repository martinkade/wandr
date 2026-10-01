package com.wandr.domain.model

/** The two images a team can have; stored in the `team-covers` bucket as `<teamId>/<filePrefix>_<timestamp>.jpg`. */
enum class TeamImageKind(val filePrefix: String) {
    AVATAR("avatar"),
    COVER("cover")
}
