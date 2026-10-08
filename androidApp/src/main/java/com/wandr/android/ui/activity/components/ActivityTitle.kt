package com.wandr.android.ui.activity.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.wandr.domain.model.Activity

@Composable
fun ActivityTitle(
    activity: Activity,
    modifier: Modifier = Modifier,
    maxDescriptionLines: Int = Int.MAX_VALUE
) {
    Text(
        text = activity.title,
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        modifier = modifier
    )
    if (!activity.description.isNullOrBlank()) {
        Text(
            text = activity.description ?: "",
            style = MaterialTheme.typography.bodyMedium,
            maxLines = maxDescriptionLines,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier
        )
    }
}