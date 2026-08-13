package com.bashkevich.tennisscorekeeperbackend.feature.theme

import de.androidpit.colorthief.ColorThief
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.math.sqrt

/**
 * Этап 1 конвейера `/themes/ai`: палитра цветов изображения + частота каждого цвета.
 *
 * Квантует изображение через Color Thief (MMCQ median-cut) в набор репрезентативных цветов,
 * затем проходит по реальным пикселям и считает долю каждого цвета ([ClusterInfo.share]).
 * Результат (hex + частота) подаётся на вход LLM (GPT-5.4), которая уже классифицирует,
 * какой цвет к какому слоту темы относится (этап 2 — [ThemeService.generateThemeFromImage]).
 *
 * Портирован из референс-проекта ScoreboardThemeRecognizer (ScoreboardPalette.jvm.kt +
 * ScoreboardZones.kt). Без обрезки фона: ожидается уже кропнутый скриншот табло.
 *
 * Color Thief — `java.awt.image.BufferedImage`, поэтому запускать только в headless-AWT
 * (см. `-Djava.awt.headless=true` в build.gradle.kts).
 */
object PaletteExtractor {

    /** Сколько репрезентативных цветов должен вернуть Color Thief. Достаточно много, чтобы
     *  всплыли редкие акценты (индикатор подачи, серый текст проигрыша), которые маленькая
     *  палитра съела бы при мёрдже. */
    private const val PALETTE_COLOR_COUNT = 16

    /** Шаг сэмплинга пикселей в самом Color Thief (1 = каждый пиксель). 10 — дефолт библиотеки. */
    private const val PALETTE_QUALITY = 10

    /** Шаг сэмплинга для нашего частотного прохода (2 = каждый второй пиксель). Хватает для
     *  стабильной доли %. */
    private const val FREQUENCY_STEP = 2

    /** Два цвета палитры ближе этой евклидовой RGB-дистанции сливаются в один, чтобы семейство
     *  почти одинаковых оттенков (например, несколько зелёных) схлопывалось в одну строку.
     *  Настраиваемо: поднять — мёрджить агрессивнее, 0.0 — выключить. */
    private const val MERGE_THRESHOLD = 40.0

    /**
     * Извлекает палитру с частотами из байтов изображения. Возвращает цвета, отсортированные
     * по убыванию [ClusterInfo.pixelCount] (самый частый — первый). При невозможности декодировать
     * изображение или пустой палитре — пустой список / исключение.
     */
    fun extract(imageBytes: ByteArray): List<ClusterInfo> {
        require(imageBytes.isNotEmpty()) { "Cannot extract palette from an empty image" }
        val buffered = ImageIO.read(ByteArrayInputStream(imageBytes))
            ?: error("Failed to decode image for palette extraction")
        return paletteWithFrequency(buffered)
    }

    /**
     * Кодирует изображение в PNG — те же байты видит LLM-классификатор на этапе 2. Берёт
     * оригинальный [BufferedImage], полученный в [extract]; для случая, когда на вход приходит
     * произвольный формат, приводим его к единому PNG.
     */
    fun encodeToPng(imageBytes: ByteArray): ByteArray {
        require(imageBytes.isNotEmpty()) { "Cannot encode an empty image" }
        val buffered = ImageIO.read(ByteArrayInputStream(imageBytes))
            ?: error("Failed to decode image for PNG encoding")
        return encodeToPng(buffered)
    }

    private fun encodeToPng(image: BufferedImage): ByteArray {
        val out = ByteArrayOutputStream()
        ImageIO.write(image, "png", out)
        return out.toByteArray()
    }

    /**
     * Color Thief даёт репрезентативные цвета, но без частоты, поэтому мы заново сканируем
     * пиксели и причисляем каждый к ближайшему цвету палитры (squared-Euclidean на RGB — порядок
     * тот же, что и у настоящей дистанции), чтобы посчитать долю каждого цвета. `ignoreWhite = false`:
     * белый текст и его антиалиасинговые оттенки — полноправные члены палитры, а не шум.
     */
    private fun paletteWithFrequency(image: BufferedImage): List<ClusterInfo> {
        val raw = ColorThief.getPalette(image, PALETTE_COLOR_COUNT, PALETTE_QUALITY, false)
            ?: return emptyList()
        // Color Thief (MMCQ) для «разреженных» изображений (число уникальных цветов < colorCount)
        // может вернуть пустые VBox'ы, чей avg-цвет считается по середине диапазона и иногда
        // выходит за 0..255. Такие фиктивные записи отсекаем — реальных пикселей за ними нет.
        val palette = raw.mapNotNull { arr ->
            val r = arr[0].coerceIn(0, 255)
            val g = arr[1].coerceIn(0, 255)
            val b = arr[2].coerceIn(0, 255)
            if (arr[0] in 0..255 && arr[1] in 0..255 && arr[2] in 0..255) RgbColor(r, g, b) else null
        }
        if (palette.isEmpty()) return emptyList()

        val width = image.width
        val height = image.height
        val argb = image.getRGB(0, 0, width, height, null, 0, width)

        val counts = IntArray(palette.size)
        var total = 0
        var i = 0
        while (i < argb.size) {
            val r = (argb[i] shr 16) and 0xFF
            val g = (argb[i] shr 8) and 0xFF
            val b = argb[i] and 0xFF
            var bestIdx = 0
            var bestDist = Int.MAX_VALUE
            for (j in palette.indices) {
                val p = palette[j]
                val dr = r - p.r
                val dg = g - p.g
                val db = b - p.b
                val d = dr * dr + dg * dg + db * db
                if (d < bestDist) {
                    bestDist = d
                    bestIdx = j
                }
            }
            counts[bestIdx]++
            total++
            i += FREQUENCY_STEP
        }

        val merged = mergeCloseColors(
            entries = palette.mapIndexed { idx, c -> c to counts[idx] },
            threshold = MERGE_THRESHOLD,
        )
        val denom = if (total > 0) total.toDouble() else 1.0
        return merged.map { (color, count) ->
            ClusterInfo(centroid = color, pixelCount = count, share = count / denom)
        }.sortedByDescending { it.pixelCount }
    }

