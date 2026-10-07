package com.wandr.android.ui.common

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wandr.android.R
import com.wandr.android.ui.theme.WandrTheme

/** How fast the header moves compared to the content while scrolling: 0.5 = half as fast (parallax). */
private const val PARALLAX_FACTOR = 0.5f
private val TopBarHeight = 64.dp

/** Opacity of the dark discs behind the top bar's icons while the header is fully shown. */
private const val HEADER_DISC_ALPHA = 0.4f

/**
 * Page with a top bar and a header (a cover image) that scrolls away with a parallax effect. The top bar is transparent
 * over the header and fades into a solid bar with the [title] while the header collapses. The header and [content] share
 * one scroll; the header extends under the status bar.
 *
 * @param header the picture area, [headerHeight] high; it moves at half the speed of the content
 * @param actions actions of the top bar; they get the color that fits the bar right now (white over the header)
 * @param content the page below the header, in the scrolling column
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollapsingHeaderScaffold(
    title: String,
    onBack: () -> Unit,
    headerHeight: Dp,
    header: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.(contentColor: Color) -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    val scroll = rememberScrollState()
    val density = LocalDensity.current
    val statusBarPx = WindowInsets.statusBars.getTop(density)
    // The header is fully collapsed when only the top bar's height of it is left.
    val collapseDistancePx =
        with(density) { headerHeight.toPx() - TopBarHeight.toPx() } - statusBarPx
    val collapse by remember(collapseDistancePx) {
        androidx.compose.runtime.derivedStateOf {
            (scroll.value / collapseDistancePx.coerceAtLeast(
                1f
            )).coerceIn(0f, 1f)
        }
    }
    val onBar = lerp(Color.White, MaterialTheme.colorScheme.onSurface, collapse)

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.alpha(collapse)
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        colors = IconButtonDefaults.iconButtonColors(
                            // A dark disc keeps the arrow readable on any photo; it fades with the header.
                            containerColor = Color.Black.copy(alpha = HEADER_DISC_ALPHA * (1f - collapse)),
                            contentColor = onBar
                        )
                    ) {
                        Icon(
                            painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.back_button)
                        )
                    }
                },
                actions = {
                    // The same dark disc as behind the back arrow (a pill for several or wide actions), fading with the header.
                    Row(
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = HEADER_DISC_ALPHA * (1f - collapse))),
                        verticalAlignment = Alignment.CenterVertically
                    ) { actions(onBar) }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = collapse),
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        snackbarHost = snackbarHost
    ) { padding ->
        // The top padding is ignored on purpose: the header runs under the top bar.
        Column(
            Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding())
                .verticalScroll(scroll)
        ) {
            Box(Modifier
                .fillMaxWidth()
                .height(headerHeight)
                .clipToBounds()) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer { translationY = scroll.value * PARALLAX_FACTOR }
                ) { header() }
            }
            content()
        }
    }
}

@Preview(name = "Light Mode", showBackground = true, heightDp = 700)
@Preview(
    name = "Dark Mode",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    heightDp = 700
)
@Preview(name = "Font Scale 1.5x", fontScale = 1.5f, showBackground = true, heightDp = 900)
@Composable
private fun CollapsingHeaderScaffoldPreview() {
    WandrTheme {
        CollapsingHeaderScaffold(
            title = "Group Details",
            onBack = {},
            headerHeight = 220.dp,
            header = { Box(Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.primary)) },
            actions = { color ->
                Text(
                    "Edit",
                    color = color,
                    modifier = Modifier.padding(end = 16.dp)
                )
            }
        ) {
            repeat(30) { Text("Row $it", modifier = Modifier.padding(16.dp)) }
        }
    }
}
