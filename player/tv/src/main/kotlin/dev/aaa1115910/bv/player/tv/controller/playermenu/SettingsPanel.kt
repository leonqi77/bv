package dev.aaa1115910.bv.player.tv.controller.playermenu

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import dev.aaa1115910.biliapi.entity.video.Subtitle
import dev.aaa1115910.bv.player.entity.Audio
import dev.aaa1115910.bv.player.entity.DanmakuType
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerConfigData
import dev.aaa1115910.bv.player.entity.PlayMode
import dev.aaa1115910.bv.player.entity.Resolution
import dev.aaa1115910.bv.player.entity.VideoAspectRatio
import dev.aaa1115910.bv.player.entity.VideoCodec
import dev.aaa1115910.bv.player.entity.VideoPlayerClosedCaptionMenuItem
import dev.aaa1115910.bv.player.entity.VideoPlayerConfigData
import dev.aaa1115910.bv.player.entity.VideoPlayerDanmakuMenuItem
import dev.aaa1115910.bv.player.entity.VideoPlayerMenuNavItem
import dev.aaa1115910.bv.player.entity.VideoPlayerOthersMenuItem
import dev.aaa1115910.bv.player.entity.VideoPlayerPictureMenuItem
import dev.aaa1115910.bv.player.tv.PlayerColors
import dev.aaa1115910.bv.util.requestFocus
import kotlinx.coroutines.delay
import java.text.NumberFormat
import kotlin.math.roundToInt

/** Every setting the panel can change. */
class MenuActions(
    val onResolutionChange: (Resolution) -> Unit,
    val onCodecChange: (VideoCodec) -> Unit,
    val onAspectRatioChange: (VideoAspectRatio) -> Unit,
    val onPlaySpeedChange: (Float) -> Unit,
    val onAudioChange: (Audio) -> Unit,
    val onDanmakuSwitchChange: (List<DanmakuType>) -> Unit,
    val onDanmakuSizeChange: (Float) -> Unit,
    val onDanmakuOpacityChange: (Float) -> Unit,
    val onDanmakuAreaChange: (Float) -> Unit,
    val onDanmakuMaskChange: (Boolean) -> Unit,
    val onSubtitleChange: (Subtitle) -> Unit,
    val onSubtitleSizeChange: (TextUnit) -> Unit,
    val onSubtitleBackgroundOpacityChange: (Float) -> Unit,
    val onSubtitleBottomPadding: (Dp) -> Unit,
    val onPlayModeChange: (PlayMode) -> Unit
)

private class PanelItem(
    val label: String,
    val value: String? = null,
    val selected: Boolean = false,
    val chevron: Boolean = false,
    val dividerBefore: Boolean = false,
    val onClick: () -> Unit
)

private val RowTextColor = Color(0xFFDDDDDD)
private val DividerColor = Color(0x1FFFFFFF)

/** The panel body: the top list, a group list, or the choices of one setting. */
@Composable
fun SettingsPanel(
    modifier: Modifier = Modifier,
    state: MenuPanelState,
    actions: MenuActions
) {
    val context = LocalContext.current
    val config = LocalVideoPlayerConfigData.current

    Column(modifier = modifier) {
        key(state.page) {
            when (val page = state.page) {
                MenuPage.Root -> PanelList(
                    title = "播放设置",
                    items = rootItems(context, config, state),
                    initialIndex = state.rootIndex
                )

                is MenuPage.Group -> PanelList(
                    title = page.nav.getDisplayName(context),
                    items = groupItems(context, config, state, page.nav),
                    initialIndex = state.groupIndex
                )

                is MenuPage.Leaf -> LeafPage(context, config, state, actions, page.leaf)
            }
        }
    }
}

private fun pictureName(context: Context, item: VideoPlayerPictureMenuItem) =
    if (item == VideoPlayerPictureMenuItem.Audio) "音质" else item.getDisplayName(context)

private fun speedText(speed: Float) = "${(speed * 100).roundToInt() / 100f}x"

private fun percent(value: Float) = NumberFormat.getPercentInstance()
    .apply { maximumFractionDigits = 0 }
    .format(value)

private fun pictureValue(context: Context, config: VideoPlayerConfigData, item: VideoPlayerPictureMenuItem) =
    when (item) {
        VideoPlayerPictureMenuItem.Resolution -> config.currentResolution.getShortDisplayName(context)
        VideoPlayerPictureMenuItem.Codec -> config.currentVideoCodec.getDisplayName(context)
        VideoPlayerPictureMenuItem.AspectRatio -> config.currentVideoAspectRatio.getDisplayName(context)
        VideoPlayerPictureMenuItem.PlaySpeed -> speedText(config.currentVideoSpeed)
        VideoPlayerPictureMenuItem.Audio -> config.currentAudio.getDisplayName(context)
    }

