package org.cycb.canvas.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ChatFolder(
    val id: String,
    val name: String,
    val chatIds: Set<String>,
    val icon: String? = null
)
