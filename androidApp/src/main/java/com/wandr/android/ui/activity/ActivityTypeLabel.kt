package com.wandr.android.ui.activity

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.wandr.android.R

/** Localized name of an activity type ("hiking" -> "Hiking"); unknown types fall back to the capitalized value. */
@Composable
internal fun activityTypeText(type: String): String = when (type) {
    "hiking" -> stringResource(R.string.activity_type_hiking)
    "running" -> stringResource(R.string.activity_type_running)
    "cycling" -> stringResource(R.string.activity_type_cycling)
    else -> type.replaceFirstChar { it.uppercase() }
}
