package com.wandr.android.ui.challenge

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import com.wandr.android.R
import com.wandr.domain.model.ChallengeType
import com.wandr.presentation.challenge.ChallengeUnits
import java.text.NumberFormat

internal fun challengeUnitRes(type: ChallengeType) = when (type) {
    ChallengeType.DISTANCE -> R.string.challenge_unit_distance
    ChallengeType.ELEVATION -> R.string.challenge_unit_elevation
    ChallengeType.TIME -> R.string.challenge_unit_time
}

/** A stored challenge value (meters / seconds) as text in friendly units, e.g. "100 km" or "5,000 m". */
@Composable
internal fun challengeValueText(type: ChallengeType, baseValue: Double): String {
    val locale = LocalConfiguration.current.locales[0]
    val number = remember(type, baseValue, locale) {
        NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = 2 }
            .format(ChallengeUnits.toDisplay(type, baseValue))
    }
    return "$number ${stringResource(challengeUnitRes(type))}"
}
