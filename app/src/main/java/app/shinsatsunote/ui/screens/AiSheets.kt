package app.shinsatsunote.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.shinsatsunote.domain.AiPrompt
import app.shinsatsunote.ui.AppViewModel
import app.shinsatsunote.ui.components.AppSheet
import app.shinsatsunote.ui.components.Chevron
import app.shinsatsunote.ui.components.CrisisBox
import app.shinsatsunote.ui.components.MiniButton
import app.shinsatsunote.ui.components.NoteField
import app.shinsatsunote.ui.components.PrimaryButton
import app.shinsatsunote.ui.components.SheetActions
import app.shinsatsunote.ui.components.SubButton
import app.shinsatsunote.ui.icons.AppIcons
import app.shinsatsunote.ui.theme.AppTheme
import kotlinx.coroutines.delay

/** 初回のみ表示する「AIで整理」の使い方チュートリアル */
@Composable
fun AiTutorialSheet(
    onContinue: () -> Unit,
    onDismiss: () -> Unit,
) {
    val c = AppTheme.colors
    AppSheet("AIで整理の使い方", onDismiss) {
        Text(
            "手持ちのAIアプリ（ChatGPTやGeminiなど）を使って、話すことを診察室で見せやすくまとめます。",
            style = MaterialTheme.typography.bodyMedium,
        )
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            TutorialStep(
                num = "1",
                title = "AIへ送る",
                body = "次の画面で「コピー」または「アプリ」から、まとめた文章をAIアプリへ渡します。",
            )
            TutorialStep(
                num = "2",
                title = "AIの返答をコピー",
                body = "AIが診察・カウンセリング向けに整理した返答をコピーします。",
            )
            TutorialStep(
                num = "3",
                title = "「答えを貼る」に保存",
                body = "このアプリに戻って「答えを貼る」を押すと、先生に見せる画面に自動で反映されます。",
            )
        }
        Text(
            "※ このアプリ自体はネットにつなぎません。渡した文章は選んだAIアプリの会社に送られます。",
            fontSize = 12.sp,
            lineHeight = 18.sp,
            color = c.muted,
        )
        SheetActions {
            SubButton("やめる", onDismiss)
            PrimaryButton("わかった・続ける", onContinue)
        }
    }
}

@Composable
private fun TutorialStep(num: String, title: String, body: String) {
    val c = AppTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(c.paper)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(c.accent),
            contentAlignment = Alignment.Center,
        ) {
            Text(num, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = c.surface)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = c.ink)
            Text(body, fontSize = 13.sp, lineHeight = 19.sp, color = c.muted)
        }
    }
}

/**
 * AIアプリへ渡す文の確認。全文を見てから「コピー」か「アプリ」(Androidの共有)で渡す。
 * 共有で長文が欠けるアプリがありうるので、コピーを主ボタンにしておく
 */
@Composable
fun AiShareSheet(
    vm: AppViewModel,
    prompt: AiPrompt,
    onShowTutorial: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val c = AppTheme.colors
    var previewOpen by remember { mutableStateOf(false) }
    AppSheet("AIアプリへ渡す", onDismiss) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(c.accentSoft)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "① AIへ送る → ② 返答をコピー → ③ 「答えを貼る」へ",
                fontSize = 13.sp,
                lineHeight = 19.sp,
                color = c.accent,
                modifier = Modifier.weight(1f),
            )
            MiniButton("手順", onShowTutorial, icon = AppIcons.Help)
        }
        if (prompt.excluded > 0) {
            Text("${prompt.excluded}件の文は外しました。", style = MaterialTheme.typography.bodySmall)
            CrisisBox()
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(c.paper),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp)
                    .clickable { previewOpen = !previewOpen }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    if (previewOpen) "渡す文章を閉じる" else "渡す文章を確認する（${prompt.text.length}文字）",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = c.accent,
                    modifier = Modifier.weight(1f),
                )
                Chevron(Modifier.rotate(if (previewOpen) 90f else 0f))
            }
            AnimatedVisibility(visible = previewOpen) {
                Text(
                    prompt.text,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 260.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
                        .semantics { contentDescription = "AIに渡す文" },
                )
            }
        }
        Text("AIアプリに渡した内容は、そのAIの会社に送られます。", style = MaterialTheme.typography.bodySmall)
        SheetActions {
            SubButton("やめる", onDismiss)
            SubButton("アプリ", {
                val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, prompt.text)
                context.startActivity(Intent.createChooser(send, "AIアプリを選ぶ"))
                onDismiss()
            })
            PrimaryButton("コピー", {
                copyText(context, prompt.text)
                vm.toast("コピーしました")
                onDismiss()
            })
        }
    }
}

/** AIの答えを貼るシート。クリップボードに(渡した文ではない)文があれば最初から入れておく */
@Composable
fun AiPasteSheet(vm: AppViewModel, kinds: List<String>, lastPrompt: String?, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var text by remember { mutableStateOf("") }
    // Android 10 以降は、画面(シート)に入力のフォーカスが来てからでないとクリップボードを読めないので、少し待って読む
    LaunchedEffect(Unit) {
        delay(300)
        if (text.isEmpty()) clipboardText(context)?.takeIf { it.isNotBlank() && it != lastPrompt }?.let { text = it }
    }
    AppSheet("AIの答えを貼る", onDismiss) {
        NoteField(text, { text = it }, label = "AIの答え", minLines = 6, autoFocus = true)
        SheetActions {
            SubButton("やめる", onDismiss)
            PrimaryButton("保存", { vm.saveAiAnswer(text.trim(), kinds); onDismiss() }, enabled = text.isNotBlank())
        }
    }
}

private fun copyText(context: Context, text: String) {
    context.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText("AIに渡す文", text))
}

private fun clipboardText(context: Context): String? =
    context.getSystemService(ClipboardManager::class.java)?.primaryClip?.takeIf { it.itemCount > 0 }
        ?.getItemAt(0)?.coerceToText(context)?.toString()
