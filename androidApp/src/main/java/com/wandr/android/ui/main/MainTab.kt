package com.wandr.android.ui.main

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.wandr.android.R

enum class MainTab(@StringRes val labelRes: Int, @DrawableRes val iconRes: Int) {
    Activities(R.string.tab_activity, R.drawable.ic_tab_activity),
    Challenges(R.string.tab_challenges, R.drawable.ic_tab_challenges),
    Groups(R.string.tab_groups, R.drawable.ic_tab_groups),
    Profile(R.string.tab_profile, R.drawable.ic_tab_profile)
}
