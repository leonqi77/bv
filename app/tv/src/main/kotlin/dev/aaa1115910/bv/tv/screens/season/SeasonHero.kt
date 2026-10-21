package dev.aaa1115910.bv.tv.screens.season

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.ProvideTextStyle
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.request.ImageRequest
import dev.aaa1115910.bv.tv.component.BvCardDefaults
import dev.aaa1115910.bv.ui.theme.BVTheme
import dev.aaa1115910.bv.ui.theme.BvColors
import dev.aaa1115910.bv.util.ImageSize
import dev.aaa1115910.bv.util.resizedImageUrl
import dev.aaa1115910.bv.viewmodel.season.PlayButtonState
import dev.aaa1115910.bv.viewmodel.season.SeasonBackdrop
import dev.aaa1115910.bv.viewmodel.season.SeasonFacts

/** 剧集页内容的左侧起点（TV 安全边距）。 */
val SeasonContentStartPadding = 48.dp

/** 头部最小高度：正片分行从屏幕约 60% 处开始，与设计稿一致。 */
val SeasonHeroMinHeight = 318.dp

private val HeroButtonShape = RoundedCornerShape(6.dp)
private val HeroTextMaxWidth = 420.dp

// 设计稿的渐变覆盖整个屏幕，这里换算到背景图区域（宽 72%、高 78%，靠右上）内的位置；左侧略加深以保证文字可读
private val backdropHorizontalFade = Brush.horizontalGradient(
    0f to BvColors.Background,
    0.2f to BvColors.Background.copy(alpha = 0.9f),
    0.5f to BvColors.Background.copy(alpha = 0.35f),
    0.8f to Color.Transparent
)

private val backdropBottomFade = Brush.verticalGradient(
    0.513f to Color.Transparent,
    0.795f to BvColors.Background.copy(alpha = 0.67f),
    1f to BvColors.Background
)

/**
 * 背景图：靠右上，宽 72%、高 78%，向左、向下渐隐到背景色。
 * 模糊海报不做运行时模糊，而是加载极小尺寸的海报再放大并压暗，在 onn 上没有额外开销。
 */
