package com.bashkevich.tennisscorekeeperbackend.feature.theme

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Юнит-тесты [PaletteExtractor] на синтетических изображениях (без LLM/сети/БД).
 *
 * Идея: рисуем простые Bilder с известными цветами и проверяем, что палитра + частоты
 * выходят предсказуемыми. Color Thief (MMCQ) даёт квантование, поэтому проверяем по
 * ближайшему цвету к ожидаемому, а не на точное равенство hex.
 */
class PaletteExtractorTest {

    @Test
    fun `single solid color yields palette dominated by that color`() {
        val image = solidColor(40, 40, Color(0x22, 0x2E, 0x57))

        val palette = PaletteExtractor.extract(image)

        assertTrue(palette.isNotEmpty(), "palette should not be empty for a solid image")
        val top = palette.first()
        // Самый частый цвет должен быть близок к фону (допускаем квантование).
        assertCloseColor(Color(0x22, 0x2E, 0x57), top.centroid, tolerance = 30.0)
        // У солидного фона доминирующий цвет занимает почти всё изображение.
        assertTrue(top.share > 0.9, "dominant color share=${top.share} should be > 0.9")
    }

    @Test
    fun `two distinct regions produce two dominant colors`() {
        // Левая половина — тёмно-синий фон, правая — белый текст-блок.
        val image = BufferedImage(100, 40, BufferedImage.TYPE_INT_RGB)
        val g = image.createGraphics()
        g.color = Color(0x22, 0x2E, 0x57)
        g.fillRect(0, 0, 100, 40)
        g.color = Color.WHITE
        g.fillRect(60, 0, 40, 40)
        g.dispose()

        val palette = PaletteExtractor.extract(toPng(image))

        assertTrue(palette.size >= 2, "expected at least 2 colors, got ${palette.size}")
        // Среди топ-2 должны быть и синий, и белый.
        val centroids = palette.take(2).map { it.centroid }
        assertTrue(
            centroids.any { c -> distance(c, 0x22, 0x2E, 0x57) < 30.0 },
            "expected a navy cluster near #222E57"
        )
        assertTrue(
            centroids.any { c -> distance(c, 0xFF, 0xFF, 0xFF) < 30.0 },
            "expected a white cluster near #FFFFFF"
        )
    }

    @Test
    fun `close shades merge into one cluster`() {
        // Два очень близких синих оттенка (дистанция < MERGE_THRESHOLD=40) должны схлопнуться
        // в одну запись палитры после merge.
        val image = BufferedImage(100, 40, BufferedImage.TYPE_INT_RGB)
        val g = image.createGraphics()
        g.color = Color(0x22, 0x2E, 0x57)
        g.fillRect(0, 0, 50, 40)
        g.color = Color(0x24, 0x30, 0x5A) // +2/+2/+3 — явно ближе 40
        g.fillRect(50, 0, 50, 40)
        g.dispose()

        val palette = PaletteExtractor.extract(toPng(image))

        // Не должно быть двух отдельных синих кластеров.
        val navyClusters = palette.count { c -> distance(c.centroid, 0x22, 0x2E, 0x57) < 40.0 }
        assertEquals(1, navyClusters, "near-duplicate navy shades should merge into one cluster")
    }

    @Test
    fun `shares sum approximately to one`() {
        val image = BufferedImage(80, 40, BufferedImage.TYPE_INT_RGB)
        val g = image.createGraphics()
        g.color = Color.BLACK
        g.fillRect(0, 0, 80, 40)
        g.color = Color.YELLOW
        g.fillRect(20, 10, 40, 20)
        g.dispose()

        val palette = PaletteExtractor.extract(toPng(image))
        val sum = palette.sumOf { it.share }

        assertTrue(sum in 0.98..1.02, "shares should sum ~1.0, got $sum")
    }

    @Test
    fun `empty image bytes throw`() {
        try {
            PaletteExtractor.extract(ByteArray(0))
            fail("expected IllegalArgumentException for empty image")
        } catch (e: IllegalArgumentException) {
            // expected
        }
    }

    @Test
    fun `undecodable bytes throw`() {
        try {
            PaletteExtractor.extract(byteArrayOf(1, 2, 3, 4))
            fail("expected failure for undecodable bytes")
        } catch (e: IllegalStateException) {
            // expected — ImageIO.read returns null
        }
    }

    @Test
    fun `encodeToPng returns decodable png`() {
        val original = solidColor(20, 20, Color.RED)
        val png = PaletteExtractor.encodeToPng(original)

        val decoded = ImageIO.read(png.inputStream())
        assertEquals(20, decoded.width)
        assertEquals(20, decoded.height)
    }

    // --- helpers ---

    private fun solidColor(w: Int, h: Int, color: Color): ByteArray {
        val image = BufferedImage(w, h, BufferedImage.TYPE_INT_RGB)
        val g = image.createGraphics()
        g.color = color
        g.fillRect(0, 0, w, h)
        g.dispose()
        return toPng(image)
    }

    private fun toPng(image: BufferedImage): ByteArray {
        val out = ByteArrayOutputStream()
        ImageIO.write(image, "png", out)
        return out.toByteArray()
    }

    private fun assertCloseColor(expected: Color, actual: RgbColor, tolerance: Double) {
        val d = distance(actual, expected.red, expected.green, expected.blue)
        assertTrue(d <= tolerance, "color ${actual.toHex()} is too far from expected (d=$d > tol=$tolerance)")
    }

    private fun distance(c: RgbColor, r: Int, g: Int, b: Int): Double {
        val dr = (c.r - r).toDouble()
        val dg = (c.g - g).toDouble()
        val db = (c.b - b).toDouble()
        return kotlin.math.sqrt(dr * dr + dg * dg + db * db)
    }
}
