package com.bashkevich.tennisscorekeeperbackend.feature.theme

import com.bashkevich.tennisscorekeeperbackend.feature.theme.ScoreboardColorExtractor.Role
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
import kotlin.test.assertFailsWith

/**
 * Unit-тесты детерминированного движка [ScoreboardColorExtractor] на синтетических картинках.
 *
 * Никакого LLM, сети или БД: картинка рисуется прямо в тесте, роли расставляются вручную,
 * и мы проверяем, что движок измеряет точный цвет по пикселям. Это закрепляет главный
 * инвариант архитектуры — цвет всегда измеряется кодом, а не называется моделью.
 */
class ScoreboardColorExtractorTest {

    private val bg = 0x1A2B5C          // navy — та самая «тёмно-синий vs чёрный» претензия
    private val mainText = 0xFFFFFF    // белый
    private val serve = 0xFFD700       // жёлтый индикатор подачи
    private val prevWin = 0x4ADE80     // ярко-зелёный (выигранный сет)
    private val prevLose = 0xEF4444    // красный (проигранный сет)
    private val curSetBg = 0x14B8A6    // бирюзовый фон текущего сета
    private val curSetText = 0x000000  // чёрный текст текущего сета
    private val curGameBg = 0xF97316   // оранжевый фон текущего гейма
    private val curGameText = 0x000000 // чёрный текст текущего гейма

    /** Полное табло: все 9 цветов измерены точно. */
    @Test
    fun `extract measures every color from a full synthetic scoreboard`() {
        val (image, grid) = renderScoreboard(
            bg = bg,
            placements = mapOf(
                // верхняя строка игрока
                (0 to 1) to (Role.SERVE to serve),
                (0 to 0) to (Role.NAME_TEXT to mainText),
                (0 to 4) to (Role.PREV_SET_WIN_TEXT to prevWin),
                (0 to 5) to (Role.CURRENT_SET_BG to curSetBg),
                (0 to 6) to (Role.CURRENT_SET_TEXT to curSetText),
                (0 to 7) to (Role.CURRENT_GAME_BG to curGameBg),
                (0 to 8) to (Role.CURRENT_GAME_TEXT to curGameText),
                // нижняя строка игрока
                (1 to 0) to (Role.NAME_TEXT to mainText),
                (1 to 4) to (Role.PREV_SET_LOSE_TEXT to prevLose),
            )
        )

        val theme = ScoreboardColorExtractor.extract(image, grid)

        assertEquals(hex(bg), theme.mainBackgroundColor.color)
        assertEquals(hex(mainText), theme.mainTextColor.color)
        assertEquals(hex(serve), theme.serveColor.color)
        assertEquals(hex(prevWin), theme.previousSetWinTextColor.color)
        assertEquals(hex(prevLose), theme.previousSetLoseTextColor.color)
        assertEquals(hex(curSetBg), theme.currentSetBackgroundColor.color)
        assertEquals(hex(curSetText), theme.currentSetTextColor.color)
        assertEquals(hex(curGameBg), theme.currentGameBackgroundColor.color)
        assertEquals(hex(curGameText), theme.currentGameTextColor.color)
    }

    /**
     * Ключевая претензия пользователя: тёмно-синий фон не должен превращаться в чёрный.
     * Движок берёт моду реальных пикселей, поэтому navy остаётся navy.
     */
    @Test
    fun `navy background is not averaged into black`() {
        val (image, grid) = renderScoreboard(
            bg = bg,
            placements = mapOf((0 to 0) to (Role.NAME_TEXT to mainText))
        )

        val theme = ScoreboardColorExtractor.extract(image, grid)

        assertEquals(hex(bg), theme.mainBackgroundColor.color)
        assertNotEquals("#000000", theme.mainBackgroundColor.color)
    }

    /** Нет подсвеченных ячеек текущего сета/гейма → фон бэк-фолбэк на основной, текст — на основной текст. */
    @Test
    fun `missing highlight cells fall back to base colors`() {
        val (image, grid) = renderScoreboard(
            bg = bg,
            placements = mapOf(
                (0 to 0) to (Role.NAME_TEXT to mainText),
                (1 to 0) to (Role.NAME_TEXT to mainText),
            )
        )

        val theme = ScoreboardColorExtractor.extract(image, grid)

        assertEquals(hex(bg), theme.currentSetBackgroundColor.color)
        assertEquals(hex(bg), theme.currentGameBackgroundColor.color)
        assertEquals(hex(mainText), theme.currentSetTextColor.color)
        assertEquals(hex(mainText), theme.currentGameTextColor.color)
    }

    /** Нет serve-ячейки → serve_color фолбэчит на основной текст. */
    @Test
    fun `missing serve indicator falls back to main text`() {
        val (image, grid) = renderScoreboard(
            bg = bg,
            placements = mapOf((0 to 0) to (Role.NAME_TEXT to mainText))
        )

        val theme = ScoreboardColorExtractor.extract(image, grid)

        assertEquals(hex(mainText), theme.serveColor.color)
    }

    /** serve-ячейка есть и отличается от фона и текста → измеряется как акцент. */
    @Test
    fun `serve indicator is measured when distinct`() {
        val (image, grid) = renderScoreboard(
            bg = bg,
            placements = mapOf(
                (0 to 0) to (Role.NAME_TEXT to mainText),
                (0 to 1) to (Role.SERVE to serve),
            )
        )

        val theme = ScoreboardColorExtractor.extract(image, grid)

        assertEquals(hex(serve), theme.serveColor.color)
    }

