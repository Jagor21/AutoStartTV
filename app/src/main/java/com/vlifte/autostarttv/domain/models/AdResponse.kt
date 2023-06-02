package com.sgvdev.autostart.models

data class AdResponse(
    val success: Success
)

data class Success(
    val checksum: String?,
    val content: Content?
)

data class Content(
    val advertisement: Advertisement?,
    val announcement: Any,
    val id: Int,
    val options: Options
)

data class Advertisement(
    val content: List<ContentX>,
    val id: Int
)

data class Options(
    val announcement_placement: String,
    val announcement_size: String
)

data class ContentX(
    val duration: String,
    val `file`: File
)

data class File(
    val name: String,
    val url: String
)