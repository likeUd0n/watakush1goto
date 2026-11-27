package app.shinsatsunote.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.shinsatsunote.data.AppSettings
import app.shinsatsunote.data.Kind
import app.shinsatsunote.data.hasUsageAccess
import app.shinsatsunote.ui.AppViewModel
import app.shinsatsunote.ui.components.AppCard
import app.shinsatsunote.ui.components.CardTitle
import app.shinsatsunote.ui.components.Chevron
import app.shinsatsunote.ui.components.EmptyText
import app.shinsatsunote.ui.components.SubTopRow
import app.shinsatsunote.ui.theme.AppTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun SettingsScreen(vm: AppViewModel, settings: AppSettings, onBack: () -> Unit) {
    val context = LocalContext.current
    val c = AppTheme.colors
    // 使用状況へのアクセスは設定アプリで変わるので、戻ってきたら読み直す
    var usage by remember { mutableStateOf(hasUsageAccess(context)) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) usage = hasUsageAccess(context) }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let(vm::exportBackup)
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(vm::importBackup)
    }

    ScreenColumn {
        SubTopRow(title = "設定", backLabel = "記録へ戻る", onBack = onBack)

        AppCard {
            CardTitle("使うもの")
            SwitchRow("診察", "予定・記録・医師に見せる推移", settings.useDoc) { vm.setUse(Kind.DOC, it) }
            HorizontalDivider(color = c.line)
            SwitchRow("カウンセリング", "予定・記録・最近の出来事と気持ち", settings.useCoun) { vm.setUse(Kind.COUN, it) }
            if (settings.kinds.isEmpty()) {
                EmptyText("両方オフのあいだは、睡眠と調子の記録だけを使います。話すことやこれまでの記録は消えません。")
            }
        }

        AppCard {
            CardTitle("睡眠時刻の自動入力")
            SetRow("使用状況へのアクセス", if (usage) "許可済み" else "未許可") {
                try {
                    context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                } catch (_: ActivityNotFoundException) {
                    vm.toast("この端末では設定を開けませんでした")
                }
            }
            EmptyText("許可すると、画面がオフだった時間から寝た時刻と起きた時刻を推定して入れておきます。画面のオン・オフの時刻だけを使い、スマホの外には出しません。")
        }

        AppCard {
            CardTitle("バックアップ")
            SetRow("書き出す", null) {
                exporter.launch("watakush1goto-backup-${LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)}.json")
            }
            HorizontalDivider(color = c.line)
            SetRow("読み込む", null) { importer.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }
            EmptyText("記録はこのスマホの中だけにあります。アプリを消すと記録も消えるので、書き出したファイルから戻してください。読み込むと今の記録に足します(同じ記録は増えません)。")
        }
    }
}

@Composable
private fun SwitchRow(title: String, sub: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val c = AppTheme.colors
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(role = Role.Switch) { onChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp)
            Text(sub, fontSize = 13.sp, color = c.muted)
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(checkedTrackColor = c.accent, checkedThumbColor = c.surface),
        )
    }
}

@Composable
private fun SetRow(title: String, value: String?, onClick: () -> Unit) {
    val c = AppTheme.colors
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(onClick = onClick).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(title, fontSize = 15.sp, modifier = Modifier.weight(1f))
        if (value != null) Text(value, fontSize = 14.sp, color = c.muted, fontWeight = FontWeight.Bold)
        Chevron()
    }
}
