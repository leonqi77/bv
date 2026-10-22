package dev.aaa1115910.bv.tv.component

import androidx.compose.runtime.Composable
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
        containerColor = BvColors.ChipFill,
        contentColor = BvColors.TextPrimary,
        focusedContainerColor = BvColors.TextPrimary,
        focusedContentColor = BvColors.Background,
        pressedContainerColor = BvColors.TextPrimary,
        pressedContentColor = BvColors.Background
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
