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
    SeasonCoverThumbnail("466h_622w")
}