package me.bytebeats.kfiglet

import java.io.BufferedReader
import java.io.IOException
import java.io.Reader
import java.util.regex.Pattern

/**
 * @author <a href="https://github.com/bytebeats">bytebeats</a>
 * @email <happychinapc@gmail.com>
 * @since 2020/8/1 16:09
 */
class KFIGFontReader(private val reader: Reader) {

    @Throws(IOException::class)
    fun readFont(): KFIGFont? {
        val fontBuilder = KFIGFont.Builder()
        BufferedReader(reader).use {
            val header = it.readLine()
            parseHeader(header, fontBuilder)
            //Skip over comment lines
            for (i in 0 until fontBuilder.commentLines) {
                it.readLine()
            }
            // A FIGfont is required to have characters for ASCII 32 to 126 inclusive
            for (codePoint in 32 until 127) {
                readCharData(fontBuilder.height, it)?.apply {
                    fontBuilder.charData(codePoint.toChar(), this)
                }
            }
            /**
             * Additional required Deutsch FIGcharacters, in order:
             *
             * 196 (umlauted "A" -- two dots over letter "A")
             * 214 (umlauted "O" -- two dots over letter "O")
             * 220 (umlauted "U" -- two dots over letter "U")
             * 228 (umlauted "a" -- two dots over letter "a")
             * 246 (umlauted "o" -- two dots over letter "o")
             * 252 (umlauted "u" -- two dots over letter "u")
             * 223 ("ess-zed" -- see FIGcharacter illustration below)
             */
            for (codePoint in DEUTSCH_CODE_POINTS) {
                readCharData(fontBuilder.height, it)?.apply {
                    fontBuilder.charData(codePoint.toChar(), this)
                }
            }
            var line = it.readLine()
            while (line != null) {
                readCharData(fontBuilder.height, it)?.apply {
                    try {
                        parseCodeTag(line)?.let { ch -> fontBuilder.charData(ch, this) }
                    } catch (e: IllegalArgumentException) {
                        throw IOException("Could not parse code tag", e)
                    }
                }
                line = it.readLine()
            }
        }
        return fontBuilder.build()
    }

    companion object {
        /**
         * The magic string used to determine if a stream of data contains a FIGfont
         * definition.
         */
        private const val FIG_FONT_MAGIC_STRING = "flf2"

        /**
         * Based on
         * @see {@linkurl http://www.jave.de/docs/figfont.txt}
         */
        private val CODE_TAG_PATTERN = Pattern.compile("([^\\\\s]+)\\\\s*.*")
        private val DEUTSCH_CODE_POINTS = intArrayOf(196, 214, 220, 228, 246, 252, 223)

        /**
         * Returns the unicode character represented by a code tag.
         *
         * @param codeTagText
         *            The code tag text to parse.
         * @return The character represented.
         * @throws IllegalArgumentException
         *             if the text cannot be parsed as a code tag.
         */
        @Throws(IllegalArgumentException::class)
        fun parseCodeTag(codeTagText: String): Char? {
            val matcher = CODE_TAG_PATTERN.matcher(codeTagText)
            if (matcher.matches()) {
                val codePointText = matcher.group(1)
                val codePoint = codePointText.toInt()
                return codePoint.toChar()
            } else {
                throw IllegalArgumentException("Could not parse text as a code tag: $codeTagText")
            }
        }

        /**
         * Reads the data that defines a single character from a {@link BufferedReader}.
         *
         * @param height
         *            The height of the character in lines of data.
         * @param bufferedReader
         *            The buffered reader from which to read the character data.
         * @return The string that represents the character data.
         * @throws IOException
         *             if there is a problem reading the data.
         */
        @Throws(IOException::class)
        fun readCharData(height: Int, bufferedReader: BufferedReader): String? {
            val figChar = StringBuilder()
            for (lineNum in 0 until height) {
                val line = bufferedReader.readLine()
                var charIdx = line.lastIndex
                // Skip over any whitespace characters at the end of the line
                while (charIdx >= 0 && line[charIdx].isWhitespace()) {
                    charIdx -= 1
                }
                // We've found a non-whitespace character that we will interpret as an
                // end-character.
                var endChar = line[charIdx]
                // Skip over any end-characters.
                while (charIdx > -1 && line[charIdx] == endChar) {
                    charIdx -= 1
                }
                // We've found the right-hand edge of the actual character data for this line.
                figChar.append(line.subSequence(0, charIdx + 1))
            }
            return figChar.toString()
        }

        /**
         * Parses a FIGfont header into a {@link FigFont.Builder} instance.
         *
         * @param header
         *            The header to parse.
         * @param fontBuilder
         *            The font builder to set with values read from the header.
         * @throws IllegalArgumentException
         *             if the header text cannot be parsed as a FIGfont header.
         */
        @Throws(IllegalArgumentException::class)
        fun parseHeader(header: String, fontBuilder: KFIGFont.Builder) {
            val args = header.split("\\s+")
            if (args[0].startsWith(FIG_FONT_MAGIC_STRING)) {
                fontBuilder.hardBlankChar(args[0].last())
                val size = args.size
                if (size > 1) {
                    fontBuilder.height(args[1].toInt())
                }
                if (size > 2) {
                    fontBuilder.baseline(args[2].toInt())
                }
                if (size > 3) {
                    fontBuilder.maxLength(args[3].toInt())
                }
                if (size > 4) {
                    val oldLayout = KLayoutOption.ofOption(args[4].toInt())
                    fontBuilder.oldLayout(oldLayout)
                    fontBuilder.fullLayout(KLayoutOption.fullLayoutOptionFromOld(oldLayout))
                }
                if (size > 5) {
                    fontBuilder.commentLines(args[5].toInt())
                }
                if (size > 6) {
                    fontBuilder.textDirection(KFIGFont.TextDirection.ofHeaderValue(args[6].toInt()))
                }
                if (size > 7) {
                    fontBuilder.fullLayout(KLayoutOption.ofOption(args[7].toInt()))
                }
                if (size > 8) {
                    fontBuilder.codeTagCount(args[8].toInt())
                }
            } else {
                throw IllegalArgumentException("header doesn't start with FIGFont magic string $FIG_FONT_MAGIC_STRING: $header")
            }
        }
    }
}