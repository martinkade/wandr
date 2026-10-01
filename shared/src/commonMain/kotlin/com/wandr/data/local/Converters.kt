package com.wandr.data.local

import androidx.room3.ColumnTypeConverter
import com.wandr.domain.model.SystemRole
import com.wandr.domain.model.TeamRole

/** Persists role enums as their Postgres string values, so the column type stays TEXT. */
class Converters {
    @ColumnTypeConverter
    fun systemRoleToString(role: SystemRole): String = role.value

    @ColumnTypeConverter
    fun stringToSystemRole(value: String): SystemRole = SystemRole.fromValue(value)

    @ColumnTypeConverter
    fun teamRoleToString(role: TeamRole): String = role.value

    @ColumnTypeConverter
    fun stringToTeamRole(value: String): TeamRole = TeamRole.fromValue(value)
}
