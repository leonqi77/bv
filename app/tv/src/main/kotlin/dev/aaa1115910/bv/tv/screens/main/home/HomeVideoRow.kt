package dev.aaa1115910.bv.tv.screens.main.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.aaa1115910.bv.entity.carddata.VideoCardData
import dev.aaa1115910.bv.tv.component.LoadingTip
import dev.aaa1115910.bv.tv.component.videocard.SmallVideoCard
import dev.aaa1115910.bv.util.ifElse

private val RowTopPadding = 6.dp
private val RowTitleHeight = 26.dp
private val CardsVerticalPadding = 10.dp
private val CardWidth = 190.dp

/** 分行顶部到卡片封面顶部的距离，首页纵向滚动时用它把聚焦的分行对齐到头部下方。 */
val HomeVideoRowCardTop = RowTopPadding + RowTitleHeight + CardsVerticalPadding

/** 距离末尾还剩几张卡片时开始加载更多。 */
private const val LoadMoreThreshold = 5

@Composable
fun HomeVideoRow(
    modifier: Modifier = Modifier,
    title: String,
    items: List<VideoCardData>,
    loading: Boolean = false,
    listState: LazyListState = rememberLazyListState(),
    firstCardFocusRequester: FocusRequester = remember { FocusRequester() },
    onItemFocused: (Int) -> Unit,
    onItemClick: (Int) -> Unit,
    onNearEnd: () -> Unit
) {
    Column(modifier = modifier.padding(top = RowTopPadding)) {
        Box(
            modifier = Modifier
                .height(RowTitleHeight)
                .padding(start = HomeContentStartPadding),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        if (items.isEmpty()) {
            if (loading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    LoadingTip()
                }
            }
            return@Column
        }
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .focusRestorer(firstCardFocusRequester),
            state = listState,
            contentPadding = PaddingValues(
                start = HomeContentStartPadding,
                end = 48.dp,
                top = CardsVerticalPadding,
                bottom = CardsVerticalPadding
            ),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            itemsIndexed(items = items) { index, item ->
                SmallVideoCard(
                    modifier = Modifier
                        .width(CardWidth)
                        .ifElse(index == 0, Modifier.focusRequester(firstCardFocusRequester)),
                    data = item,
                    onClick = { onItemClick(index) },
                    onFocus = {
                        onItemFocused(index)
                        if (index >= items.size - LoadMoreThreshold) onNearEnd()
                    }
                )
            }
            if (loading) {
                item {
                    Box(
                        modifier = Modifier
                            .width(CardWidth)
                            .height(107.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        LoadingTip()
                    }
                }
            }
        }
    }
}
