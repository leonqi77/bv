package dev.aaa1115910.bv.tv.component.videocard

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import dev.aaa1115910.bv.entity.carddata.VideoCardData
import dev.aaa1115910.bv.entity.carddata.metaLine
import dev.aaa1115910.bv.tv.component.BvCardDefaults
import dev.aaa1115910.bv.tv.component.UpIcon
import dev.aaa1115910.bv.ui.theme.BVTheme
import dev.aaa1115910.bv.util.ImageSize
import dev.aaa1115910.bv.util.resizedImageUrl

@Composable
fun SmallVideoCard(
    modifier: Modifier = Modifier,
    data: VideoCardData,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
    onFocus: () -> Unit = {}
) {
    var hasFocus by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }

    SmallVideoCardContent(
        modifier = modifier
            .onFocusChanged {
                hasFocus = it.isFocused
                if (hasFocus) onFocus()
            },
        data = data,
        hasFocus = hasFocus,
        interactionSource = interactionSource,
        onClick = onClick,
        onLongClick = onLongClick,
        onFocusChanged = {
            hasFocus = it
            if (it) onFocus()
        }
    )
}

@Composable
fun SmallVideoCardContent(
    modifier: Modifier = Modifier,
    data: VideoCardData,
    hasFocus: Boolean,
    interactionSource: MutableInteractionSource? = null,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
    onFocusChanged: (Boolean) -> Unit = {}
) {
    Column(
        modifier = modifier
    ) {
        Card(
            onClick = onClick,
            onLongClick = onLongClick,
            colors = CardDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surface,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                pressedContainerColor = MaterialTheme.colorScheme.surface
            ),
            shape = CardDefaults.shape(shape = MaterialTheme.shapes.medium),
            scale = BvCardDefaults.scale(),
            border = BvCardDefaults.border(),
            glow = BvCardDefaults.glow()
        ) {
            CardCover(
                cover = data.cover,
                time = data.timeString,
                is4K = data.is4K
            )
        }

        CardInfo(
            modifier = Modifier.padding(top = 8.dp),
            title = data.title,
            upFace = data.upFace,
            meta = data.metaLine()
        )
    }
}

@Composable
fun CardCover(
    modifier: Modifier = Modifier,
    cover: String,
    time: String,
    is4K: Boolean = false
) {
    Box(
        modifier = modifier.clip(MaterialTheme.shapes.medium)
    ) {
        AsyncImage(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f),
            model = cover.resizedImageUrl(ImageSize.SmallVideoCardCover),
            contentDescription = null,
            contentScale = ContentScale.Crop
        )
        if (is4K) {
            Text(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .background(Color.Black.copy(alpha = 0.6f), MaterialTheme.shapes.extraSmall)
                    .padding(horizontal = 4.dp, vertical = 1.dp),
                text = "4K",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White
            )
        }
        if (time.isNotBlank()) {
            Text(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 1.dp),
                text = time,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun CardInfo(
    modifier: Modifier = Modifier,
    title: String,
    upFace: String,
    meta: String
) {
    Row(
        modifier = modifier.padding(horizontal = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (upFace.isNotBlank()) {
            AsyncImage(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape),
                model = upFace.resizedImageUrl(ImageSize.Avatar),
                contentDescription = null,
                contentScale = ContentScale.Crop
            )
        } else {
            UpIcon(color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (meta.isNotBlank()) {
                Text(
                    modifier = Modifier.padding(top = 2.dp),
                    text = meta,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun SmallVideoCardWithoutFocusPreview() {
    val data = VideoCardData(
        avid = 0,
        title = "震惊！太震惊了！真的是太震惊了！我的天呐！真TMD震惊！",
        cover = "http://i2.hdslb.com/bfs/archive/af17fc07b8f735e822563cc45b7b5607a491dfff.jpg",
        upName = "bishi",
        play = 2333,
        danmaku = 666,
        time = 2333 * 1000
    )
    BVTheme {
        Surface(
            modifier = Modifier.width(300.dp)
        ) {
            SmallVideoCardContent(
                modifier = Modifier.padding(20.dp),
                data = data,
                hasFocus = false
            )
        }
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun SmallVideoCardWithFocusPreview() {
    val data = VideoCardData(
        avid = 0,
        title = "震惊！太震惊了！真的是太震惊了！我的天呐！真TMD震惊！",
        cover = "http://i2.hdslb.com/bfs/archive/af17fc07b8f735e822563cc45b7b5607a491dfff.jpg",
        upName = "bishi",
        play = 2333,
        danmaku = 666,
        time = 2333 * 1000
    )
    BVTheme {
        Surface(
            modifier = Modifier.width(300.dp)
        ) {
            SmallVideoCardContent(
                modifier = Modifier.padding(20.dp),
                data = data,
                hasFocus = true
            )
        }
    }
}

@Preview(device = "id:tv_1080p")
@Preview(device = "id:tv_1080p", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun SmallVideoCardsPreview() {
    val data = VideoCardData(
        avid = 0,
        title = "震惊！太震惊了！真的是太震惊了！我的天呐！真TMD震惊！",
        //cover = "http://i2.hdslb.com/bfs/archive/af17fc07b8f735e822563cc45b7b5607a491dfff.jpg",
        cover = "",
        upName = "bishi",
        play = 2333,
        danmaku = 666,
        time = 2333 * 1000
    )
    BVTheme {
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            repeat(20) {
                item {
                    SmallVideoCard(
                        data = data
                    )
                }
            }
        }
    }
}