    /**
     * Сливает цвета палитры, чья RGB-дистанция укладывается в [threshold] (жадно, от самого
     * populous). Каждый цвет примыкает к первому существующему кластеру в пределах порога, иначе
     * создаёт новый; представитель кластера — count-weighted average участников, счётчик — их сумма,
     * поэтому доли цветов всё равно суммируются в 1. Нулевые записи (цвет, к которому не отнеслось
     * ни одного пикселя) отбрасываются.
     */
    private fun mergeCloseColors(
        entries: List<Pair<RgbColor, Int>>,
        threshold: Double,
    ): List<Pair<RgbColor, Int>> {
        val clusters = ArrayList<ColorAccumulator>()
        for ((color, count) in entries.filter { it.second > 0 }.sortedByDescending { it.second }) {
            val target = clusters.firstOrNull { it.centroid.distanceTo(color) <= threshold }
            if (target != null) target.add(color, count) else clusters.add(ColorAccumulator(color, count))
        }
        return clusters.map { it.centroid to it.count.toInt() }
    }

    /** Накопитель RGB по одному кластеру мёрджа, count-weighted. */
    private class ColorAccumulator(color: RgbColor, count: Int) {
        private val seed: RgbColor = color
        private var sumR = color.r.toDouble() * count
        private var sumG = color.g.toDouble() * count
        private var sumB = color.b.toDouble() * count
        private var pixelCount: Long = count.toLong()
        val count: Long get() = pixelCount
        val centroid: RgbColor
            get() = if (pixelCount == 0L) seed else RgbColor(
                (sumR / pixelCount).toInt().coerceIn(0, 255),
                (sumG / pixelCount).toInt().coerceIn(0, 255),
                (sumB / pixelCount).toInt().coerceIn(0, 255),
            )

        fun add(color: RgbColor, count: Int) {
            sumR += color.r.toDouble() * count
            sumG += color.g.toDouble() * count
            sumB += color.b.toDouble() * count
            pixelCount += count.toLong()
        }
    }
}

/**
 * RGB-цвет в 0..255. [centroid] у [ClusterInfo] — **мода** (самый частый цвет) пикселей кластера,
 * а не среднее: так плосшие заливки табло (например, фон #222E57) выходят бит-аккуратными, а не
 * уплывают к антиалиасинговым краям.
 */
data class RgbColor(val r: Int, val g: Int, val b: Int) {
    init {
        require(r in 0..255 && g in 0..255 && b in 0..255) { "RGB components must be in 0..255" }
    }

    fun toHex(): String = "#" + r.toHexByte() + g.toHexByte() + b.toHexByte()

    fun distanceTo(other: RgbColor): Double {
        val dr = (r - other.r).toDouble()
        val dg = (g - other.g).toDouble()
        val db = (b - other.b).toDouble()
        return sqrt(dr * dr + dg * dg + db * db)
    }

    private fun Int.toHexByte(): String {
        val v = coerceIn(0, 255)
        return HEX[v / 16].toString() + HEX[v % 16].toString()
    }

    companion object {
        private val HEX = "0123456789ABCDEF".toCharArray()
    }
}

/** Запись палитры: представитель (центроид) + число пикселей + их доля. */
data class ClusterInfo(
    val centroid: RgbColor,
    val pixelCount: Int,
    val share: Double,
)

/** Парсит `#RRGGBB` / `RRGGBB` в [RgbColor], либо null при некорректной строке. */
fun parseHexColor(hex: String?): RgbColor? {
    if (hex == null) return null
    val s = hex.removePrefix("#").trim()
    if (s.length != 6) return null
    val r = s.substring(0, 2).toIntOrNull(16) ?: return null
    val g = s.substring(2, 4).toIntOrNull(16) ?: return null
    val b = s.substring(4, 6).toIntOrNull(16) ?: return null
    return runCatching { RgbColor(r, g, b) }.getOrNull()
}
