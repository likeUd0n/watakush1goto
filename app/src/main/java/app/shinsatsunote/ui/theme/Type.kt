package app.shinsatsunote.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.shinsatsunote.R

// 本文は読みやすさ重視の BIZ UDPゴシック、見出しは Zen Kaku Gothic New(どちらも OFL。端末に同梱する)
val UiFont = FontFamily(
    Font(R.font.biz_udp_gothic_regular, FontWeight.Normal),
    Font(R.font.biz_udp_gothic_bold, FontWeight.Bold),
)
val HeadFont = FontFamily(
    Font(R.font.zen_kaku_gothic_new_bold, FontWeight.Bold),
)

// モックの rem を sp にする(1rem = 16sp)。行の高さはモックの line-height 1.6
private val base = TextStyle(fontFamily = UiFont, color = Ink, lineHeight = 1.6.em)
private val head = TextStyle(fontFamily = HeadFont, fontWeight = FontWeight.Bold, color = Ink, lineHeight = 1.3.em)

val AppTypography = Typography(
    // 画面の見出し(.top h1: 1.05rem)
    titleLarge = head.copy(fontSize = 17.sp),
    // 入口のカードの見出し(.entry-btn b: .98rem)
    titleMedium = head.copy(fontSize = 16.sp),
    // カードの小見出し(.card h2: .86rem、muted)
    titleSmall = head.copy(fontSize = 14.sp, color = Muted),
    bodyLarge = base.copy(fontSize = 16.sp),
    bodyMedium = base.copy(fontSize = 15.sp),
    // 補足の文字(最小 .8rem)
    bodySmall = base.copy(fontSize = 13.sp, color = Muted),
    // 下のタブのラベル(.tabs a: .8rem、太字)
    labelMedium = base.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold, lineHeight = 1.3.em),
    labelLarge = base.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold, lineHeight = 1.3.em),
)
