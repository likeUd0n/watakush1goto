package app.shinsatsunote.domain

import java.text.Normalizer

/**
 * 自傷・自殺を示す入力の判定(キーワード)。
 * 該当する入力では相談窓口のシートを出し、外部AIに渡す文からは外す。
 *
 * 見逃しを減らすことを優先し、比喩(「死にたいくらい恥ずかしい」)も拾う(安全側に倒す)。
 * 一覧は仮のもの。語の範囲は医療専門家の意見を踏まえて見直す予定。
 */
private const val TA = "た[いかく]" // 「〜たい」の活用(たい / たか(った)/ たく(て・なる))

private val PATTERNS: List<Regex> = listOf(
    Regex("[死し]に$TA|死のう|死んじゃい$TA|死にそう|死んでしまい$TA|死んだほうが|死んだ方が|死んでしまおう|死んでやる"),
    Regex("自殺|自死|自傷|リスカ|リストカット|オーバードーズ|過剰摂取|OD(?![A-Za-z])"),
    Regex("消え$TA|消えてしまい$TA|消えてなくなり$TA|いなくなり$TA|生きていたくな[いかく]|生きるのが(?:つらい|辛い|しんどい)|生きていても仕方|生きて(?:い)?る意味が(?:ない|無い|わからない|分からない)|生きる意味が(?:ない|無い)"),
    Regex("首を?吊|飛び降り|飛び込み(?:$TA|自殺)|手首を切"),
    Regex("楽になりたい.*(?:死|終わ)|(?:終わらせ|終わりにし)たい.*(?:人生|命)|終わりにし$TA|命を絶"),
    Regex("殺してほしい|殺してくれ"),
)

fun looksLikeCrisis(text: String): Boolean {
    // 全角・半角やスペースの揺れで見逃さないよう、空白を除いて正規化する
    val t = Normalizer.normalize(text, Normalizer.Form.NFKC).replace(Regex("\\s+"), "")
    return PATTERNS.any { it.containsMatchIn(t) }
}
