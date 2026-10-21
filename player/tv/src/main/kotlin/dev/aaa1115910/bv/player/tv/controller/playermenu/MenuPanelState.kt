package dev.aaa1115910.bv.player.tv.controller.playermenu

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.aaa1115910.bv.player.entity.VideoPlayerClosedCaptionMenuItem
import dev.aaa1115910.bv.player.entity.VideoPlayerDanmakuMenuItem
import dev.aaa1115910.bv.player.entity.VideoPlayerMenuNavItem
import dev.aaa1115910.bv.player.entity.VideoPlayerOthersMenuItem
import dev.aaa1115910.bv.player.entity.VideoPlayerPictureMenuItem

/** One screen of choices (radio list, check list or stepper) reached from a settings row. */
sealed interface MenuLeaf {
    data class Picture(val item: VideoPlayerPictureMenuItem) : MenuLeaf
    data class Danmaku(val item: VideoPlayerDanmakuMenuItem) : MenuLeaf
    data class Caption(val item: VideoPlayerClosedCaptionMenuItem) : MenuLeaf
    data class Others(val item: VideoPlayerOthersMenuItem) : MenuLeaf
}

sealed interface MenuPage {
    /** The top list: five picture rows, then the danmaku, subtitle and other groups. */
    data object Root : MenuPage

    /** The rows of one group (Danmaku, ClosedCaption or Others). */
    data class Group(val nav: VideoPlayerMenuNavItem) : MenuPage

    data class Leaf(val leaf: MenuLeaf) : MenuPage
}

/**
 * Navigation of the settings panel. Hoisted so the player can send Back here before closing the panel:
 * [back] steps up one level and returns false only on the top list.
 */
@Stable
class MenuPanelState {
    var page by mutableStateOf<MenuPage>(MenuPage.Root)
        private set

    /** Index of the row to focus when the top list or a group list is shown again. */
    var rootIndex by mutableIntStateOf(0)
    var groupIndex by mutableIntStateOf(0)

    /**
     * A targeted open shows that item's options directly. Without a picture item, a group nav shows
     * the options of its first item (subtitles open on the subtitle choice); no target shows the list.
     */
    fun open(nav: VideoPlayerMenuNavItem?, pictureItem: VideoPlayerPictureMenuItem?) {
        rootIndex = 0
        groupIndex = 0
        page = when {
            pictureItem != null -> {
                rootIndex = pictureItem.ordinal
                MenuPage.Leaf(MenuLeaf.Picture(pictureItem))
            }

            nav == VideoPlayerMenuNavItem.Danmaku ->
                MenuPage.Leaf(MenuLeaf.Danmaku(VideoPlayerDanmakuMenuItem.entries.first()))

            nav == VideoPlayerMenuNavItem.ClosedCaption ->
                MenuPage.Leaf(MenuLeaf.Caption(VideoPlayerClosedCaptionMenuItem.entries.first()))

            nav == VideoPlayerMenuNavItem.Others ->
                MenuPage.Leaf(MenuLeaf.Others(VideoPlayerOthersMenuItem.entries.first()))

            else -> MenuPage.Root
        }
        if (nav != null && pictureItem == null && nav != VideoPlayerMenuNavItem.Picture) {
            rootIndex = PICTURE_ROWS + nav.groupOrdinal()
        }
    }

    fun show(page: MenuPage) {
        this.page = page
    }

    fun back(): Boolean {
        when (val current = page) {
            MenuPage.Root -> return false
            is MenuPage.Group -> page = MenuPage.Root
            is MenuPage.Leaf -> when (val leaf = current.leaf) {
                is MenuLeaf.Picture -> page = MenuPage.Root
                is MenuLeaf.Danmaku -> {
                    groupIndex = leaf.item.ordinal
                    page = MenuPage.Group(VideoPlayerMenuNavItem.Danmaku)
                }

                is MenuLeaf.Caption -> {
                    groupIndex = leaf.item.ordinal
                    page = MenuPage.Group(VideoPlayerMenuNavItem.ClosedCaption)
                }

                is MenuLeaf.Others -> {
                    groupIndex = leaf.item.ordinal
                    page = MenuPage.Group(VideoPlayerMenuNavItem.Others)
                }
            }
        }
        return true
    }

    companion object {
        val PICTURE_ROWS = VideoPlayerPictureMenuItem.entries.size

        /** Groups in row order below the divider. */
        val groups = listOf(
            VideoPlayerMenuNavItem.Danmaku,
            VideoPlayerMenuNavItem.ClosedCaption,
            VideoPlayerMenuNavItem.Others
        )

        private fun VideoPlayerMenuNavItem.groupOrdinal() = groups.indexOf(this).coerceAtLeast(0)
    }
}