@Composable
fun SeasonBackdropImage(
    modifier: Modifier = Modifier,
    backdrop: SeasonBackdrop
) {
    Box(
        modifier = modifier
            .fillMaxWidth(0.72f)
            .fillMaxHeight(0.78f)
    ) {
        when (backdrop) {
            is SeasonBackdrop.Image -> if (backdrop.url.isNotBlank()) {
                val context = LocalContext.current
                // 大图加载完成前，先显示剧集卡片已缓存的小图（第一集封面作为背景时）
                val request = remember(backdrop.url) {
                    ImageRequest.Builder(context)
                        .data(backdrop.url.resizedImageUrl(ImageSize.SeasonBackdrop))
                        .placeholderMemoryCacheKey(backdrop.url.resizedImageUrl(ImageSize.SmallVideoCardCover))
                        .build()
                }
                AsyncImage(
                    modifier = Modifier.fillMaxSize(),
                    model = request,
                    contentDescription = null,
                    contentScale = ContentScale.Crop
                )
            }

            is SeasonBackdrop.BlurredPoster -> if (backdrop.url.isNotBlank()) {
                AsyncImage(
                    modifier = Modifier.fillMaxSize(),
                    model = backdrop.url.resizedImageUrl(ImageSize.SeasonBackdropBlurred),
                    contentDescription = null,
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(BvColors.Background.copy(alpha = 0.5f))
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backdropHorizontalFade)
                .background(backdropBottomFade)
        )
    }
}

/** 头部：大标题、信息行、简介（3 行）、播放 / 追番 / 选集 按钮。 */
@Composable
fun SeasonHero(
    modifier: Modifier = Modifier,
    title: String,
    facts: SeasonFacts,
    description: String,
    playState: PlayButtonState,
    following: Boolean,
    showEpisodesButton: Boolean,
    playFocusRequester: FocusRequester = remember { FocusRequester() },
    onPlay: () -> Unit,
    onClickFollow: (following: Boolean) -> Unit,
    onClickEpisodes: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = SeasonHeroMinHeight)
    ) {
        Column(
            modifier = Modifier
                .padding(start = SeasonContentStartPadding, top = 40.dp, bottom = 16.dp)
        ) {
            // 文字限制在左侧 420dp 内；按钮行不受此限制，避免长的“继续播放”文案把其他按钮挤到换行
            Text(
                modifier = Modifier.widthIn(max = HeroTextMaxWidth),
                text = title,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = BvColors.TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            SeasonFactsLine(
                modifier = Modifier
                    .padding(top = 10.dp)
                    .widthIn(max = HeroTextMaxWidth),
                facts = facts
            )
            if (description.isNotBlank()) {
                Text(
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .widthIn(max = HeroTextMaxWidth),
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = BvColors.TextSecondary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(
                modifier = Modifier.padding(top = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PlayButton(
                    modifier = Modifier.focusRequester(playFocusRequester),
                    state = playState,
                    onClick = onPlay
                )
                HeroButton(onClick = { onClickFollow(following) }) {
                    Icon(
                        modifier = Modifier.size(18.dp),
                        imageVector = if (following) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = null
                    )
                    Text(text = if (following) "已追番" else "追番", maxLines = 1)
                }
                if (showEpisodesButton) {
                    HeroButton(onClick = onClickEpisodes) {
                        Icon(
                            modifier = Modifier.size(18.dp),
                            imageVector = Icons.AutoMirrored.Rounded.List,
                            contentDescription = null
                        )
                        Text(text = "选集", maxLines = 1)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SeasonFactsLine(
    modifier: Modifier = Modifier,
    facts: SeasonFacts
) {
    if (facts.rating == null && facts.items.isEmpty() && facts.tags.isEmpty()) return
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        val textStyle = MaterialTheme.typography.bodyMedium
        facts.rating?.let {
            Text(
                modifier = Modifier.align(Alignment.CenterVertically),
                text = it,
                style = textStyle,
                fontWeight = FontWeight.Bold,
                color = BvColors.Accent
            )
        }
        facts.items.forEach {
            Text(
                modifier = Modifier.align(Alignment.CenterVertically),
                text = it,
                style = textStyle,
                color = BvColors.TextSecondary,
                maxLines = 1
            )
        }
        facts.tags.forEach {
            Text(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(3.dp))
                    .padding(horizontal = 5.dp, vertical = 1.dp),
                text = it,
                style = MaterialTheme.typography.labelSmall,
                color = BvColors.TextPrimary.copy(alpha = 0.85f),
                maxLines = 1
            )
        }
    }
}

/** 播放按钮：浅色填充；不可播放时显示为禁用样式，仍可获得焦点但点击无效。 */
@Composable
private fun PlayButton(
    modifier: Modifier = Modifier,
    state: PlayButtonState,
    onClick: () -> Unit
) {
    HeroButton(
        modifier = modifier,
        primary = true,
        enabled = state.enabled,
        onClick = onClick
    ) {
        Icon(
            modifier = Modifier.size(20.dp),
            imageVector = Icons.Rounded.PlayArrow,
            contentDescription = null
        )
        // "继续播放 第N话 · 剩 M 分钟"：主文字加粗，其余部分弱化
        val resumePrefix = "继续播放 "
        if (state.enabled && state.text.startsWith(resumePrefix)) {
            Text(text = resumePrefix.trim(), fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text(
                text = state.text.removePrefix(resumePrefix),
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                color = BvColors.Background.copy(alpha = 0.7f)
            )
        } else {
            Text(
                text = state.text,
                fontWeight = if (state.enabled) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun HeroButton(
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit
) {
    val lightFill = primary && enabled
    val dimFill = Color.White.copy(alpha = 0.15f)
    Surface(
        modifier = modifier,
        onClick = { if (enabled) onClick() },
        shape = ClickableSurfaceDefaults.shape(shape = HeroButtonShape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (lightFill) BvColors.TextPrimary else dimFill,
            contentColor = when {
                lightFill -> BvColors.Background
                enabled -> BvColors.TextPrimary
                else -> BvColors.TextSecondary
            },
            focusedContainerColor = if (enabled) BvColors.TextPrimary else Color.White.copy(alpha = 0.22f),
            focusedContentColor = if (enabled) BvColors.Background else BvColors.TextPrimary,
            pressedContainerColor = if (enabled) BvColors.TextPrimary else Color.White.copy(alpha = 0.22f),
            pressedContentColor = if (enabled) BvColors.Background else BvColors.TextPrimary
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = BvCardDefaults.FocusedScale),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(
                    width = 2.dp,
                    color = if (lightFill) BvColors.Accent else Color.White.copy(alpha = 0.6f)
                ),
                shape = HeroButtonShape
            )
        ),
        glow = ClickableSurfaceDefaults.glow(focusedGlow = BvCardDefaults.focusedGlow())
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProvideTextStyle(MaterialTheme.typography.titleSmall) {
                content()
            }
        }
    }
}

@Preview(device = "id:tv_1080p")
@Preview(device = "id:tv_1080p", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SeasonHeroPreview() {
    BVTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BvColors.Background)
        ) {
            SeasonHero(
                title = "全职猎人",
                facts = SeasonFacts(
                    rating = "★ 9.8",
                    items = listOf("1999", "日本", "已完结, 全92话"),
                    tags = listOf("奇幻", "热血", "智斗", "战斗")
                ),
                description = "全职猎人（HUNTER×HUNTER）是漫画家富坚义博的一部力作，在其天马行空的想象力下打造出了一个奇妙的幻想世界，讲述了主人公小杰在寻父的道路上与奇犽、酷拉皮卡、雷欧力等几位友人经历的各种冒险，以及其他或有趣或沉重的故事。",
                playState = PlayButtonState("继续播放 第3话 · 剩 12 分钟", true, 3),
                following = false,
                showEpisodesButton = true,
                onPlay = {},
                onClickFollow = {},
                onClickEpisodes = {}
            )
        }
    }
}
