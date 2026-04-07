package com.dare.innertube.models.body

import com.dare.innertube.models.Context
import com.dare.innertube.models.Continuation
import kotlinx.serialization.Serializable

@Serializable
data class BrowseBody(
    val context: Context,
    val browseId: String?,
    val params: String?,
    val continuation: String?
)