    /** Если serve-цвет совпадает с основным текстом, serveColor фолбэчит на текст (не считает акцентом). */
    @Test
    fun `serve equal to main text falls back to main text`() {
        val (image, grid) = renderScoreboard(
            bg = bg,
            placements = mapOf(
                (0 to 0) to (Role.NAME_TEXT to mainText),
                (0 to 1) to (Role.SERVE to mainText), // тот же цвет, что и текст
            )
        )

        val theme = ScoreboardColorExtractor.extract(image, grid)

        assertEquals(hex(mainText), theme.serveColor.color)
    }

    @Test
    fun `extract rejects empty grid`() {
        val (image, _) = renderScoreboard(bg = bg, placements = emptyMap())
        assertFailsWith<IllegalArgumentException> {
            ScoreboardColorExtractor.extract(image, emptyList())
        }
    }

    @Test
    fun `extract rejects non-rectangular grid`() {
        val (image, _) = renderScoreboard(bg = bg, placements = emptyMap())
        val ragged = listOf(
            listOf(Role.BACKGROUND, Role.BACKGROUND, Role.IGNORE),
            listOf(Role.BACKGROUND, Role.BACKGROUND), // короче
        )
        assertFailsWith<IllegalArgumentException> {
            ScoreboardColorExtractor.extract(image, ragged)
        }
    }

    @Test
    fun `extract rejects grid with empty rows`() {
        val (image, _) = renderScoreboard(bg = bg, placements = emptyMap())
        assertFailsWith<IllegalArgumentException> {
            ScoreboardColorExtractor.extract(image, listOf(emptyList()))
        }
    }

    @Test
    fun `drawGridOverlay returns decodable png of same size with requested grid`() {
        val original = solidImage(0x223355, width = 240, height = 160)
        val rows = 8
        val cols = 12

        val overlay = ScoreboardColorExtractor.drawGridOverlay(original, rows, cols)

        val decoded = ImageIO.read(ByteArrayInputStream(overlay))
        assertEquals(240, decoded.width)
        assertEquals(160, decoded.height)
    }

    @Test
    fun `drawGridOverlay rejects non-positive dimensions`() {
        val image = solidImage(0x000000, width = 40, height = 40)
        assertFailsWith<IllegalArgumentException> { ScoreboardColorExtractor.drawGridOverlay(image, 0, 8) }
        assertFailsWith<IllegalArgumentException> { ScoreboardColorExtractor.drawGridOverlay(image, 8, 0) }
    }

    @Test
    fun `extract ignores cells marked ignore`() {
        // Единственная не-background ячейка помечена ignore → движок её не видит,
        // остаётся только фон и (отсутствующий) текст → ошибка: нет основного текста.
        val (image, grid) = renderScoreboard(
            bg = bg,
            placements = mapOf((0 to 0) to (Role.IGNORE to mainText))
        )

        val ex = assertFailsWith<IllegalStateException> {
            ScoreboardColorExtractor.extract(image, grid)
        }
        assertTrue(ex.message!!.contains("main_text_color", ignoreCase = true))
    }

    // ---------- helpers ----------

    /**
     * Рисует синтетическое табло [rows]×[cols] ячеек и строит согласованную с ним grid-матрицу ролей.
     *
     * Всё изображение заливается цветом [bg]; каждая ячейка из [placements] заливается своим цветом и
     * помечается соответствующей ролью. Все прочие ячейки помечаются [Role.BACKGROUND] (и остаются bg).
     * Размер ячейки берётся с запасом, чтобы inset (marginFraction) не «съел» её целиком.
     */
    private fun renderScoreboard(
        bg: Int,
        placements: Map<Pair<Int, Int>, Pair<String, Int>>,
        rows: Int = 8,
        cols: Int = 20,
        cellW: Int = 40,
        cellH: Int = 50,
    ): Pair<ByteArray, List<List<String>>> {
        val w = cols * cellW
        val h = rows * cellH
        val img = BufferedImage(w, h, BufferedImage.TYPE_INT_RGB)
        val g = img.createGraphics()
        try {
            g.color = Color(bg)
            g.fillRect(0, 0, w, h)
            for ((pos, pair) in placements) {
                val (r, c) = pos
                val (role, rgb) = pair
                require(r in 0 until rows && c in 0 until cols) { "placement $pos out of $rows×$cols grid" }
                g.color = Color(rgb)
                g.fillRect(c * cellW, r * cellH, cellW, cellH)
            }
        } finally {
            g.dispose()
        }

        val grid: List<List<String>> = List(rows) { r ->
            List(cols) { c ->
                placements[Pair(r, c)]?.first ?: Role.BACKGROUND
            }
        }
        return encodePng(img) to grid
    }

    private fun solidImage(rgb: Int, width: Int, height: Int): ByteArray {
        val img = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val g = img.createGraphics()
        try {
            g.color = Color(rgb)
            g.fillRect(0, 0, width, height)
        } finally {
            g.dispose()
        }
        return encodePng(img)
    }

    private fun encodePng(img: BufferedImage): ByteArray {
        val baos = java.io.ByteArrayOutputStream()
        ImageIO.write(img, "png", baos)
        return baos.toByteArray()
    }

    private fun hex(rgb: Int): String = "#%06X".format(rgb and 0xFFFFFF)
}
