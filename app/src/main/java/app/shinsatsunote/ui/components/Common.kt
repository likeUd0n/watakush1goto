package app.shinsatsunote.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.shinsatsunote.ui.icons.AppIcons
import app.shinsatsunote.ui.theme.AppTheme

/** 画面の上の行(.top): 左・見出し・右。高さ 48dp 以上 */
@Composable
fun TopRow(
    title: String,
    modifier: Modifier = Modifier,
    start: @Composable RowScope.() -> Unit = {},
    end: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier.fillMaxWidth().heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        start()
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        end()
    }
}

/** サブ画面の上の行: 戻る・見出し・右の操作 */
@Composable
fun SubTopRow(
    title: String,
    backLabel: String,
    onBack: () -> Unit,
    end: @Composable RowScope.() -> Unit = {},
) {
    TopRow(
        title = title,
        start = { AppIconButton(AppIcons.Left, backLabel, onBack) },
        end = end,
    )
}

/** 丸いアイコンボタン(.icon-btn): 押せる範囲は 48dp、アイコンは 24dp */
@Composable
fun AppIconButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = AppTheme.colors.muted,
) {
    IconButton(onClick = onClick, modifier = modifier, enabled = enabled) {
        Icon(icon, contentDescription = label, modifier = Modifier.size(24.dp), tint = if (enabled) tint else tint.copy(alpha = 0.38f))
    }
}

/** 画面上部の角丸アイコンタイル: Segmented と高さ・質感を揃え、ボタンが浮かないようにする */
@Composable
fun HeaderIconTile(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = AppTheme.colors
    Surface(
        onClick = onClick,
        modifier = modifier.size(42.dp),
        shape = RoundedCornerShape(12.dp),
        color = c.surface,
        border = BorderStroke(1.dp, c.line.copy(alpha = 0.85f)),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(23.dp), tint = c.ink.copy(alpha = 0.78f))
        }
    }
}

/** カード(.card): 白地・角丸 16dp・薄い枠 */
@Composable
fun AppCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = AppTheme.colors.surface,
        border = BorderStroke(1.dp, AppTheme.colors.line.copy(alpha = 0.65f)),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}

/** 入口のボタン(.entry-btn): 押すとサブ画面へ。muted=true で未記録の薄い灰色スタイル */
@Composable
fun EntryButton(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    subtitle: String? = null,
    muted: Boolean = false,
) {
    val c = AppTheme.colors
    val dashColor = c.line
    val bgColor = if (muted) Color(0xFFE9EEF3) else c.surface
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (muted) {
                    Modifier.drawBehind {
                        val r = 16.dp.toPx()
                        drawRoundRect(
                            color = dashColor,
                            cornerRadius = CornerRadius(r, r),
                            style = Stroke(
                                width = 1.2.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)),
                            ),
                        )
                    }
                } else Modifier,
            ),
        shape = MaterialTheme.shapes.medium,
        color = bgColor,
        border = if (muted) null else BorderStroke(1.dp, c.line.copy(alpha = 0.7f)),
    ) {
        Row(
            Modifier.padding(PaddingValues(16.dp)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (icon != null) {
                EntryIcon(
                    icon = icon,
                    bg = if (muted) c.empty else c.accentSoft,
                    tint = if (muted) c.muted.copy(alpha = 0.75f) else c.accent,
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (muted) c.muted else c.ink,
                )
                if (subtitle != null) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (muted) c.muted.copy(alpha = 0.8f) else c.muted,
                    )
                }
            }
            Box { Icon(AppIcons.Right, contentDescription = null, modifier = Modifier.size(18.dp), tint = c.muted.copy(alpha = if (muted) 0.65f else 1f)) }
        }
    }
}

/** 入口のアイコン(.entry-ico): 淡い色の角丸の中に置く */
@Composable
fun EntryIcon(icon: ImageVector, bg: Color = AppTheme.colors.accentSoft, tint: Color = AppTheme.colors.accent, box: Dp = 40.dp) {
    Box(
        Modifier.size(box).clip(RoundedCornerShape(12.dp)).background(bg),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(box * 0.55f), tint = tint)
    }
}

/** 右向きの小さな矢印(.chev) */
@Composable
fun Chevron(modifier: Modifier = Modifier) {
    Icon(AppIcons.Right, contentDescription = null, modifier = modifier.size(16.dp), tint = AppTheme.colors.muted)
}
