package com.bashkevich.tennisscorekeeperbackend.feature.theme

import com.bashkevich.tennisscorekeeperbackend.model.theme.ThemeColor
import com.bashkevich.tennisscorekeeperbackend.model.theme.ThemeContent
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.math.roundToInt

/**
 * Детерминированный движок извлечения цветовой схемы теннисного табло.
 *
 * Архитектура (см. план шагов 1–4): LLM только **локализует** элементы табло,
 * размечая ячейки сетки R×C ролями (см. [Role]); движок **измеряет** точный цвет
 * по реальным пикселям. LLM никогда не называет цвета — поэтому исчезает
 * «чёрный вместо тёмно-синего»: мы берём самый частый реальный цвет пикселей,
 * а не то, как модель его категоризует.
 *
 * Алгоритм замера — гистограммная мода (НЕ среднее):
 *  - FILL-роли (фоны)         → самый частый кластер палитры;
 *  - GLYPH-роли (текст/индикатор) → самый частый кластер среди пикселей, далёких
 *    от опорного фона (текст занимает меньшинство пикселей ячейки, но это именно
 *    те пиксели, что отличаются от фона);
 *  - serve (индикатор подачи) → кластер, далёкий и от основного фона, и от
 *    основного текста (акцент); если такого нет — фолбэк на основной текст.
 *
 * Опорный фон зависит от роли: для current_set_text это current_set_background,
 * для current_game_text — current_game_background, для остального текста и serve —
 * основной фон. Поэтому порядок вычисления важен: сначала фоны, потом текст.
 *
 * Все цвета возвращаются как `#RRGGBB` (как в контракте /themes/ai).
 */
object ScoreboardColorExtractor {

    init {
        // AWT должен работать в headless-режиме (особенно в Linux/Docker и в тестах).
        // Канонический JVM-флаг добавляется в build.gradle.kts (шаг 6); здесь —
        // страховка, чтобы компонент был самодостаточным в любом окружении.
        System.setProperty("java.awt.headless", "true")
    }

    /** Роли, которыми LLM размечает ячейки сетки. */
    object Role {
        const val BACKGROUND = "background"
        const val NAME_TEXT = "name_text"
        const val PREV_SET_WIN_TEXT = "prev_set_win_text"
        const val PREV_SET_LOSE_TEXT = "prev_set_lose_text"
        const val CURRENT_SET_BG = "current_set_bg"
        const val CURRENT_SET_TEXT = "current_set_text"
        const val CURRENT_GAME_BG = "current_game_bg"
        const val CURRENT_GAME_TEXT = "current_game_text"
        const val SERVE = "serve"
        const val IGNORE = "ignore"
    }

    /**
     * Тюнинг алгоритма. Значения по умолчанию — стартовые; калибруются на golden-сете
     * реальных табло (шаг 5).
     *
     * @property marginFraction какую долю пикселей ячейки отбросить с каждой стороны
     *  (убирает антиалиасинг и смесь цветов на границах элементов).
     * @property quantBits бит на канал при квантизации гистограммы (5 → 32 уровня,
     *  32768 корзинок; достаточно тонко, чтобы navy ≠ black).
     * @property minPixels минимум пикселей у роли, чтобы считать её присутствующей.
     * @property bgTolerance порог (евклидово расстояние в RGB), ближе которого
     *  пиксель считается «фоном» и маскируется при поиске глифа.
     * @property textTolerance порог для serve: пиксель, близкий к основному тексту,
     *  не считается акцентом-индикатором.
     */
    data class Config(
        val marginFraction: Double = 0.10,
        val quantBits: Int = 5,
        val minPixels: Int = 4,
        val bgTolerance: Double = 60.0,
        val textTolerance: Double = 60.0,
    )

    private val DEFAULT_CONFIG = Config()

    /**
     * Рисует поверх изображения сетку [rows]×[cols] с номерами ячеек и возвращает
     * PNG-байты. Эту картинку отдают LLM, чтобы она оперировала номерами ячеек
     * (надёжный grid-паттерн grounding'а для GPT-4o), а не координатами.
     */
    fun drawGridOverlay(imageBytes: ByteArray, rows: Int, cols: Int): ByteArray {
        require(rows > 0 && cols > 0) { "rows and cols must be > 0" }
        val image = decode(imageBytes)
        val w = image.width
        val h = image.height
        val g = image.createGraphics()
        try {
            g.stroke = BasicStroke(1f)
            g.font = Font("SansSerif", Font.PLAIN, 10)
            for (r in 0 until rows) {
                for (c in 0 until cols) {
                    val x0 = c * w / cols
                    val y0 = r * h / rows
                    val x1 = if (c == cols - 1) w else (c + 1) * w / cols
                    val y1 = if (r == rows - 1) h else (r + 1) * h / rows
                    g.color = Color(255, 0, 0, 180)
                    g.drawRect(x0, y0, x1 - x0 - 1, y1 - y0 - 1)
                    g.color = Color(255, 255, 255, 220)
                    g.fillRect(x0 + 1, y0 + 1, 16, 12)
                    g.color = Color.BLACK
                    g.drawString((r * cols + c).toString(), x0 + 2, y0 + 11)
                }
            }
        } finally {
            g.dispose()
        }
        return encode(image)
    }