private fun rootItems(context: Context, config: VideoPlayerConfigData, state: MenuPanelState): List<PanelItem> {
    val pictureRows = VideoPlayerPictureMenuItem.entries.mapIndexed { index, item ->
        PanelItem(
            label = pictureName(context, item),
            value = pictureValue(context, config, item)
        ) {
            state.rootIndex = index
            state.show(MenuPage.Leaf(MenuLeaf.Picture(item)))
        }
    }
    val groupRows = MenuPanelState.groups.mapIndexed { index, nav ->
        PanelItem(
            label = nav.getDisplayName(context),
            chevron = true,
            dividerBefore = index == 0
        ) {
            state.rootIndex = MenuPanelState.PICTURE_ROWS + index
            state.groupIndex = 0
            state.show(MenuPage.Group(nav))
        }
    }
    return pictureRows + groupRows
}

private fun groupItems(
    context: Context,
    config: VideoPlayerConfigData,
    state: MenuPanelState,
    nav: VideoPlayerMenuNavItem
): List<PanelItem> = when (nav) {
    VideoPlayerMenuNavItem.Danmaku -> VideoPlayerDanmakuMenuItem.entries.mapIndexed { index, item ->
        val value = when (item) {
            VideoPlayerDanmakuMenuItem.Switch -> {
                val enabled = config.currentDanmakuEnabledList
                if (enabled.contains(DanmakuType.All)) DanmakuType.All.getDisplayName(context)
                else if (enabled.isEmpty()) "关闭"
                else "${enabled.size}项"
            }

            VideoPlayerDanmakuMenuItem.Size -> percent(config.currentDanmakuScale)
            VideoPlayerDanmakuMenuItem.Opacity -> percent(config.currentDanmakuOpacity)
            VideoPlayerDanmakuMenuItem.Area -> percent(config.currentDanmakuArea)
            VideoPlayerDanmakuMenuItem.Mask -> if (config.currentDanmakuMask) "开启" else "关闭"
        }
        PanelItem(label = item.getDisplayName(context), value = value) {
            state.groupIndex = index
            state.show(MenuPage.Leaf(MenuLeaf.Danmaku(item)))
        }
    }

    VideoPlayerMenuNavItem.ClosedCaption -> VideoPlayerClosedCaptionMenuItem.entries.mapIndexed { index, item ->
        val value = when (item) {
            VideoPlayerClosedCaptionMenuItem.Switch ->
                config.availableSubtitleTracks.firstOrNull { it.id == config.currentSubtitleId }?.langDoc
                    ?: "关闭"

            VideoPlayerClosedCaptionMenuItem.Size -> "${config.currentSubtitleFontSize.value.toInt()} SP"
            VideoPlayerClosedCaptionMenuItem.Opacity -> percent(config.currentSubtitleBackgroundOpacity)
            VideoPlayerClosedCaptionMenuItem.Padding -> "${config.currentSubtitleBottomPadding.value.toInt()} DP"
        }
        PanelItem(label = item.getDisplayName(context), value = value) {
            state.groupIndex = index
            state.show(MenuPage.Leaf(MenuLeaf.Caption(item)))
        }
    }

    VideoPlayerMenuNavItem.Others -> VideoPlayerOthersMenuItem.entries.mapIndexed { index, item ->
        PanelItem(
            label = item.getDisplayName(context),
            value = config.currentPlayMode.getDisplayName(context)
        ) {
            state.groupIndex = index
            state.show(MenuPage.Leaf(MenuLeaf.Others(item)))
        }
    }

    VideoPlayerMenuNavItem.Picture -> emptyList()
}

