package com.wandr.android.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LabeledLoading(
    isLoading: Boolean,
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) = Column(
    modifier = modifier
        .padding(32.dp),
    verticalArrangement = Arrangement.spacedBy(32.dp, Alignment.CenterVertically),
    horizontalAlignment = Alignment.CenterHorizontally,
) {
    if (!isLoading) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge
        )
        if (message != null) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
        if (onAction != null && action != null) {
            Button(onClick = onAction) { Text(action) }
        }
    } else {
        CircularProgressIndicator()
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}