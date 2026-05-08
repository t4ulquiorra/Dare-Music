/**
 * Dare Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.dare.music.viewmodels

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dare.innertube.YouTube
import com.dare.innertube.models.filterExplicit
import com.dare.innertube.models.filterVideoSongs
import com.dare.innertube.models.filterYoutubeShorts
import com.dare.innertube.pages.ArtistPage
import com.dare.music.constants.HideExplicitKey
import com.dare.music.constants.HideVideoSongsKey
import com.dare.music.constants.HideYoutubeShortsKey
import com.dare.music.db.MusicDatabase
import com.dare.music.db.entities.ArtistEntity
import com.dare.music.extensions.filterExplicit
import com.dare.music.extensions.filterExplicitAlbums
import com.dare.music.utils.SyncUtils
import com.dare.music.utils.dataStore
import com.dare.music.utils.get
import com.dare.music.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import com.dare.music.extensions.filterVideoSongs as filterVideoSongsLocal

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ArtistViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: MusicDatabase,
    private val syncUtils: SyncUtils,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val artistId = savedStateHandle.get<String>("artistId")!!
    private val isPodcastChannel = savedStateHandle.get<Boolean>("isPodcastChannel") ?: false
    var artistPage by mutableStateOf<ArtistPage?>(null)

    // Track API subscription state separately
    private val _apiSubscribed = MutableStateFlow<Boolean?>(null)

    val libraryArtist = database.artist(artistId)
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    // Combine API state with local database state - local takes precedence when not logged in
    val isChannelSubscribed = kotlinx.coroutines.flow.combine(
        _apiSubscribed,
        database.artist(artistId),
    ) { apiState, localArtist ->
        val locallyBookmarked = localArtist?.artist?.bookmarkedAt != null
        locallyBookmarked || (apiState == true)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val librarySongs = context.dataStore.data
        .map { (it[HideExplicitKey] ?: false) to (it[HideVideoSongsKey] ?: false) }
        .distinctUntilChanged()
        .flatMapLatest { (hideExplicit, hideVideoSongs) ->
            database.artistSongsPreview(artistId).map { it.filterExplicit(hideExplicit).filterVideoSongsLocal(hideVideoSongs) }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val libraryAlbums = context.dataStore.data
        .map { it[HideExplicitKey] ?: false }
        .distinctUntilChanged()
        .flatMapLatest { hideExplicit ->
            database.artistAlbumsPreview(artistId).map { it.filterExplicitAlbums(hideExplicit) }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        // Load artist page and reload when hide explicit setting changes
        viewModelScope.launch {
            context.dataStore.data
                .map {
                    Triple(
                        it[HideExplicitKey] ?: false,
                        it[HideVideoSongsKey] ?: false,
                        it[HideYoutubeShortsKey] ?: false
                    )
                }
                .distinctUntilChanged()
                .collect {
                    fetchArtistsFromYTM()
                }
        }
    }

    fun fetchArtistsFromYTM() {
        viewModelScope.launch {
            val hideExplicit = context.dataStore.get(HideExplicitKey, false)
            val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)
            val hideYoutubeShorts = context.dataStore.get(HideYoutubeShortsKey, false)
            YouTube.artist(artistId)
                .onSuccess { page ->
                    val filteredSections = page.sections
                        .map { section ->
                            section.copy(items = section.items.filterExplicit(hideExplicit).filterVideoSongs(hideVideoSongs).filterYoutubeShorts(hideYoutubeShorts))
                        }
                        .filter { section -> section.items.isNotEmpty() }

                    artistPage = page.copy(sections = filteredSections)
                    // Store API subscription state
                    _apiSubscribed.value = page.isSubscribed
                }.onFailure {
                    reportException(it)
                }
        }
    }

    fun toggleChannelSubscription() {
    val channelId = artistPage?.artist?.channelId ?: artistId
    val isCurrentlySubscribed = isChannelSubscribed.value
    val shouldBeSubscribed = !isCurrentlySubscribed

    _apiSubscribed.value = shouldBeSubscribed

    viewModelScope.launch(Dispatchers.IO) {
        val artist = libraryArtist.value?.artist
        if (artist != null) {
            val newBookmark = if (shouldBeSubscribed) {
                artist.bookmarkedAt ?: java.time.LocalDateTime.now()
            } else null
            database.update(
                artist.copy(
                    bookmarkedAt = newBookmark,
                    isPodcastChannel = if (shouldBeSubscribed && isPodcastChannel) true else artist.isPodcastChannel,
                )
            )
        } else if (shouldBeSubscribed) {
            // Always insert — even if artistPage hasn't loaded yet
            database.insert(
                ArtistEntity(
                    id             = artistId,
                    name           = artistPage?.artist?.title ?: artistId,
                    channelId      = artistPage?.artist?.channelId,
                    thumbnailUrl   = artistPage?.artist?.thumbnail,
                    bookmarkedAt   = java.time.LocalDateTime.now(),
                    isPodcastChannel = isPodcastChannel,
                )
            )
        }
        syncUtils.subscribeChannel(channelId, shouldBeSubscribed)
    }
}
