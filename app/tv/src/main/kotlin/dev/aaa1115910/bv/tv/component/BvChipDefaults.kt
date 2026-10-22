package dev.aaa1115910.bv.tv.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.TabDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableChipBorder
import androidx.tv.material3.ClickableChipColors
import androidx.tv.material3.SuggestionChipDefaults
import dev.aaa1115910.bv.ui.theme.BvColors

/** 标签/分区芯片统一样式：平时 [BvColors.ChipFill]，聚焦时亮色填充并反色文字。 */
object BvChipDefaults {
    @OptIn(ExperimentalTvMaterial3Api::class)
    @Composable
    fun suggestionColors(): ClickableChipColors = SuggestionChipDefaults.colors(
        containerColor = restingFill(),
        contentColor = MaterialTheme.colorScheme.onSurface,
        focusedContainerColor = MaterialTheme.colorScheme.onSurface,
        focusedContentColor = MaterialTheme.colorScheme.surface,
        pressedContainerColor = MaterialTheme.colorScheme.onSurface,
        pressedContentColor = MaterialTheme.colorScheme.surface
    )

    /** 芯片静止填充：由主题 onSurface 派生，深色下等同 [BvColors.ChipFill]，浅色下仍可见。 */
    @Composable
    fun restingFill(): Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)

    /** 选中但未聚焦的标签文字：深色为 TextPrimary，其余标签用 onSurfaceVariant。 */
    @Composable
    fun tabColors() = TabDefaults.pillIndicatorTabColors(
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        selectedContentColor = MaterialTheme.colorScheme.onSurface,
        focusedContentColor = MaterialTheme.colorScheme.surface,
        focusedSelectedContentColor = MaterialTheme.colorScheme.surface
    )

    /** 平时无描边（只靠 [BvColors.ChipFill] 区分），聚焦用亮色填充。 */
    @OptIn(ExperimentalTvMaterial3Api::class)
    @Composable
    fun suggestionBorder(): ClickableChipBorder = SuggestionChipDefaults.border(
        border = Border.None,
        focusedBorder = Border.None,
        pressedBorder = Border.None
    )
}
