package io.iotv.app.api.responses


data class Video(
        var id: String?
)

data class VideoAttribues(
        var type: VideoType,
        var isPublic: Boolean,
        var isUploaded: Boolean?,
        var isBuffered: Boolean?
)

enum class VideoType(val repr: String) {
    LIVE("LIVE"),
    VOD("VOD")
}