package com.dare.music.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import coil3.compose.AsyncImage
import com.dare.music.LocalPlayerConnection

@Composable
fun DareMiniPlayer(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onClose: () -> Unit = {},
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val currentSong by playerConnection.mediaMetadata.collectAsState()
    val isPlaying   by playerConnection.isPlaying.collectAsState()

    if (currentSong == null) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AsyncImage(
            model              = currentSong?.thumbnailUrl,
            contentDescription = null,
            contentScale       = ContentScale.Crop,
            modifier           = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(6.dp)),
        )
        Column(
            modifier            = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text     = currentSong?.title ?: "",
                style    = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val artists = currentSong?.artists?.joinToString { it.name }.orEmpty()
            if (artists.isNotEmpty()) {
                Text(
                    text     = artists,
                    style    = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            }
        }
        IconButton(
            onClick = {
                if (isPlaying) playerConnection.player.pause()
                else           playerConnection.player.play()
            },
        ) {
            Icon(
                imageVector        = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = null,
            )
        }
        IconButton(onClick = onClose) {
            Icon(imageVector = Icons.Rounded.Close, contentDescription = null)
        }
    }
}