    /**
     * Измеряет цветовую схему табло по изображению и сетке ролей.
     *
     * @param grid матрица R×C, каждой ячейке сопоставлена роль из [Role]
     *  (строки/пробелы игнорируются, регистр приводится к нижнему).
     * @return готовый [ThemeContent] с применёнными фолбэками.
     * @throws IllegalArgumentException если сетка пустая или не прямоугольная.
     * @throws IllegalStateException если невозможно вычислить обязательный цвет
     *  (основной фон / основной текст).
     */
    fun extract(
        imageBytes: ByteArray,
        grid: List<List<String>>,
        config: Config = DEFAULT_CONFIG,
    ): ThemeContent {
        require(grid.isNotEmpty()) { "Grid must not be empty" }
        val rows = grid.size
        val cols = grid[0].size
        require(cols > 0 && grid.all { it.size == cols }) { "Grid must be rectangular and non-empty" }
        require(config.quantBits in 1..8) { "quantBits must be in 1..8" }

        val image = decode(imageBytes)
        val w = image.width
        val h = image.height

        val histograms = HashMap<String, Histogram>()
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val role = grid[r][c].trim().lowercase()
                if (role.isEmpty() || role == Role.IGNORE) continue
                val cellX0 = c * w / cols
                val cellY0 = r * h / rows
                val cellX1 = if (c == cols - 1) w else (c + 1) * w / cols
                val cellY1 = if (r == rows - 1) h else (r + 1) * h / rows
                val insetX = ((cellX1 - cellX0) * config.marginFraction).toInt()
                val insetY = ((cellY1 - cellY0) * config.marginFraction).toInt()
                val px0 = (cellX0 + insetX).coerceIn(0, w - 1)
                val px1 = (cellX1 - insetX).coerceIn(1, w)
                val py0 = (cellY0 + insetY).coerceIn(0, h - 1)
                val py1 = (cellY1 - insetY).coerceIn(1, h)
                if (px1 <= px0 || py1 <= py0) continue
                val hist = histograms.getOrPut(role) { Histogram(config.quantBits) }
                for (y in py0 until py1) {
                    for (x in px0 until px1) {
                        hist.add(image.getRGB(x, y))
                    }
                }
            }
        }

        val mainBg = fillColor(histograms[Role.BACKGROUND], config)
            ?: error("Cannot determine main_background_color: no 'background' cells with enough pixels")

        val mainText = glyphColor(histograms[Role.NAME_TEXT], mainBg, config)
            ?: glyphColor(histograms[Role.PREV_SET_WIN_TEXT], mainBg, config)
            ?: glyphColor(histograms[Role.PREV_SET_LOSE_TEXT], mainBg, config)
            ?: error("Cannot determine main_text_color: no text cells with enough pixels")

        val serve = serveColor(histograms[Role.SERVE], mainBg, mainText, config) ?: mainText

        val prevWin = glyphColor(histograms[Role.PREV_SET_WIN_TEXT], mainBg, config) ?: mainText
        val prevLose = glyphColor(histograms[Role.PREV_SET_LOSE_TEXT], mainBg, config) ?: mainText

        val curSetBg = fillColor(histograms[Role.CURRENT_SET_BG], config) ?: mainBg
        val curSetText = glyphColor(histograms[Role.CURRENT_SET_TEXT], curSetBg, config) ?: mainText
        val curGameBg = fillColor(histograms[Role.CURRENT_GAME_BG], config) ?: mainBg
        val curGameText = glyphColor(histograms[Role.CURRENT_GAME_TEXT], curGameBg, config) ?: mainText

        return ThemeContent(
            mainBackgroundColor = ThemeColor(hex(mainBg)),
            mainTextColor = ThemeColor(hex(mainText)),
            serveColor = ThemeColor(hex(serve)),
            previousSetWinTextColor = ThemeColor(hex(prevWin)),
            previousSetLoseTextColor = ThemeColor(hex(prevLose)),
            currentSetBackgroundColor = ThemeColor(hex(curSetBg)),
            currentSetTextColor = ThemeColor(hex(curSetText)),
            currentGameBackgroundColor = ThemeColor(hex(curGameBg)),
            currentGameTextColor = ThemeColor(hex(curGameText)),
        )
    }

    /** FILL-роль: самый частый кластер (мода палитры). */
    private fun fillColor(hist: Histogram?, config: Config): Int? {
        if (hist == null || hist.total < config.minPixels) return null
        return hist.best { true }?.center
    }

    /** GLYPH-роль: самый частый кластер среди пикселей, далёких от [refBg]. */
    private fun glyphColor(hist: Histogram?, refBg: Int, config: Config): Int? {
        if (hist == null || hist.total < config.minPixels) return null
        val tol2 = config.bgTolerance * config.bgTolerance
        return hist.best { dist2(it.center, refBg) > tol2 }?.center
    }

    /** serve: самый частый кластер, далёкий и от основного фона, и от основного текста. */
    private fun serveColor(hist: Histogram?, mainBg: Int, mainText: Int, config: Config): Int? {
        if (hist == null || hist.total < config.minPixels) return null
        val bg2 = config.bgTolerance * config.bgTolerance
        val text2 = config.textTolerance * config.textTolerance
        return hist.best { b -> dist2(b.center, mainBg) > bg2 && dist2(b.center, mainText) > text2 }?.center
    }

    private fun dist2(a: Int, b: Int): Long {
        val dr = ((a shr 16) and 0xFF) - ((b shr 16) and 0xFF)
        val dg = ((a shr 8) and 0xFF) - ((b shr 8) and 0xFF)
        val db = (a and 0xFF) - (b and 0xFF)
        return dr.toLong() * dr + dg.toLong() * dg + db.toLong() * db
    }

    private fun hex(rgb: Int): String = "#%06X".format(rgb and 0xFFFFFF)

    private fun decode(bytes: ByteArray): BufferedImage {
        val raw = ImageIO.read(ByteArrayInputStream(bytes)) ?: error("Cannot decode image")
        if (raw.type == BufferedImage.TYPE_INT_RGB) return raw
        // Приводим к TYPE_INT_RGB — единый формат для getRGB и для overlay.
        val out = BufferedImage(raw.width, raw.height, BufferedImage.TYPE_INT_RGB)
        val g = out.createGraphics()
        try {
            g.drawImage(raw, 0, 0, null)
        } finally {
            g.dispose()
        }
        return out
    }

    private fun encode(image: BufferedImage): ByteArray {
        val baos = ByteArrayOutputStream()
        ImageIO.write(image, "png", baos)
        return baos.toByteArray()
    }

    private class Bucket(val key: Int, val count: Long, val center: Int)

    /**
     * Гистограмма цвета региона с квантованием [quantBits] бит/канал.
     * Для каждой корзинки храним количество пикселей и сумму компонент — это даёт
     * точный центр кластера (среднее по пикселям корзинки) вместо огрублённого
     * значения квантования.
     */
    private class Histogram(quantBits: Int) {
        private val shift = 8 - quantBits
        private val gShift = quantBits
        private val rShift = 2 * quantBits

        private val counts = HashMap<Int, Long>()
        private val sumR = HashMap<Int, Long>()
        private val sumG = HashMap<Int, Long>()
        private val sumB = HashMap<Int, Long>()

        var total: Long = 0L
            private set

        fun add(argb: Int) {
            val r = (argb shr 16) and 0xFF
            val g = (argb shr 8) and 0xFF
            val b = argb and 0xFF
            val key = ((r shr shift) shl rShift) or ((g shr shift) shl gShift) or (b shr shift)
            counts.merge(key, 1L) { acc, v -> acc + v }
            sumR.merge(key, r.toLong()) { acc, v -> acc + v }
            sumG.merge(key, g.toLong()) { acc, v -> acc + v }
            sumB.merge(key, b.toLong()) { acc, v -> acc + v }
            total++
        }

        /** Лучшая (макс. количество пикселей) корзинка, удовлетворяющая [predicate]. */
        fun best(predicate: (Bucket) -> Boolean): Bucket? {
            var best: Bucket? = null
            for ((key, count) in counts) {
                if (count == 0L) continue
                val r = (sumR.getValue(key).toDouble() / count).roundToInt()
                val g = (sumG.getValue(key).toDouble() / count).roundToInt()
                val b = (sumB.getValue(key).toDouble() / count).roundToInt()
                val bucket = Bucket(key, count, (r shl 16) or (g shl 8) or b)
                if (!predicate(bucket)) continue
                val cur = best
                if (cur == null || count > cur.count || (count == cur.count && key < cur.key)) {
                    best = bucket
                }
            }
            return best
        }
    }
}
