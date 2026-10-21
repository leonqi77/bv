package dev.aaa1115910.bv.player.tv.controller

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import dev.aaa1115910.biliapi.entity.video.VideoShot
import dev.aaa1115910.bv.player.tv.PlayerColors
import dev.aaa1115910.bv.util.formatHourMinSec

/** The controls-layer buttons, in on-screen order. */
enum class ControlButtonId { PlayPause, Previous, Next, Resolution, Speed, Danmaku, Subtitle, Episodes, Settings }

private const val HINT = "←/→ 快退/快进 · ↓ 更多操作 · ↑ 播放列表 · Menu 设置"

// Sizes follow player.html section 1, where 1cqw = 9.6dp on the 960dp-wide TV layout.
private val SideInset = 38.dp
private val TopInset = 29.dp
private val BottomInset = 29.dp
private val ButtonHeight = 28.dp
private val ButtonShape = RoundedCornerShape(14.dp)
private val TextShadow = Shadow(color = Color.Black, blurRadius = 8f)

private val labels = mapOf(
    ControlButtonId.Resolution to "清晰度",
    ControlButtonId.Speed to "倍速",
    ControlButtonId.Danmaku to "弹幕",
    ControlButtonId.Subtitle to "字幕",
    ControlButtonId.Episodes to "选集",
)

/**
 * YouTube-style controls overlay: who made it and what it is at the top, the progress bar and a
 * row of buttons at the bottom. Purely visual; keys are handled by [VideoPlayerController].
 *
 * @param activeButtons buttons whose value is shown in the accent (an "on" switch).
 * @param disabledButtons buttons drawn dimmed because they would do nothing.
 * @param progress read only by the progress bar and the time row, so a position tick
 *   recomposes just those and not the header or the buttons.
 * @param seekPreview the preview thumbnails, passed only while a seek is in progress.
 */
@Composable
fun ControlsLayer(
    state: ControlsState,
    header: PlayerHeader,
    progress: () -> PlaybackProgress,
    isPlaying: Boolean,
    buttonValues: Map<ControlButtonId, String>,
    clock: String,
    modifier: Modifier = Modifier,
    followed: Boolean = false,
    activeButtons: Set<ControlButtonId> = emptySet(),
    disabledButtons: Set<ControlButtonId> = emptySet(),
    seekPreview: VideoShot? = null,
) {
    AnimatedVisibility(
        modifier = modifier.fillMaxSize(),
        visible = state.focus != ControlsFocus.Hidden,
        enter = fadeIn(),
        exit = fadeOut(),
        label = "ControlsLayerVisible"
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(0.28f)
                    .background(Brush.verticalGradient(listOf(Color(0xCC000000), Color.Transparent)))
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(0.46f)
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.45f to Color(0x99000000),
                            0.9f to Color(0xEE000000),
                            1f to Color(0xEE000000),
                        )
                    )
            )

            Header(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = SideInset, top = TopInset, end = 192.dp),
                header = header,
                followed = followed
            )
            Text(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = SideInset, top = TopInset),
                text = clock,
                style = TextStyle(
                    color = PlayerColors.TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    shadow = TextShadow
                )
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(start = SideInset, end = SideInset, bottom = BottomInset)
            ) {
                ProgressSection(
                    progress = progress,
                    focused = state.focus == ControlsFocus.ProgressBar,
                    seekPreview = seekPreview
                )
                ButtonRow(
                    modifier = Modifier.padding(top = 10.dp),
                    focusedIndex = state.buttonIndex.takeIf { state.focus == ControlsFocus.Buttons },
                    isPlaying = isPlaying,
                    buttonValues = buttonValues,
                    activeButtons = activeButtons,
                    disabledButtons = disabledButtons
                )
            }

            if (state.focus == ControlsFocus.ProgressBar) {
                Text(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp),
                    text = HINT,
                    style = TextStyle(color = Color(0x88FFFFFF), fontSize = 8.sp)
                )
            }
        }
    }
}

/** Position, duration and buffered end of the playback, in milliseconds. */
data class PlaybackProgress(val positionMs: Long, val durationMs: Long, val bufferedMs: Long)

@Composable
private fun ProgressSection(
    progress: () -> PlaybackProgress,
    focused: Boolean,
    seekPreview: VideoShot?
) {
    val (positionMs, durationMs, bufferedMs) = progress()
    Column {
        if (seekPreview != null) {
            VideoShot(
                modifier = Modifier.padding(bottom = 10.dp),
                videoShot = seekPreview,
                position = positionMs,
                duration = durationMs
            )
        }
        ProgressBar(
            played = fraction(positionMs, durationMs),
            buffered = fraction(bufferedMs, durationMs),
            focused = focused
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 7.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val timeStyle = TextStyle(color = Color(0xFFDDDDDD), fontSize = 10.sp)
            Text(text = positionMs.formatHourMinSec(), style = timeStyle)
            Text(text = durationMs.formatHourMinSec(), style = timeStyle)
        }
    }
}

