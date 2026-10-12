package com.sirmrm.messageapp.domain.model

/** A folder created by the user, in addition to the built-ins. */
data class UserFolder(
    val id: Long,
    val name: String,
    val createdAt: Long,
)
