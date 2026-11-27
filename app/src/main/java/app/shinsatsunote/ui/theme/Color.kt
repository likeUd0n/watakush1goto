package app.shinsatsunote.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// v19 モック(docs/reference/prototype-mvp-v19.html)の CSS 変数と同じ色
val Paper = Color(0xFFEEF1F4)
val Surface = Color(0xFFFFFFFF)
val Ink = Color(0xFF1D2733)
val Muted = Color(0xFF4E5B69)
val Line = Color(0xFFD5DCE3)
val Seg = Color(0xFFE1E6EB)
val Accent = Color(0xFF2E5A88)
val AccentSoft = Color(0xFFDCE6F1)
val Warm = Color(0xFFB0623A)
val WarmSoft = Color(0xFFF3E2D8)
val Other = Color(0xFFE7E3DA)
val Body = Color(0xFF4C9A6A)
val Nap = Color(0xFFD9A441)

// ココロ・カラダの5段階(1 = つらい 〜 5 = よい)の色
val Level1 = Color(0xFFC0573B)
val Level2 = Color(0xFFCF8A3F)
val Level3 = Color(0xFFB8A845)
val Level4 = Color(0xFF7FA65A)
val Level5 = Color(0xFF4C9A6A)

// ココロの顔の5段階の色(目に優しい淡いパステル調)
val Mind1 = Color(0xFFA7ACC9)
val Mind2 = Color(0xFFA4BED9)
val Mind3 = Color(0xFFA6CBC8)
val Mind4 = Color(0xFFC2D7A4)
val Mind5 = Color(0xFFF0D488)
val Empty = Color(0xFFDCE2E8)

/** Material3 の色の枠に収まらない、アプリ独自の色 */
@Immutable
data class AppColors(
    val paper: Color = Paper,
    val surface: Color = Surface,
    val ink: Color = Ink,
    val muted: Color = Muted,
    val line: Color = Line,
    val seg: Color = Seg,
    val accent: Color = Accent,
    val accentSoft: Color = AccentSoft,
    val warm: Color = Warm,
    val warmSoft: Color = WarmSoft,
    val other: Color = Other,
    val body: Color = Body,
    val nap: Color = Nap,
    val levels: List<Color> = listOf(Level1, Level2, Level3, Level4, Level5),
    val minds: List<Color> = listOf(Mind1, Mind2, Mind3, Mind4, Mind5),
    val empty: Color = Empty,
) {
    /** カラダ(電池)の5段階の値(1〜5)の色 */
    fun level(value: Int): Color = levels[(value - 1).coerceIn(0, levels.lastIndex)]

    /** ココロ(顔)の5段階の値(1〜5)の色 */
    fun mind(value: Int): Color = minds[(value - 1).coerceIn(0, minds.lastIndex)]
}

val LocalAppColors = staticCompositionLocalOf { AppColors() }
