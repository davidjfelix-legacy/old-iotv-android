package tv.mg4.app.api.responses


data class Video(
        var id: String?
)

data class VideoAttribues(
        var type: VideoType,
        var isPublic: Boolean,
        var isUploaded: Boolean?,
        var isBuffered: Boolean?
)

enum class VideoType(repr: String) {
    LIVE("LIVE"),
    VOD("VOD")
}