private fun fraction(value: Long, total: Long): Float =
    if (total <= 0) 0f else (value.toFloat() / total).coerceIn(0f, 1f)

@Composable
private fun Header(
    modifier: Modifier = Modifier,
    header: PlayerHeader,
    followed: Boolean
) {
    Column(modifier = modifier) {
        if (header.small.isNotEmpty()) {
            Row(
                modifier = Modifier.padding(bottom = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                if (header.smallFace.isNotEmpty()) {
                    AsyncImage(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape),
                        model = header.smallFace,
                        contentDescription = null
                    )
                }
                Text(
                    text = header.small,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(
                        color = Color(0xFFDDDDDD),
                        fontSize = 11.sp,
                        shadow = TextShadow
                    )
                )
                if (followed) {
                    Text(
                        modifier = Modifier
                            .border(1.dp, PlayerColors.Accent.copy(alpha = 0.53f), ButtonShape)
                            .padding(horizontal = 6.dp),
                        text = "已关注",
                        style = TextStyle(color = PlayerColors.Accent, fontSize = 8.sp)
                    )
                }
            }
        }
        Text(
            text = header.title,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(
                color = PlayerColors.TextPrimary,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                shadow = TextShadow
            )
        )
    }
}

@Composable
private fun ProgressBar(
    played: Float,
    buffered: Float,
    focused: Boolean
) {
    // Focused: a 13dp knob with a 3dp translucent accent ring; otherwise a small plain knob.
    val knobSize = if (focused) 13.dp else 8.dp
    val ring = if (focused) 3.dp else 0.dp
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(19.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        val track = Modifier
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
        Box(modifier = track.fillMaxWidth().background(Color(0x38FFFFFF)))
        Box(modifier = track.width(maxWidth * buffered).background(Color(0x55FFFFFF)))
        Box(modifier = track.width(maxWidth * played).background(PlayerColors.Accent))
        Box(
            modifier = Modifier
                .offset(x = maxWidth * played - knobSize / 2 - ring)
                .size(knobSize + ring * 2)
                .clip(CircleShape)
                .background(PlayerColors.Accent.copy(alpha = 0.4f))
                .padding(ring)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

@Composable
private fun ButtonRow(
    modifier: Modifier = Modifier,
    focusedIndex: Int?,
    isPlaying: Boolean,
    buttonValues: Map<ControlButtonId, String>,
    activeButtons: Set<ControlButtonId>,
    disabledButtons: Set<ControlButtonId>
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        ControlButtonId.entries.forEach { id ->
            val focused = focusedIndex == id.ordinal
            val disabled = id in disabledButtons
            val icon = iconOf(id, isPlaying)
            if (icon != null) {
                RoundButton(icon = icon, focused = focused, disabled = disabled)
            } else {
                LabelButton(
                    label = labels.getValue(id),
                    value = buttonValues[id].orEmpty(),
                    active = id in activeButtons,
                    focused = focused
                )
            }
            if (id == ControlButtonId.Next) Spacer(modifier = Modifier.weight(1f))
        }
    }
}

private fun iconOf(id: ControlButtonId, isPlaying: Boolean): ImageVector? = when (id) {
    ControlButtonId.PlayPause -> if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow
    ControlButtonId.Previous -> Icons.Rounded.SkipPrevious
    ControlButtonId.Next -> Icons.Rounded.SkipNext
    ControlButtonId.Settings -> Icons.Rounded.Settings
    else -> null
}

private fun Modifier.buttonSurface(focused: Boolean): Modifier = this
    .scale(if (focused) 1.08f else 1f)
    .height(ButtonHeight)
    .clip(ButtonShape)
    .background(if (focused) PlayerColors.TextPrimary else Color(0x1FFFFFFF))

@Composable
private fun RoundButton(icon: ImageVector, focused: Boolean, disabled: Boolean) {
    Box(
        modifier = Modifier
            .alpha(if (disabled) 0.4f else 1f)
            .buttonSurface(focused)
            .width(ButtonHeight),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            modifier = Modifier.size(16.dp),
            imageVector = icon,
            contentDescription = null,
            tint = if (focused) PlayerColors.Background else PlayerColors.TextPrimary
        )
    }
}

@Composable
private fun LabelButton(label: String, value: String, active: Boolean, focused: Boolean) {
    Row(
        modifier = Modifier
            .buttonSurface(focused)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = TextStyle(
                color = if (focused) PlayerColors.Background else PlayerColors.TextPrimary,
                fontSize = 10.sp
            )
        )
        if (value.isNotEmpty()) {
            Text(
                text = value,
                style = TextStyle(
                    color = if (active) PlayerColors.Accent else PlayerColors.TextSecondary,
                    fontSize = 9.sp,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal
                )
            )
        }
    }
}
