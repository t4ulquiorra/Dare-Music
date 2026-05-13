package com.dare.music.ui.component

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.dare.music.LocalGlassBackdrop
import com.dare.music.LocalPlayerConnection
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.effect

@Composable
fun DareMiniPlayer(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onClose: () -> Unit = {},
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val backdrop         = LocalGlassBackdrop.current
    val currentSong      by playerConnection.mediaMetadata.collectAsState()
    val isPlaying        by playerConnection.isPlaying.collectAsState()

    if (currentSong == null) return

    val shape = RoundedCornerShape(12.dp)

    Row(
        modifier = modifier
            .clip(shape)
            .then(
                backdrop?.let { bd ->
                    Modifier.drawBackdrop(
                        backdrop      = bd,
                        shape         = { shape },
                        effects       = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                effect(
                                    RenderEffect.createBlurEffect(40f, 40f, Shader.TileMode.CLAMP)
                                )
                            }
                        },
                        onDrawSurface = { drawRect(Color.Black.copy(alpha = 0.45f)) },
                    )
                } ?: Modifier
            )
            .border(1.dp, Color.White.copy(alpha = 0.18f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment     = Alignment.CenterVertically,
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
                color    = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val artists = currentSong?.artists?.joinToString { it.name }.orEmpty()
            if (artists.isNotEmpty()) {
                Text(
                    text     = artists,
                    style    = MaterialTheme.typography.bodySmall,
                    color    = Color.White.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
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
                tint               = Color.White,
            )
        }
        IconButton(onClick = onClose) {
            Icon(
                imageVector        = Icons.Rounded.Close,
                contentDescription = null,
                tint               = Color.White,
            )
        }
    }
}
