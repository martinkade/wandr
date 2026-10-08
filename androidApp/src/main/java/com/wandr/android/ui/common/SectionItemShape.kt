package com.wandr.android.ui.common

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * The shape of an item in a list of connected cards: the first item has round top corners, the last one round bottom
 * corners, the corners in between are only slightly rounded (profile's teams, notifications).
 */
fun sectionItemShape(isFirstItemInSection: Boolean, isLastItemInSection: Boolean) =
    RoundedCornerShape(
        topStart = if (isFirstItemInSection) 16.dp else 4.dp,
        topEnd = if (isFirstItemInSection) 16.dp else 4.dp,
        bottomStart = if (isLastItemInSection) 16.dp else 4.dp,
        bottomEnd = if (isLastItemInSection) 16.dp else 4.dp
    )
