package dev.aaa1115910.bv.tv.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import dev.aaa1115910.bv.ui.theme.BvColors

/** 列表加载完成后仍为空时显示：图标 + 一行提示。 */
@Composable
fun EmptyState(
    text: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            modifier = Modifier.size(48.dp),
            imageVector = Icons.Rounded.Inbox,
            contentDescription = null,
            tint = BvColors.TextSecondary
        )
        Text(text = text, color = BvColors.TextSecondary)
    }
}
