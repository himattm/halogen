package halogen

import halogen.color.Hct
import halogen.color.TonalPalette
import kotlin.test.Test

class HctSolverDebugTest {


    private fun Int.toHex2(): String {
        val chars = CharArray(2)
        for (i in 1 downTo 0) {
            val v = (this shr ((1 - i) * 4)) and 0xF
            chars[i] = if (v < 10) (v + '0'.code).toChar() else (v - 10 + 'A'.code).toChar()
        }
        return chars.concatToString()
    }

    @Test
    fun debugGrassColors() {
        // Reproduce the "grass" bug: hue 65, chroma 32, various tones
        val palette = TonalPalette.fromHueAndChroma(64.66, 32.0)

        listOf(10, 20, 30, 40, 50, 60, 70, 80, 90, 95).forEach { tone ->
            val argb = palette.tone(tone)
            val r = (argb shr 16) and 0xFF
            val g = (argb shr 8) and 0xFF
            val b = argb and 0xFF
            val hex = "#${r.toHex2()}${g.toHex2()}${b.toHex2()}"
            println("Tone $tone: $hex (R=$r G=$g B=$b)")
        }

        // Also check error palette
        println("\nError palette (hue=25, chroma=84):")
        val errorPalette = TonalPalette.fromHueAndChroma(25.0, 84.0)
        listOf(20, 30, 40, 50, 60, 70, 80, 90).forEach { tone ->
            val argb = errorPalette.tone(tone)
            val r = (argb shr 16) and 0xFF
            val g = (argb shr 8) and 0xFF
            val b = argb and 0xFF
            println("Tone $tone: #${r.toHex2()}${g.toHex2()}${b.toHex2()} (R=$r G=$g B=$b)")
        }

        // Direct HCT test
        println("\nDirect HCT.from(65, 32, 40):")
        val hct = Hct.from(65.0, 32.0, 40.0)
        val argb = hct.toInt()
        val r = (argb shr 16) and 0xFF
        val g = (argb shr 8) and 0xFF
        val b = argb and 0xFF
        println("Result: #${r.toHex2()}${g.toHex2()}${b.toHex2()} (R=$r G=$g B=$b) hue=${hct.hue} chroma=${hct.chroma} tone=${hct.tone}")
    }
}
