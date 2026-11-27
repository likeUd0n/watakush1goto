package app.shinsatsunote.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.shinsatsunote.data.Kind
import app.shinsatsunote.domain.level
import app.shinsatsunote.ui.theme.AppTheme
import app.shinsatsunote.ui.theme.Ink

// ---- ボタン ----

/** 主ボタン(.btn) */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    iconEnd: Boolean = false,
) = PillButton(text, onClick, modifier, icon, enabled, AppTheme.colors.accent, AppTheme.colors.surface, iconEnd)

/** 控えめなボタン(.btn.sub) */
@Composable
fun SubButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null) =
    PillButton(text, onClick, modifier, icon, true, AppTheme.colors.paper, AppTheme.colors.ink)

@Composable
private fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier,
    icon: ImageVector?,
    enabled: Boolean,
    bg: Color,
    fg: Color,
    iconEnd: Boolean = false,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 48.dp),
        shape = RoundedCornerShape(999.dp),
        color = if (enabled) bg else AppTheme.colors.line,
        contentColor = fg,
    ) {
        Row(
            Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null && !iconEnd) {
                Icon(icon, null, Modifier.size(18.dp), tint = fg)
                Spacer(Modifier.width(6.dp))
            }
            Text(text, style = MaterialTheme.typography.labelLarge, color = fg, maxLines = 1)
            if (icon != null && iconEnd) {
                Spacer(Modifier.width(6.dp))
                Icon(icon, null, Modifier.size(18.dp), tint = fg)
            }
        }
    }
}

enum class Tone { Muted, Accent, Warm }

/** 小さな枠つきボタン(.mini-btn) */
@Composable
fun MiniButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    tone: Tone = Tone.Muted,
) {
    val c = AppTheme.colors
    val fg = when (tone) {
        Tone.Muted -> c.muted
        Tone.Accent -> c.accent
        Tone.Warm -> c.warm
    }
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 44.dp),
        shape = RoundedCornerShape(999.dp),
        color = c.surface,
        border = BorderStroke(1.dp, c.line),
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (icon != null) {
                Icon(icon, null, Modifier.size(16.dp), tint = fg)
                Spacer(Modifier.width(5.dp))
            }
            Text(text, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = fg, maxLines = 1)
        }
    }
}

// ---- 相手のバッジ(.kind) ----
@Composable
fun KindBadge(kind: String, modifier: Modifier = Modifier) {
    val c = AppTheme.colors
    val coun = kind == Kind.COUN
    Text(
        Kind.label(kind),
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (coun) c.warmSoft else c.accentSoft)
            .padding(horizontal = 8.dp),
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = if (coun) c.warm else c.accent,
        maxLines = 1,
    )
}

// ---- ココロの顔・カラダの電池(小さな表示) ----

/** ココロの小さな顔。値が無ければ「-」 */
@Composable
fun MiniMind(value: Double?, size: Dp = 20.dp) {
    if (value == null) {
        Text("-", color = AppTheme.colors.muted, fontSize = 13.sp)
        return
    }
    val v = value.level()
    val fill = AppTheme.colors.mind(v)
    Canvas(Modifier.size(size)) { drawFace(v, fill) }
}

/** 24×24 の座標で顔を描く(モックの miniMind) */
private fun DrawScope.drawFace(v: Int, fill: Color) {
    val s = size.width / 24f
    val curve = listOf(-5f, -2.5f, 0f, 3f, 5.5f)[v - 1]
    drawCircle(fill, radius = 10f * s, center = Offset(12f * s, 12f * s))
    drawCircle(Ink, radius = 1.2f * s, center = Offset(9f * s, 10f * s))
    drawCircle(Ink, radius = 1.2f * s, center = Offset(15f * s, 10f * s))
    val p = Path().apply {
        moveTo(8.5f * s, 15f * s)
        quadraticTo(12f * s, (15f + curve) * s, 15.5f * s, 15f * s)
    }
    drawPath(p, Ink, style = Stroke(width = 1.6f * s, cap = StrokeCap.Round))
}

/** カラダの小さな電池。値が無ければ「-」 */
@Composable
fun MiniBattery(value: Double?, height: Dp = 20.dp) {
    if (value == null) {
        Text("-", color = AppTheme.colors.muted, fontSize = 13.sp)
        return
    }
    val n = value.level()
    val c = AppTheme.colors
    val col = c.level(n)
    Canvas(Modifier.size(width = height * 1.4f, height = height)) {
        val s = size.height / 20f
        drawRoundRect(
            Ink, topLeft = Offset(1f * s, 3f * s), size = Size(23f * s, 14f * s),
            cornerRadius = CornerRadius(3f * s), style = Stroke(1.5f * s),
        )
        for (i in 0 until 5) {
            drawRoundRect(
                if (i < n) col else c.empty,
                topLeft = Offset((2f + i * 4.2f + 0.4f) * s, 5f * s),
                size = Size(3.2f * s, 10f * s),
                cornerRadius = CornerRadius(1f * s),
            )
        }
        drawRoundRect(Ink, topLeft = Offset(24.5f * s, 7f * s), size = Size(2.5f * s, 6f * s), cornerRadius = CornerRadius(1f * s))
    }
}

