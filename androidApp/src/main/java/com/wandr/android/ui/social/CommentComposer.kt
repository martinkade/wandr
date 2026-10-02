package com.wandr.android.ui.social

import android.content.res.Configuration
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wandr.android.R
import com.wandr.android.ui.theme.WandrTheme

/** Text field with a send button; the text is cleared after sending. */
@Composable
fun CommentComposer(
    isPosting: Boolean,
    onSend: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var text by rememberSaveable { mutableStateOf("") }
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            placeholder = { Text(stringResource(R.string.social_comment_hint)) },
            enabled = !isPosting,
            maxLines = 4,
            modifier = Modifier.weight(1f)
        )
        IconButton(
            onClick = {
                onSend(text)
                text = ""
            },
            enabled = text.isNotBlank() && !isPosting,
            modifier = Modifier.padding(start = 4.dp)
        ) {
            Icon(painterResource(R.drawable.ic_send), contentDescription = stringResource(R.string.social_send))
        }
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Preview(name = "Font Scale 1.5x", fontScale = 1.5f, showBackground = true)
@Composable
private fun CommentComposerPreview() {
    WandrTheme { CommentComposer(isPosting = false, onSend = {}, modifier = Modifier.padding(16.dp)) }
}
