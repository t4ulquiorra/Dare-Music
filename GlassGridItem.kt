fun GlassGridItem(
    item: YTItem,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
) {
    val database = LocalDatabase.current
    val gridHeight = currentGridThumbnailHeight()
    val cardSize = gridHeight * 0.92f
    val cornerRadius = 16.dp

    // Liked / bookmarked state
    val song by produceState<Song?>(initialValue = null, item.id) {
        if (item is AlbumItem || item is SongItem) {
            // no-op for non-song, handled below
        }
        value = null
    }
    val album by produceState<Album?>(initialValue = null, item.id) {
        if (item is AlbumItem) value = database.album(item.id).firstOrNull()
    }
    val isLiked = album?.album?.bookmarkedAt != null

    val subtitle = when (item) {
        is AlbumItem    -> item.year?.toString()
        is PlaylistItem -> item.author?.name
        else            -> null
    }
    val artistLine = when (item) {
        is AlbumItem    -> item.artists?.joinToString { it.name }
        is PlaylistItem -> item.author?.name
        else            -> null
    }

    Box(
        modifier = modifier
            .padding(6.dp)
            .size(cardSize)
            .clip(RoundedCornerShape(cornerRadius))
            .border(
                width = 0.8.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.22f),
                        Color.White.copy(alpha = 0.05f),
                    )
                ),
                shape = RoundedCornerShape(cornerRadius),
            ),
    ) {
        // Full bleed thumbnail
        AsyncImage(
            model = item.thumbnail,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

        // Hard gradient overlay — bottom 55% of card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxSize(0.65f)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Transparent,
                            0.45f to Color(0xFF0A0A0A).copy(alpha = 0.85f),
                            1.0f to Color(0xFF000000),
                        )
                    )
                )
        )

        // Text + badges — inside card, bottom-left
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 10.dp, end = 48.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text       = item.title,
                color      = Color.White,
                style      = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis,
            )
            if (artistLine != null) {
                Text(
                    text     = artistLine,
                    color    = Color.White.copy(alpha = 0.75f),
                    style    = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                // Heart
                Icon(
                    painter = painterResource(
                        if (isLiked) R.drawable.favorite else R.drawable.favorite_border
                    ),
                    contentDescription = null,
                    tint     = if (isLiked) Color(0xFFFF4D6D) else Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(13.dp),
                )
                // Explicit badge
                if (item.explicit) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color.White.copy(alpha = 0.25f))
                            .padding(horizontal = 3.dp, vertical = 1.dp),
                    ) {
                        Text(
                            text  = "E",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                // Year or song count
                if (subtitle != null) {
                    Text(
                        text  = subtitle,
                        color = Color.White.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }

        // Play button — bottom-right inside card
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp)
                .size(34.dp)
                .clip(CircleShape)
                .background(Color(0xFF1A1A1A).copy(alpha = 0.85f))
                .border(0.6.dp, Color.White.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.play),
                contentDescription = null,
                tint     = Color.White,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}
