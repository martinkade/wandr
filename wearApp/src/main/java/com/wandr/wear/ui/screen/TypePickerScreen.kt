package com.wandr.wear.ui.screen

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.wandr.wear.R
import com.wandr.wear.model.WearActivityType
import com.wandr.wear.ui.WearPreviews
import com.wandr.wear.ui.theme.WandrWearTheme

@Composable
fun TypePickerScreen(onSelect: (WearActivityType) -> Unit, modifier: Modifier = Modifier) {
    val listState = rememberScalingLazyListState()
    ScreenScaffold(scrollState = listState, modifier = modifier) { contentPadding ->
        ScalingLazyColumn(state = listState, contentPadding = contentPadding) {
            item { ListHeader { Text(stringResource(R.string.picker_title)) } }
            WearActivityType.entries.forEach { type ->
                item(key = type.name) {
                    Button(
                        onClick = { onSelect(type) },
                        label = { Text(stringResource(type.labelRes)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@WearPreviews
@Composable
private fun TypePickerScreenPreview() {
    WandrWearTheme { TypePickerScreen(onSelect = {}) }
}
