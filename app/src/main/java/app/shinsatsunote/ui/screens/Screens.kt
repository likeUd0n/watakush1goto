package app.shinsatsunote.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.shinsatsunote.ui.components.AppCard
import app.shinsatsunote.ui.components.AppIconButton
import app.shinsatsunote.ui.components.EntryButton
import app.shinsatsunote.ui.components.SubTopRow
import app.shinsatsunote.ui.components.TopRow
import app.shinsatsunote.ui.icons.AppIcons

// 画面の枠だけ。中身は課題3〜8で作る

/** 画面の共通の枠(main + .view): 左右 16dp、上 12dp、要素の間 12dp */
@Composable
fun ScreenColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}
