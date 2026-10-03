package com.wandr.android.ui.main

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.wandr.android.R

enum class MainTab(@StringRes val labelRes: Int, @DrawableRes val iconRes: Int) {
    Feed(R.string.tab_feed, R.drawable.ic_tab_feed),
    Challenges(R.string.tab_challenges, R.drawable.ic_tab_challenges),
    Groups(R.string.tab_groups, R.drawable.ic_tab_groups),
    Profile(R.string.tab_profile, R.drawable.ic_tab_profile)
}
