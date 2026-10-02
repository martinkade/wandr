package com.wandr.wear.ui

import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.tooling.preview.devices.WearDevices

/** Small round, large round and large round with a large font scale, all in dark mode (Wear OS is always dark). */
@Preview(name = "Small round", device = WearDevices.SMALL_ROUND, uiMode = Configuration.UI_MODE_NIGHT_YES, showSystemUi = true)
@Preview(name = "Large round", device = WearDevices.LARGE_ROUND, uiMode = Configuration.UI_MODE_NIGHT_YES, showSystemUi = true)
@Preview(name = "Large round, font 1.5", device = WearDevices.LARGE_ROUND, fontScale = 1.5f, uiMode = Configuration.UI_MODE_NIGHT_YES, showSystemUi = true)
annotation class WearPreviews
