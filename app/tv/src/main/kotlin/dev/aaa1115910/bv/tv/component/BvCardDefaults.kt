package dev.aaa1115910.bv.tv.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.CardBorder
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.CardGlow
import androidx.tv.material3.CardScale
import androidx.tv.material3.Glow
import androidx.tv.material3.MaterialTheme

/** 卡片统一焦点效果：放大 1.06、1.5dp 白色描边（跟随卡片形状）、柔和深色阴影。 */
object BvCardDefaults {
    const val FocusedScale = 1.06f

    @Composable
    fun scale(): CardScale = CardDefaults.scale(focusedScale = FocusedScale)

    @Composable
    fun focusedBorder(): Border = Border(
        border = BorderStroke(width = 1.5.dp, color = Color.White),
        shape = MaterialTheme.shapes.medium
    )

    fun focusedGlow(): Glow = Glow(
        elevationColor = Color.Black.copy(alpha = 0.6f),
        elevation = 16.dp
    )

    @Composable
    fun border(): CardBorder = CardDefaults.border(focusedBorder = focusedBorder())

    @Composable
    fun glow(): CardGlow = CardDefaults.glow(focusedGlow = focusedGlow())
}
