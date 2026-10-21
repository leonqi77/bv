package dev.aaa1115910.bv.util

fun String.resizedImageUrl(size: ImageSize): String {
    return when (size) {
        ImageSize.Default -> this
        else -> "$this@${size.sizeString}.webp"
    }
}

enum class ImageSize(val sizeString: String) {
    Default(""),
    Cover("180h_288w_1c"),
    SmallVideoCardCover("400h_640w_1c"),
    FeaturedHeaderCover("540h_960w_1c"),
    Avatar("96h_96w_1c"),
    SeasonCoverThumbnail("466h_622w"),
    /** 剧集页背景大图，绘制尺寸约 1382×842 像素 */
    SeasonBackdrop("720h_1280w_1c"),
    /** 剧集页模糊海报背景：取极小尺寸再放大，靠双线性插值得到模糊效果，无需运行时模糊 */
    SeasonBackdropBlurred("64h_48w_1c")
}