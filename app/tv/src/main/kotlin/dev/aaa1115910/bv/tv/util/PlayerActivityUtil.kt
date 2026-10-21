package dev.aaa1115910.bv.tv.util

import android.content.Context
import dev.aaa1115910.bv.entity.proxy.ProxyArea
import dev.aaa1115910.bv.tv.activities.video.RemoteControllerPanelDemoActivity
import dev.aaa1115910.bv.tv.activities.video.VideoPlayerV3Activity
import dev.aaa1115910.bv.util.Prefs

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
    if (Prefs.showedRemoteControllerPanelDemo) {
        VideoPlayerV3Activity.actionStart(
            context, avid, cid, title, partTitle, played, fromSeason, subType, epid, seasonId,
            isVerticalVideo, proxyArea, playerIconIdle, playerIconMoving,
            upName, upFace, upFollowed
        )
    } else {
        RemoteControllerPanelDemoActivity.actionStart(
            context, avid, cid, title, partTitle, played, fromSeason, subType, epid, seasonId,
            isVerticalVideo, proxyArea, playerIconIdle, playerIconMoving,
            upName, upFace, upFollowed
        )
    }
}