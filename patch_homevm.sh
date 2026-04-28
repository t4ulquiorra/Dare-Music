#!/bin/bash
# Run this from ~/Dare to patch HomeViewModel.kt with home caching
# Usage: bash patch_homevm.sh

VM="app/src/main/kotlin/com/dare/music/viewmodels/HomeViewModel.kt"

# ── 1. Add serialization imports after the last kotlin import ─────────────────
# Find the line with "import kotlin.random.Random" and add after it
python3 - <<'PYEOF'
import re

path = "app/src/main/kotlin/com/dare/music/viewmodels/HomeViewModel.kt"
with open(path, "r") as f:
    content = f.read()

new_imports = """import kotlin.random.Random
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json"""

content = content.replace("import kotlin.random.Random", new_imports, 1)

# ── 2. Add cache data classes after CommunityPlaylistItem ─────────────────────
cache_classes = """
data class CommunityPlaylistItem(
    val playlist: PlaylistItem,
    val songs: List<SongItem>
)

@Serializable
data class CachedHomeAlbum(
    val browseId: String,
    val playlistId: String,
    val title: String,
    val artistNames: List<String>,
    val year: Int?,
    val thumbnail: String,
    val explicit: Boolean,
) {
    fun toAlbumItem() = AlbumItem(
        browseId  = browseId,
        playlistId = playlistId,
        title     = title,
        artists   = artistNames.map { com.dare.innertube.models.Artist(name = it, id = null) },
        year      = year,
        thumbnail = thumbnail,
        explicit  = explicit,
    )
    companion object {
        fun from(item: AlbumItem) = CachedHomeAlbum(
            browseId    = item.browseId,
            playlistId  = item.playlistId,
            title       = item.title,
            artistNames = item.artists?.map { it.name } ?: emptyList(),
            year        = item.year,
            thumbnail   = item.thumbnail,
            explicit    = item.explicit,
        )
    }
}

@Serializable
data class CachedHomeArtist(
    val id: String,
    val title: String,
    val thumbnail: String?,
    val channelId: String?,
) {
    fun toArtistItem() = ArtistItem(
        id              = id,
        title           = title,
        thumbnail       = thumbnail,
        channelId       = channelId,
        shuffleEndpoint = null,
        radioEndpoint   = null,
    )
    companion object {
        fun from(item: ArtistItem) = CachedHomeArtist(
            id        = item.id,
            title     = item.title,
            thumbnail = item.thumbnail,
            channelId = item.channelId,
        )
    }
}

@Serializable
data class CachedHomePlaylist(
    val id: String,
    val title: String,
    val authorName: String?,
    val thumbnail: String?,
) {
    fun toPlaylistItem() = PlaylistItem(
        id              = id,
        title           = title,
        author          = authorName?.let { com.dare.innertube.models.Artist(name = it, id = null) },
        songCountText   = null,
        thumbnail       = thumbnail,
        playEndpoint    = null,
        shuffleEndpoint = null,
        radioEndpoint   = null,
    )
    companion object {
        fun from(item: PlaylistItem) = CachedHomePlaylist(
            id         = item.id,
            title      = item.title,
            authorName = item.author?.name,
            thumbnail  = item.thumbnail,
        )
    }
}"""

# Replace the CommunityPlaylistItem block (without the one we're appending after)
old_block = """data class CommunityPlaylistItem(
    val playlist: PlaylistItem,
    val songs: List<SongItem>
)"""
content = content.replace(old_block, cache_classes, 1)

# ── 3. Add cache keys + functions before init { ───────────────────────────────
cache_members = """    // ── Home cache keys ──────────────────────────────────────────────────────
    private val CACHE_QUICKPICK_IDS = stringPreferencesKey("home_cache_quickpick_ids")
    private val CACHE_ALBUMS        = stringPreferencesKey("home_cache_albums")
    private val CACHE_ARTISTS       = stringPreferencesKey("home_cache_artists")
    private val CACHE_PLAYLISTS     = stringPreferencesKey("home_cache_playlists")
    private val homeJson            = Json { ignoreUnknownKeys = true; encodeDefaults = false }

    private suspend fun loadCache() {
        try {
            val prefs = context.dataStore.data.first()
            prefs[CACHE_QUICKPICK_IDS]?.let { idsJson ->
                val ids = homeJson.decodeFromString<List<String>>(idsJson)
                if (ids.isNotEmpty() && quickPicks.value == null) {
                    val songs = database.getSongsByIds(ids)
                    if (songs.isNotEmpty()) quickPicks.value = songs
                }
            }
            prefs[CACHE_ALBUMS]?.let { json ->
                val cached = homeJson.decodeFromString<List<CachedHomeAlbum>>(json)
                if (cached.isNotEmpty() && relatedAlbums.value.isEmpty())
                    relatedAlbums.value = cached.map { it.toAlbumItem() }
            }
            prefs[CACHE_ARTISTS]?.let { json ->
                val cached = homeJson.decodeFromString<List<CachedHomeArtist>>(json)
                if (cached.isNotEmpty() && similarArtists.value.isEmpty())
                    similarArtists.value = cached.map { it.toArtistItem() }
            }
            prefs[CACHE_PLAYLISTS]?.let { json ->
                val cached = homeJson.decodeFromString<List<CachedHomePlaylist>>(json)
                if (cached.isNotEmpty() && recommendedPlaylists.value.isEmpty())
                    recommendedPlaylists.value = cached.map { it.toPlaylistItem() }
            }
        } catch (e: Exception) {
            // Non-fatal — fresh fetch will populate flows
        }
    }

    private suspend fun saveCache() {
        try {
            context.dataStore.edit { prefs ->
                quickPicks.value?.takeIf { it.isNotEmpty() }?.let { songs ->
                    prefs[CACHE_QUICKPICK_IDS] = homeJson.encodeToString(songs.map { it.id })
                }
                relatedAlbums.value.takeIf { it.isNotEmpty() }?.let { albums ->
                    prefs[CACHE_ALBUMS] = homeJson.encodeToString(albums.map { CachedHomeAlbum.from(it) })
                }
                similarArtists.value.takeIf { it.isNotEmpty() }?.let { artists ->
                    prefs[CACHE_ARTISTS] = homeJson.encodeToString(artists.map { CachedHomeArtist.from(it) })
                }
                recommendedPlaylists.value.takeIf { it.isNotEmpty() }?.let { playlists ->
                    prefs[CACHE_PLAYLISTS] = homeJson.encodeToString(playlists.map { CachedHomePlaylist.from(it) })
                }
            }
        } catch (e: Exception) {
            // Non-fatal
        }
    }

    init {"""

content = content.replace("    init {", cache_members, 1)

# ── 4. Call saveCache() after load() in init ──────────────────────────────────
# Find the pattern inside the first viewModelScope.launch in init and add loadCache + saveCache
old_init_launch = """    viewModelScope.launch(Dispatchers.IO) {
            context.dataStore.data
                .map { it[InnerTubeCookieKey] }
                .distinctUntilChanged()
                .first()

            load()
        }"""

new_init_launch = """    viewModelScope.launch(Dispatchers.IO) {
            loadCache() // Show cached data immediately while network fetches fresh
            context.dataStore.data
                .map { it[InnerTubeCookieKey] }
                .distinctUntilChanged()
                .first()

            load()
            saveCache() // Persist fresh data for next session
        }"""

content = content.replace(old_init_launch, new_init_launch, 1)

with open(path, "w") as f:
    f.write(content)

print("✅ HomeViewModel.kt patched successfully")
PYEOF
