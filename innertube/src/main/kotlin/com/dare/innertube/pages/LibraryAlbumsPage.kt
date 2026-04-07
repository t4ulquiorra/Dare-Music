package com.dare.innertube.pages

import com.dare.innertube.models.Album
import com.dare.innertube.models.AlbumItem
import com.dare.innertube.models.Artist
import com.dare.innertube.models.ArtistItem
import com.dare.innertube.models.MusicResponsiveListItemRenderer
import com.dare.innertube.models.MusicTwoRowItemRenderer
import com.dare.innertube.models.PlaylistItem
import com.dare.innertube.models.SongItem
import com.dare.innertube.models.YTItem
import com.dare.innertube.models.oddElements
import com.dare.innertube.utils.parseTime

data class LibraryAlbumsPage(
    val albums: List<AlbumItem>,
    val continuation: String?,
) {
    companion object {
        fun fromMusicTwoRowItemRenderer(renderer: MusicTwoRowItemRenderer): AlbumItem? {
            return AlbumItem(
                        browseId = renderer.navigationEndpoint.browseEndpoint?.browseId ?: return null,
                        playlistId = renderer.thumbnailOverlay?.musicItemThumbnailOverlayRenderer?.content
                            ?.musicPlayButtonRenderer?.playNavigationEndpoint
                            ?.watchPlaylistEndpoint?.playlistId ?: return null,
                        title = renderer.title.runs?.firstOrNull()?.text ?: return null,
                        artists = null,
                        year = renderer.subtitle?.runs?.lastOrNull()?.text?.toIntOrNull(),
                        thumbnail = renderer.thumbnailRenderer.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                        explicit = renderer.subtitleBadges?.find {
                            it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                        } != null
                    )
        }
    }
}
