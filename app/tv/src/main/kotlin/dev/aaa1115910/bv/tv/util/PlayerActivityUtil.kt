package dev.aaa1115910.bv.tv.util

import android.content.Context
import dev.aaa1115910.bv.entity.proxy.ProxyArea
import dev.aaa1115910.bv.tv.activities.video.RemoteControllerPanelDemoActivity
import dev.aaa1115910.bv.tv.activities.video.VideoPlayerV3Activity
import dev.aaa1115910.bv.util.ImageSize
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.resizedImageUrl

fun launchPlayerActivity(
    context: Context,
    avid: Long,
    cid: Long,
    title: String,
    partTitle: String,
    played: Int,
    fromSeason: Boolean,
    subType: Int? = null,
    epid: Int? = null,
    seasonId: Int? = null,
    isVerticalVideo: Boolean = false,
    proxyArea: ProxyArea = ProxyArea.MainLand,
    playerIconIdle: String = "",
    playerIconMoving: String = "",
    upName: String = "",
    upFace: String = "",
    upFollowed: Boolean = false
) {
    // The player shows the face at avatar size: use the same resized URL the cards cache.
    val avatarUrl = upFace.takeIf { it.isNotEmpty() }?.resizedImageUrl(ImageSize.Avatar).orEmpty()
    if (Prefs.showedRemoteControllerPanelDemo) {
        VideoPlayerV3Activity.actionStart(
            context, avid, cid, title, partTitle, played, fromSeason, subType, epid, seasonId,
            isVerticalVideo, proxyArea, playerIconIdle, playerIconMoving,
            upName, avatarUrl, upFollowed
        )
    } else {
        RemoteControllerPanelDemoActivity.actionStart(
            context, avid, cid, title, partTitle, played, fromSeason, subType, epid, seasonId,
            isVerticalVideo, proxyArea, playerIconIdle, playerIconMoving,
            upName, avatarUrl, upFollowed
        )
    }
}