// ---- シート ----

/** 下から出るシート(.sheet)。見出しと中身。中身は長ければスクロールする */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSheet(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = AppTheme.colors.surface,
        shape = MaterialTheme.shapes.large.copy(bottomStart = CornerSize0, bottomEnd = CornerSize0),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
            content()
        }
    }
}

private val CornerSize0 = androidx.compose.foundation.shape.CornerSize(0.dp)

/** シートの下のボタンの並び(.actions)。右寄せ */
@Composable
fun SheetActions(modifier: Modifier = Modifier, start: @Composable () -> Unit = {}, content: @Composable () -> Unit) {
    Row(
        modifier.fillMaxWidth().padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        start()
        Spacer(Modifier.weight(1f))
        content()
    }
}

// ---- セグメント(.seg) ----
@Composable
fun <T> Segmented(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = AppTheme.colors
    Row(
        modifier
            .clip(RoundedCornerShape(999.dp))
            .background(c.seg)
            .padding(3.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEach { (value, label) ->
            val on = value == selected
            Box(
                Modifier
                    .weight(1f)
                    .heightIn(min = 40.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (on) c.surface else Color.Transparent)
                    .selectable(selected = on, role = Role.RadioButton) { onSelect(value) }
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (on) c.ink else c.muted, maxLines = 1)
            }
        }
    }
}

// ---- 文の入力欄(textarea) ----
@Composable
fun NoteField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    minLines: Int = 4,
    autoFocus: Boolean = false,
) {
    val c = AppTheme.colors
    val focus = remember { FocusRequester() }
    if (autoFocus) LaunchedEffect(Unit) { focus.requestFocus() }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        // 見出しは呼ぶ側で示すので、読み上げ用の名前だけ付ける
        modifier = modifier.fillMaxWidth().focusRequester(focus).semantics { contentDescription = label },
        placeholder = placeholder?.let { { Text(it, color = c.muted) } },
        textStyle = MaterialTheme.typography.bodyLarge,
        minLines = minLines,
        maxLines = 12,
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = Color(0xFF8A96A3),
            focusedBorderColor = c.accent,
            unfocusedContainerColor = c.surface,
            focusedContainerColor = c.surface,
        ),
    )
}

// ---- 相談窓口(.crisis) ----
@Composable
fun CrisisBox(modifier: Modifier = Modifier, showTitle: Boolean = true) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AppTheme.colors.warmSoft)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (showTitle) Text("ひとりで抱えないでください", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text("つらい気持ちを話せる窓口があります。", style = MaterialTheme.typography.bodyMedium)
        Text("いのちの電話 0120-783-556", style = MaterialTheme.typography.bodyMedium)
        Text("よりそいホットライン 0120-279-338(24時間)", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun CrisisSheet(onDismiss: () -> Unit) {
    AppSheet("ひとりで抱えないでください", onDismiss) {
        CrisisBox(showTitle = false)
        SheetActions { SubButton("閉じる", onDismiss) }
    }
}

// ---- お知らせ(.toast) ----
@Composable
fun AppSnackbar(data: SnackbarData) {
    val c = AppTheme.colors
    Snackbar(
        modifier = Modifier.padding(horizontal = 16.dp),
        shape = RoundedCornerShape(999.dp),
        containerColor = c.ink,
        contentColor = c.surface,
        action = data.visuals.actionLabel?.let { label ->
            {
                TextButton(onClick = data::performAction) {
                    Text(label, color = c.accentSoft, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissAction = {
            TextButton(onClick = data::dismiss, contentPadding = PaddingValues(0.dp)) {
                Text("×", color = c.surface.copy(alpha = 0.75f), fontSize = 18.sp)
            }
        },
    ) { Text(data.visuals.message, color = c.surface) }
}

/** カードの見出し(.card h2) */
@Composable
fun CardTitle(text: String, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    Row(modifier.semantics { heading() }, verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            Icon(icon, null, Modifier.size(17.dp), tint = AppTheme.colors.muted)
            Spacer(Modifier.width(6.dp))
        }
        Text(text, style = MaterialTheme.typography.titleSmall)
    }
}

/** 空のときの文(.empty) */
@Composable
fun EmptyText(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier = modifier, style = MaterialTheme.typography.bodySmall)
}

/** 件数表示: 日本語フォントの上下余白を除き、隣の＋ボタンと視覚的に中央を揃える */
@Composable
fun CountText(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.bodySmall.copy(
            lineHeight = 13.sp,
            platformStyle = PlatformTextStyle(includeFontPadding = false),
            lineHeightStyle = LineHeightStyle(
                alignment = LineHeightStyle.Alignment.Center,
                trim = LineHeightStyle.Trim.Both,
            ),
        ),
    )
}
