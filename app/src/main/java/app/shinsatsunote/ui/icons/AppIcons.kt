package app.shinsatsunote.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * v19 モックの SVG アイコン(24×24、線のみ)をそのまま写したもの。
 * 色は Icon の tint で付けるので、ここでは黒で描く。円と四角はパスに直してある。
 */
object AppIcons {
    val Rec by lazy { icon("rec", "M5 4h14v16H5z", "M9 9h6M9 13h6M9 17h3") }
    val Visit by lazy {
        icon(
            "visit",
            "M12 20s-7-4.4-7-10a4 4 0 0 1 7-2.6A4 4 0 0 1 19 10c0 5.6-7 10-7 10z",
            "M8.5 12h2l1-2 1.5 4 1-2h1.5",
        )
    }
    val Moon by lazy { icon("moon", "M20 14.5A8.5 8.5 0 1 1 9.5 4a7 7 0 0 0 10.5 10.5z") }
    val Smile by lazy { icon("smile", circle(12f, 12f, 9f), "M9 10h.01M15 10h.01M8.5 14.5a4.5 4.5 0 0 0 7 0") }
    val Talk by lazy { icon("talk", "M5 5h14v10H10l-4 4v-4H5z", "M9 9h6M9 12h4") }
    val Plus by lazy { icon("plus", "M12 5v14M5 12h14", width = 2f) }
    val Check by lazy { icon("check", "M5 12l5 5 9-10", width = 2.2f) }
    val Trash by lazy { icon("trash", "M4 7h16M9 7V4h6v3M6 7l1 13h10l1-13") }
    val Gear by lazy {
        icon(
            "gear",
            "M12 8.5a3.5 3.5 0 1 0 0 7 3.5 3.5 0 0 0 0-7z",
            "M19.4 13.5l1.6 1.2-2 3.4-1.9-.7a7 7 0 0 1-1.7 1l-.3 2H11l-.3-2a7 7 0 0 1-1.7-1l-1.9.7-2-3.4 1.6-1.2a7 7 0 0 1 0-2L5 10.3l2-3.4 1.9.7a7 7 0 0 1 1.7-1l.3-2h4.1l.3 2a7 7 0 0 1 1.7 1l1.9-.7 2 3.4-1.6 1.2a7 7 0 0 1 0 2z",
        )
    }
    val Help by lazy {
        icon(
            "help",
            circle(12f, 12f, 9f),
            "M9.5 9.5a2.5 2.5 0 0 1 5 .5c0 1.5-2.5 2-2.5 3.5",
            filled = listOf(circle(12f, 17f, 0.6f)),
        )
    }
    val Cal by lazy { icon("cal", "M6 5h12a2 2 0 0 1 2 2v11a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2z", "M8 3v4M16 3v4M4 10h16") }
    val Left by lazy { icon("left", "M15 5l-7 7 7 7", width = 2f) }
    val Right by lazy { icon("right", "M9 5l7 7-7 7", width = 2f) }
    val Ai by lazy { icon("ai", "M5 6h14M5 12h9M5 18h6", "M17 14l1 2 2 1-2 1-1 2-1-2-2-1 2-1z") }
    val ListIcon by lazy { icon("list", "M8 6h12M8 12h12M8 18h12M4 6h.01M4 12h.01M4 18h.01") }
    val Chart by lazy { icon("chart", "M4 19h16", "M5 14l4-4 4 3 6-7") }
    val Close by lazy { icon("close", "M6 6l12 12M18 6L6 18", width = 2f) }

    private fun circle(cx: Float, cy: Float, r: Float): String =
        "M${cx - r} ${cy}a$r $r 0 1 0 ${2 * r} 0a$r $r 0 1 0 ${-2 * r} 0z"

    private fun icon(
        name: String,
        vararg strokes: String,
        width: Float = 1.8f,
        filled: List<String> = emptyList(),
    ): ImageVector = ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        strokes.forEach { d ->
            addPath(
                pathData = addPathNodes(d),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = width,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
        filled.forEach { d -> addPath(pathData = addPathNodes(d), fill = SolidColor(Color.Black)) }
    }.build()
}
