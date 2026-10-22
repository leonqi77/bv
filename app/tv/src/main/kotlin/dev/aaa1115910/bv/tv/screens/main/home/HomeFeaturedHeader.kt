package dev.aaa1115910.bv.tv.screens.main.home

import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.request.ImageRequest
import dev.aaa1115910.bv.entity.carddata.HomeFeaturedItem
import dev.aaa1115910.bv.entity.carddata.statsLine
import dev.aaa1115910.bv.tv.component.UpIcon
import dev.aaa1115910.bv.ui.theme.BvColors
import dev.aaa1115910.bv.util.ImageSize
import dev.aaa1115910.bv.util.resizedImageUrl

/** 首页大图头部的高度，约为屏幕的 46%。 */
val HomeFeaturedHeaderHeight = 248.dp

/** 头部文字与分行卡片共用的左侧起点。 */
val HomeContentStartPadding = 28.dp

// 渐隐到页面底色（随主题），深色主题下即 BvColors.Background
private fun coverHorizontalFade(background: Color) = Brush.horizontalGradient(
    0f to background,
    0.19f to background.copy(alpha = 0.8f),
    0.63f to Color.Transparent
)

private fun coverBottomFade(background: Color) = Brush.verticalGradient(
    0.6f to Color.Transparent,
    1f to background
)

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun HomeFeaturedHeader(
    modifier: Modifier = Modifier,
    item: HomeFeaturedItem?
) {
    updateTransition(targetState = item, label = "home featured header").Crossfade(
        modifier = modifier
            .fillMaxWidth()
            .height(HomeFeaturedHeaderHeight),
        animationSpec = tween(durationMillis = 250),
        contentKey = { it?.aid }
    ) { featured ->
        if (featured != null) HeaderContent(item = featured)
    }
}

@Composable
private fun HeaderContent(item: HomeFeaturedItem) {
    Box(modifier = Modifier.fillMaxSize()) {
        // 封面靠右，向左、向下渐隐到背景色
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .fillMaxWidth(0.74f)
                .fillMaxHeight()
        ) {
            if (item.cover.isNotBlank()) {
                val context = LocalContext.current
                // 大图加载完成前先显示卡片已缓存的小图，避免头部空白
                val request = remember(item.cover) {
                    ImageRequest.Builder(context)
                        .data(item.cover.resizedImageUrl(ImageSize.FeaturedHeaderCover))
                        .placeholderMemoryCacheKey(item.cover.resizedImageUrl(ImageSize.SmallVideoCardCover))
                        .build()
                }
                AsyncImage(
                    modifier = Modifier.fillMaxSize(),
                    model = request,
                    contentDescription = null,
                    contentScale = ContentScale.Crop
                )
            }
            val background = MaterialTheme.colorScheme.surface
            val horizontalFade = remember(background) { coverHorizontalFade(background) }
            val bottomFade = remember(background) { coverBottomFade(background) }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(horizontalFade)
                    .background(bottomFade)
            )
        }

        Column(
            modifier = Modifier
                .padding(start = HomeContentStartPadding, top = 28.dp)
                .width(420.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            HeaderUp(upName = item.upName, upFace = item.upFace)
            Text(
                text = item.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            val stats = item.statsLine()
            val statsText = remember(stats, item.is4K) {
                buildAnnotatedString {
                    append(stats)
                    if (item.is4K) {
                        if (stats.isNotEmpty()) append(" · ")
                        withStyle(SpanStyle(color = BvColors.Accent, fontWeight = FontWeight.Bold)) {
                            append("4K")
                        }
                    }
                }
            }
            if (statsText.isNotEmpty()) {
                Text(
                    text = statsText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            if (item.description.isNotBlank()) {
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun HeaderUp(upName: String, upFace: String) {
    if (upName.isBlank() && upFace.isBlank()) return
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (upFace.isNotBlank()) {
            AsyncImage(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape),
                model = upFace.resizedImageUrl(ImageSize.Avatar),
                contentDescription = null,
                contentScale = ContentScale.Crop
            )
        } else {
            UpIcon(color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            text = upName,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