@Composable
private fun LeafPage(
    context: Context,
    config: VideoPlayerConfigData,
    state: MenuPanelState,
    actions: MenuActions,
    leaf: MenuLeaf
) {
    val done = { state.back(); Unit }

    @Composable
    fun <T> Radio(title: String, options: List<T>, current: T, name: (T) -> String, onPick: (T) -> Unit) {
        PanelList(
            title = title,
            items = options.map { option ->
                PanelItem(label = name(option), selected = option == current) { onPick(option) }
            },
            initialIndex = options.indexOf(current).coerceAtLeast(0),
            checkSelected = true
        )
    }

    @Composable
    fun Stepper(title: String, value: Float, range: ClosedFloatingPointRange<Float>, step: Float, text: String, onChange: (Float) -> Unit) {
        PanelStepper(
            title = title,
            valueText = text,
            fraction = (value - range.start) / (range.endInclusive - range.start),
            onStep = { direction ->
                val next = ((value + direction * step) * 100).roundToInt() / 100f
                onChange(next.coerceIn(range.start, range.endInclusive))
            },
            onDone = done
        )
    }

    @Composable
    fun IntStepper(title: String, value: Int, range: IntRange, text: String, onChange: (Int) -> Unit) {
        PanelStepper(
            title = title,
            valueText = text,
            fraction = (value - range.first).toFloat() / (range.last - range.first),
            onStep = { direction -> onChange((value + direction).coerceIn(range)) },
            onDone = done
        )
    }

    when (leaf) {
        is MenuLeaf.Picture -> {
            val title = pictureName(context, leaf.item)
            when (leaf.item) {
                VideoPlayerPictureMenuItem.Resolution -> Radio(
                    title,
                    config.availableResolutions.sortedByDescending { it.code },
                    config.currentResolution,
                    { it.getShortDisplayName(context) },
                    actions.onResolutionChange
                )

                VideoPlayerPictureMenuItem.Codec -> Radio(
                    title, config.availableVideoCodec, config.currentVideoCodec,
                    { it.getDisplayName(context) }, actions.onCodecChange
                )

                VideoPlayerPictureMenuItem.AspectRatio -> Radio(
                    title, VideoAspectRatio.entries, config.currentVideoAspectRatio,
                    { it.getDisplayName(context) }, actions.onAspectRatioChange
                )

                VideoPlayerPictureMenuItem.PlaySpeed -> Stepper(
                    title, config.currentVideoSpeed, 0.25f..2f, 0.25f,
                    speedText(config.currentVideoSpeed), actions.onPlaySpeedChange
                )

                VideoPlayerPictureMenuItem.Audio -> Radio(
                    title, config.availableAudio.sortedBy { it.ordinal }, config.currentAudio,
                    { it.getDisplayName(context) }, actions.onAudioChange
                )
            }
        }

        is MenuLeaf.Danmaku -> {
            val title = leaf.item.getDisplayName(context)
            when (leaf.item) {
                VideoPlayerDanmakuMenuItem.Switch -> {
                    val enabled = config.currentDanmakuEnabledList
                    PanelList(
                        title = title,
                        items = DanmakuType.entries.map { type ->
                            PanelItem(label = type.getDisplayName(context), selected = enabled.contains(type)) {
                                val next = enabled.toMutableList()
                                if (!next.remove(type)) next.add(type)
                                actions.onDanmakuSwitchChange(toggledDanmakuTypes(enabled, next))
                            }
                        },
                        initialIndex = 0,
                        checkSelected = true
                    )
                }

                VideoPlayerDanmakuMenuItem.Size -> Stepper(
                    title, config.currentDanmakuScale, 0.5f..4f, 0.01f,
                    percent(config.currentDanmakuScale), actions.onDanmakuSizeChange
                )

                VideoPlayerDanmakuMenuItem.Opacity -> Stepper(
                    title, config.currentDanmakuOpacity, 0f..1f, 0.01f,
                    percent(config.currentDanmakuOpacity), actions.onDanmakuOpacityChange
                )

                VideoPlayerDanmakuMenuItem.Area -> Stepper(
                    title, config.currentDanmakuArea, 0f..1f, 0.01f,
                    percent(config.currentDanmakuArea), actions.onDanmakuAreaChange
                )

                VideoPlayerDanmakuMenuItem.Mask -> Radio(
                    title, listOf(false, true), config.currentDanmakuMask,
                    { if (it) "开启" else "关闭" }, actions.onDanmakuMaskChange
                )
            }
        }

        is MenuLeaf.Caption -> {
            val title = leaf.item.getDisplayName(context)
            when (leaf.item) {
                VideoPlayerClosedCaptionMenuItem.Switch -> Radio(
                    title,
                    config.availableSubtitleTracks,
                    config.availableSubtitleTracks.firstOrNull { it.id == config.currentSubtitleId },
                    { it?.langDoc ?: "" },
                    { it?.let(actions.onSubtitleChange) }
                )

                VideoPlayerClosedCaptionMenuItem.Size -> IntStepper(
                    title, config.currentSubtitleFontSize.value.toInt(), 12..48,
                    "${config.currentSubtitleFontSize.value.toInt()} SP"
                ) { actions.onSubtitleSizeChange(it.sp) }

                VideoPlayerClosedCaptionMenuItem.Opacity -> Stepper(
                    title, config.currentSubtitleBackgroundOpacity, 0f..1f, 0.01f,
                    percent(config.currentSubtitleBackgroundOpacity), actions.onSubtitleBackgroundOpacityChange
                )

                VideoPlayerClosedCaptionMenuItem.Padding -> IntStepper(
                    title, config.currentSubtitleBottomPadding.value.toInt(), 0..48,
                    "${config.currentSubtitleBottomPadding.value.toInt()} DP"
                ) { actions.onSubtitleBottomPadding(it.dp) }
            }
        }

        is MenuLeaf.Others -> when (leaf.item) {
            VideoPlayerOthersMenuItem.PlayMode -> Radio(
                leaf.item.getDisplayName(context), PlayMode.entries, config.currentPlayMode,
                { it.getDisplayName(context) }, actions.onPlayModeChange
            )
        }
    }
}

