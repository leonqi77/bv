package dev.aaa1115910.biliapi.entity.video

data class Dimension(
    val width: Int,
    val height: Int,
    val isVertical: Boolean = width < height
) {
    private val longSide get() = maxOf(width, height)

    val is4K get() = longSide >= 3840

    /**
     * 根据上传视频的分辨率估计的最高清晰度，实际可用清晰度以播放地址为准
     */
    val qualityLabel
        get() = when {
            longSide >= 3840 -> "4K"
            longSide >= 2560 -> "2K"
            longSide >= 1920 -> "1080P"
            longSide >= 1280 -> "720P"
            longSide >= 854 -> "480P"
            longSide > 0 -> "360P"
            else -> ""
        }

    companion object {
        fun fromDimension(dimension: bilibili.app.archive.v1.Dimension) = Dimension(
            width = dimension.width.toInt(),
            height = dimension.height.toInt()
        )

        fun fromDimension(dimension: dev.aaa1115910.biliapi.http.entity.video.Dimension) =
            Dimension(
                width = dimension.width,
                height = dimension.height
            )
    }
}
