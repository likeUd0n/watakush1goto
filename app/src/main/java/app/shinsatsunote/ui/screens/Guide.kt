package app.shinsatsunote.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.shinsatsunote.ui.components.PrimaryButton
import app.shinsatsunote.ui.components.SubButton
import app.shinsatsunote.ui.theme.AppTheme
import kotlin.math.roundToInt

/** コーチマークでハイライトする UI 要素の識別子 */
object CoachId {
    const val SLEEP = "sleep"
    const val CONDITION = "condition"
    const val HISTORY = "history"
    const val TALK = "talk"
    const val VISIT_TAB = "visit_tab"
    const val RECORD_GUIDE = "record_guide"
    const val VISIT_SEG = "visit_seg"
    const val VISIT_CAL = "visit_cal"
    const val VISIT_MEMO = "visit_memo"
    const val VISIT_GUIDE = "visit_guide"
}

/** コーチマークの1ステップ。targetId が null なら中央カードとして表示 */
data class CoachStep(
    val targetId: String?,
    val title: String,
    val body: String,
)

/** 要素の画面上座標をコーチマーク用に登録する Modifier */
fun Modifier.coachTarget(id: String, targets: SnapshotStateMap<String, Rect>): Modifier = composed {
    onGloballyPositioned { coords ->
        targets[id] = coords.boundsInRoot()
    }
}

/** 記録タブのコーチマーク手順 */
fun recordCoachSteps(firstLaunch: Boolean, hasVisitTab: Boolean): List<CoachStep> = buildList {
    if (firstLaunch) {
        add(
            CoachStep(
                targetId = null,
                title = "わたくしごと へようこそ",
                body = "調子と睡眠を記録して、診察やカウンセリングで先生に見せる1画面にまとめます。\n\n通知はありません。書ける日だけで大丈夫です。記録はスマホの中だけに保存されます。",
            ),
        )
    }
    add(
        CoachStep(
            targetId = CoachId.SLEEP,
            title = "睡眠の記録",
            body = "寝た・起きた時刻や二度寝を記録できます。未記録のときは薄い灰色で表示されます。",
        ),
    )
    add(
        CoachStep(
            targetId = CoachId.CONDITION,
            title = "調子の記録",
            body = "ココロとカラダを5段階とひとことで残します。1日に何度でも記録でき、押すとあとから直せます。",
        ),
    )
    add(
        CoachStep(
            targetId = CoachId.HISTORY,
            title = "振り返り",
            body = "月ごとのグラフや週ごとの一覧で、調子と睡眠の推移をゆっくり振り返れます。",
        ),
    )
    if (hasVisitTab) {
        add(
            CoachStep(
                targetId = CoachId.TALK,
                title = "話すこと",
                body = "診察やカウンセリングで伝えたいことを書きためておけます。右の「＋」からすぐ追加できます。",
            ),
        )
        add(
            CoachStep(
                targetId = CoachId.VISIT_TAB,
                title = "診察・カウンセリングへ",
                body = "書きためたメモをAIで整理したり、先生に見せるまとめ画面を開けます。このガイドは右上の「？」からいつでも見直せます。",
            ),
        )
    } else {
        add(
            CoachStep(
                targetId = CoachId.RECORD_GUIDE,
                title = "ナビゲーションガイド",
                body = "使い方に迷ったら、右上の「？」からいつでもこの案内を開き直せます。",
            ),
        )
    }
}

/** 診察タブのコーチマーク手順 */
fun visitCoachSteps(): List<CoachStep> = listOf(
    CoachStep(
        targetId = CoachId.VISIT_SEG,
        title = "「準備」と「見せる」",
        body = "「準備」で話すことやAIの整理を整え、診察室では「見せる」に切り替えて先生にそのまま見せられます。",
    ),
    CoachStep(
        targetId = CoachId.VISIT_CAL,
        title = "次の予定",
        body = "次の診察やカウンセリングの日時を登録しておくと、予定日に記録タブでお知らせが出ます。",
    ),
    CoachStep(
        targetId = CoachId.VISIT_MEMO,
        title = "話すこととAIで整理",
        body = "メモを押すと編集や診察・カウンセリングの指定ができます。「AIで整理」で手持ちのAIに要約を頼めます。",
    ),
    CoachStep(
        targetId = CoachId.VISIT_GUIDE,
        title = "ナビゲーションガイド",
        body = "使い方に迷ったら、右上の「？」からいつでもこの案内を開き直せます。",
    ),
)

/**
 * 実際の画面要素をハイライトして案内するコーチマーク型ナビゲーションガイド。
 * 画面全体を半透明の幕で覆い、対象要素の周囲だけを明るく切り抜いてツールチップを出す。
 */