/** Keeps "全部弹幕" consistent with the individual types, exactly as the old check list did. */
private fun toggledDanmakuTypes(current: List<DanmakuType>, next: MutableList<DanmakuType>): List<DanmakuType> {
    val allBefore = current.contains(DanmakuType.All)
    val allNow = next.contains(DanmakuType.All)
    return when {
        allNow && !allBefore -> DanmakuType.entries
        allBefore && !allNow -> listOf()
        allBefore && allNow && current.size != next.size -> next.also { it.remove(DanmakuType.All) }
        !allBefore && next.size == DanmakuType.entries.size - 1 -> DanmakuType.entries
        else -> next
    }
}

@Composable
private fun PanelHeader(title: String) {
    Text(
        modifier = Modifier.padding(start = 8.dp, bottom = 13.dp),
        text = title,
        color = PlayerColors.TextPrimary,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun PanelList(
    title: String,
    items: List<PanelItem>,
    initialIndex: Int,
    checkSelected: Boolean = false
) {
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = (initialIndex - 2).coerceAtLeast(0)
    )

    LaunchedEffect(Unit) {
        delay(100)
        focusRequester.requestFocus(scope)
    }

    PanelHeader(title)
    LazyColumn(
        state = listState,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        itemsIndexed(items) { index, item ->
            if (item.dividerBefore) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(DividerColor)
                )
            }
            PanelRow(
                modifier = if (index == initialIndex) Modifier.focusRequester(focusRequester) else Modifier,
                item = item,
                checkSelected = checkSelected
            )
        }
    }
}

@Composable
private fun PanelRow(modifier: Modifier, item: PanelItem, checkSelected: Boolean) {
    var focused by remember { mutableStateOf(false) }
    val labelColor = when {
        focused -> PlayerColors.Background
        item.selected -> PlayerColors.Accent
        else -> RowTextColor
    }
    val trailing = when {
        item.chevron -> "›"
        checkSelected -> if (item.selected) "✓" else null
        else -> item.value
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(if (focused) PlayerColors.TextPrimary else Color.Transparent)
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = item.onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier.weight(1f, fill = false),
            text = item.label,
            color = labelColor,
            fontSize = 10.5.sp,
            maxLines = 1
        )
        if (trailing != null) {
            Text(
                modifier = Modifier.padding(start = 12.dp),
                text = trailing,
                color = when {
                    focused -> PlayerColors.Accent
                    checkSelected -> PlayerColors.Accent
                    else -> PlayerColors.TextSecondary
                },
                fontSize = 9.6.sp,
                fontWeight = if (focused) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1
            )
        }
    }
}

/** Up/Right raise the value and Down/Left lower it; OK returns to the list. */
@Composable
private fun PanelStepper(
    title: String,
    valueText: String,
    fraction: Float,
    onStep: (direction: Int) -> Unit,
    onDone: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(100)
        focusRequester.requestFocus(scope)
    }

    PanelHeader(title)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .focusRequester(focusRequester)
            .onPreviewKeyEvent {
                val direction = when (it.key) {
                    Key.DirectionUp, Key.DirectionRight -> 1
                    Key.DirectionDown, Key.DirectionLeft -> -1
                    else -> 0
                }
                when {
                    direction != 0 -> {
                        if (it.type == KeyEventType.KeyDown) onStep(direction)
                        true
                    }

                    it.key == Key.Enter || it.key == Key.DirectionCenter -> {
                        if (it.type == KeyEventType.KeyUp) onDone()
                        true
                    }

                    else -> false
                }
            }
            .focusable(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            modifier = Modifier.padding(top = 24.dp),
            text = valueText,
            color = PlayerColors.TextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(DividerColor)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .background(PlayerColors.Accent)
            )
        }
        Text(
            text = "↑/↓ 调整 · OK 完成",
            color = PlayerColors.TextSecondary,
            fontSize = 9.sp
        )
    }
}