@Composable
fun CoachMarkOverlay(
    steps: List<CoachStep>,
    targets: Map<String, Rect>,
    onFinish: () -> Unit,
) {
    if (steps.isEmpty()) return
    var index by remember(steps) { mutableIntStateOf(0) }
    val safeIndex = index.coerceIn(0, steps.lastIndex)
    val step = steps[safeIndex]
    val rect = step.targetId?.let { targets[it] }
    val c = AppTheme.colors
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    if (safeIndex < steps.lastIndex) index = safeIndex + 1 else onFinish()
                },
            ),
    ) {
        val screenHeightPx = with(density) { maxHeight.toPx() }
        val padPx = with(density) { 6.dp.toPx() }
        val radiusPx = with(density) { 16.dp.toPx() }

        // 背景の暗い幕とターゲットの切り抜き
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen },
        ) {
            drawRect(Color.Black.copy(alpha = 0.62f))
            if (rect != null && rect.width > 0f && rect.height > 0f) {
                val left = (rect.left - padPx).coerceAtLeast(0f)
                val top = (rect.top - padPx).coerceAtLeast(0f)
                val width = (rect.width + padPx * 2).coerceAtMost(size.width - left)
                val height = (rect.height + padPx * 2).coerceAtMost(size.height - top)
                drawRoundRect(
                    color = Color.Transparent,
                    topLeft = Offset(left, top),
                    size = Size(width, height),
                    cornerRadius = CornerRadius(radiusPx, radiusPx),
                    blendMode = BlendMode.Clear,
                )
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.92f),
                    topLeft = Offset(left, top),
                    size = Size(width, height),
                    cornerRadius = CornerRadius(radiusPx, radiusPx),
                    style = Stroke(width = 2.dp.toPx()),
                )
            }
        }

        // ツールチップカードの配置: 上下の空きを見て収まる側へ置き、縦長ターゲットでも画面内にクランプする
        var cardHeightPx by remember(safeIndex) { mutableIntStateOf(0) }
        val gapPx = with(density) { 12.dp.toPx() }
        val safeTopPx = with(density) { 48.dp.toPx() }
        val safeBottomPx = with(density) { 56.dp.toPx() }
        val cardAlignment = if (rect == null) Alignment.Center else Alignment.TopCenter
        val yOffsetPx = if (rect == null) {
            0
        } else {
            val estHeight = if (cardHeightPx > 0) cardHeightPx.toFloat() else with(density) { 210.dp.toPx() }
            val belowTop = rect.bottom + padPx + gapPx
            val aboveTop = rect.top - padPx - gapPx - estHeight
            val minTop = safeTopPx
            val maxTop = (screenHeightPx - safeBottomPx - estHeight).coerceAtLeast(minTop)
            val spaceBelow = screenHeightPx - safeBottomPx - (rect.bottom + padPx)
            val spaceAbove = (rect.top - padPx) - safeTopPx
            val rawTop = when {
                spaceBelow >= estHeight + gapPx -> belowTop
                spaceAbove >= estHeight + gapPx -> aboveTop
                spaceBelow >= spaceAbove -> belowTop
                else -> aboveTop
            }
            rawTop.coerceIn(minTop, maxTop).roundToInt()
        }

        Surface(
            modifier = Modifier
                .align(cardAlignment)
                .offset { IntOffset(0, yOffsetPx) }
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .onSizeChanged { cardHeightPx = it.height }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ),
            shape = RoundedCornerShape(18.dp),
            color = c.surface,
            border = BorderStroke(1.dp, c.line),
            shadowElevation = 8.dp,
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "ナビゲーションガイド",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = c.accent,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        "${safeIndex + 1} / ${steps.size}",
                        fontSize = 12.sp,
                        color = c.muted,
                    )
                }
                Text(step.title, style = MaterialTheme.typography.titleMedium, fontSize = 17.sp)
                Text(step.body, style = MaterialTheme.typography.bodyMedium, lineHeight = 22.sp)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TextButton(onClick = onFinish) {
                        Text("スキップ", color = c.muted, fontSize = 14.sp)
                    }
                    Spacer(Modifier.weight(1f))
                    if (safeIndex > 0) {
                        SubButton("前へ", { index = safeIndex - 1 })
                    }
                    PrimaryButton(
                        text = if (safeIndex == steps.lastIndex) "はじめる" else "次へ",
                        onClick = {
                            if (safeIndex < steps.lastIndex) index = safeIndex + 1 else onFinish()
                        },
                    )
                }
            }
        }
    }
}
