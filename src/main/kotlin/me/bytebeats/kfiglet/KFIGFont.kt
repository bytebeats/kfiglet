package me.bytebeats.kfiglet

import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.lang.IllegalArgumentException
import java.lang.IndexOutOfBoundsException

/**
 * @author bytebeats
 * @email <happychinapc@gmail.com>
 * @since 2020/8/1 12:32
 */

/**
 *@param hardBlankChar The sub-character used to represent hardblanks in the FIGcharacter data.
 * <p>
 * By convention, the usual hardblank is a "$", but it can be any character
 * except a blank (space), a carriage-return, a newline (linefeed) or a null
 * character. If you want the entire printable ASCII set available to use, make
 * the hardblank a "delete" character (character code 127). With the exception
 * of delete, it is inadvisable to use non-printable characters as a hardblank.
 * </p>
 * @param height The consistent height of every FIGcharacter, measured in sub-characters. Note
 * that ALL FIGcharacters in a given FIGfont have the same height, since this
 * includes any empty space above or below. This is a measurement from the top
 * of the tallest FIGcharacter to the bottom of the lowest hanging FIGcharacter,
 * such as a lowercase g.
 * @param baseline The number of lines of sub-characters from the baseline of a FIGcharacter to
 * the top of the tallest FIGcharacter. The baseline of a FIGfont is an
 * imaginary line on top of which capital letters would rest, while the tails of
 * lowercase g, j, p, q, and y may hang below. In other words, Baseline is the
 * height of a FIGcharacter, ignoring any descenders.
 * @param maxLength The maximum length of any line describing a FIGcharacter. This is usually the
 * width of the widest FIGcharacter, plus 2 (to accommodate end marks).
 * @param oldLayout
 * @param commentLines The number of lines there are between the first line and the actual
 * FIGcharacters of the FIGfont. Comments are optional, but recommended to
 * properly document the origin of a FIGfont.
 * @param textDirection The direction the font is to be printed by default.
 * @param codeTagCount The number of code-tagged (non-required) FIGcharacters in this FIGfont. This
 * is always equal to the total number of FIGcharacters in the font minus 102.
 */

data class KFIGFont private constructor(
    val hardBlankChar: Char,
    val height: Int,
    val baseline: Int,
    val maxLength: Int,
    val oldLayout: KLayoutOption,
    val commentLines: Int,
    val textDirection: TextDirection = TextDirection.LTR,
    val fullLayout: KLayoutOption,
    val codeTagCount: Int,
    val figChars: MutableMap<Char, KFIGChar>
) {
    /**
     * Returns the {@link FigCharacter} that represents a character. If the font
     * does not have a {@link FigCharacter} for the requested character then
     * <code>null</code> is returned.
     *
     * @param char The character for which to return a {@link FigCharacter}.
     * @return The {@link FigCharacter} for the requested character or
     *         <code>null</code> if the font does not contain a suitable
     *         {@link FigCharacter}.
     */
    fun getFIGChar(char: Char): KFIGChar? = figChars[char]

    fun computeOverlapAmount(char1: Char, char2: Char, smushMode: Int, textDirection: TextDirection): Int {
        if (!KLayoutOption.isLayoutOptionSelected(
                KLayoutOption.HORIZONTAL_SMUSHING_BY_DEFAULT.option and KLayoutOption.HORIZONTAL_FITTING_BY_DEFAULT.option,
                smushMode
            )) {
            return 0
        }
        if (char1.isISOZero() || char2.isISOZero()) {
            return 0
        }
        val leftFIGChar = if (textDirection == TextDirection.LTR) {
            getFIGChar(char1)
        } else {
            getFIGChar(char2)
        }
        val rightFIGChar = if (textDirection == TextDirection.LTR) {
            getFIGChar(char2)
        } else {
            getFIGChar(char1)
        }
        if (leftFIGChar?.width ?: 0 < 2 || rightFIGChar?.width ?: 0 < 2) {
            return 0
        }
        var smushAmount = rightFIGChar?.width ?: throw NullPointerException("FIGChar of right side can't be null")
        // Calculate the minimum amount that a row of rightFigChar may be smushed into
        // the corresponding row of leftFigChar
        for (row in 0 until height) {
            var rowSmushAmount = 0
            var leftFIGCharRightBoundary = leftFIGChar!!.width - 1
            while (leftFIGCharRightBoundary > 0 && leftFIGChar.getCharAt(leftFIGCharRightBoundary, row) == ' ') {
                leftFIGCharRightBoundary -= 1
            }
            var rightFIGCharLeftBoundary = 0
            while (rightFIGCharLeftBoundary < rightFIGChar.width - 1
                && rightFIGChar.getCharAt(rightFIGCharLeftBoundary, row) == ' ') {
                rightFIGCharLeftBoundary += 1
            }
            rowSmushAmount =
                rightFIGChar.width.coerceAtMost(leftFIGChar.width - leftFIGCharRightBoundary - 1 + rightFIGCharLeftBoundary)
            if (leftFIGChar.getCharAt(leftFIGCharRightBoundary, row) == ' ') {
                rowSmushAmount += 1
            } else if (!smushem(
                    leftFIGChar.getCharAt(leftFIGCharRightBoundary, row),
                    rightFIGChar.getCharAt(rightFIGCharLeftBoundary, row),
                    smushMode,
                    textDirection
                ).isISOZero()) {
                rowSmushAmount += 1
            }
            smushAmount = smushAmount.coerceAtMost(rowSmushAmount)
        }
        return smushAmount
    }

    /**
     * Calculates the character that is the result of merging two characters.
     * Possible outcomes are a single character to use as the replacement for the
     * input characters when smushing FIGcharacters, or the <code>null</code>
     * character '/0' which represents unsmushable input characters.
     *
     * @param char1
     *            The first character to smush.
     * @param char2
     *            The second character to smush.
     * @param smushmode
     *            The smushmode that determines how smushing occurs. This value may
     *            be generated by combining values from {@link LayoutOptions}.
     * @param textDirection
     *            The print direction that determines whether the second character
     *            is considered to be to the right or the left of the first.
     * @return The character representing the result of smushing the input
     *         characters, or the <code>null</code> character '/0' if the input
     *         characters cannot be smushed.
     * @see LayoutOptions
     */
    fun smushem(char1: Char, char2: Char, smushMode: Int, textDirection: TextDirection): Char {
        if (char1 == ' ') return char2
        if (char2 == ' ') return char1
        if (!KLayoutOption.isLayoutOptionSelected(KLayoutOption.HORIZONTAL_SMUSHING_BY_DEFAULT.option, smushMode)) {
            return '\u0000'//kerning
        }
        if (smushMode and 63 == 0) {
            /* This is smushing by universal overlapping. */
            /* Below four lines ensure overlapping preference to visible characters. */
            if (char1 == ' ') return char2
            if (char2 == ' ') return char1
            if (char1 == hardBlankChar) return char2
            if (char2 == hardBlankChar) return char1
            /**
             * Below line ensures that the dominant (foreground) fig-character for overlapping is the latter in the user's text,
             * not necessarily the rightmost character.
             * */
            if (textDirection == TextDirection.LTR) return char2
            /* Occurs in the absence of above exceptions. */
            return char1
        }
        if (KLayoutOption.isLayoutOptionSelected(KLayoutOption.HORIZONTAL_HARDBLANK_SMUSHING.option, smushMode)) {
            if (char1 == hardBlankChar && char2 == hardBlankChar) return char1
        }
        if (char1 == hardBlankChar || char2 == hardBlankChar) return ISO_ZERO
        if (KLayoutOption.isLayoutOptionSelected(KLayoutOption.HORIZONTAL_EQUAL_CHARACTER_SMUSHING.option, smushMode)) {
            if (char1 == char2) return char1
        }
        if (KLayoutOption.isLayoutOptionSelected(KLayoutOption.HORIZONTAL_UNDERSCORE_SMUSHING.option, smushMode)) {
            if (char1 == '_' && "|/\\\\[]{}()<>".indexOf(char2) > -1) return char2
            if (char2 == '_' && "|/\\\\[]{}()<>".indexOf(char1) > -1) return char1
        }
        if (KLayoutOption.isLayoutOptionSelected(KLayoutOption.HORIZONTAL_HIERARCHY_SMUSHING.option, smushMode)) {
            if (char1 == '|' && "/\\\\[]{}()<>".indexOf(char2) > -1) return char2
            if (char2 == '|' && "/\\\\[]{}()<>".indexOf(char1) > -1) return char1
            if ("/\\\\".indexOf(char1) > -1 && "[]{}()<>".indexOf(char2) > -1) return char2
            if ("/\\\\".indexOf(char2) > -1 && "[]{}()<>".indexOf(char1) > -1) return char1
            if ("[]".indexOf(char1) > -1 && "{}()<>".indexOf(char2) > -1) return char2
            if ("[]".indexOf(char2) > -1 && "{}()<>".indexOf(char1) > -1) return char1
            if ("{}".indexOf(char1) > -1 && "()<>".indexOf(char2) > -1) return char2
            if ("{}".indexOf(char2) > -1 && "()<>".indexOf(char1) > -1) return char1
            if ("()".indexOf(char1) > -1 && "<>".indexOf(char2) > -1) return char2
            if ("()".indexOf(char2) > -1 && "<>".indexOf(char1) > -1) return char1
        }

        if (KLayoutOption.isLayoutOptionSelected(KLayoutOption.HORIZONTAL_OPPOSITE_PAIR_SMUSHING.option, smushMode)) {
            if (char1 == '[' && char2 == ']') return '|'
            if (char2 == '[' && char1 == ']') return '|'
            if (char1 == '{' && char2 == '}') return '|'
            if (char2 == '{' && char1 == '}') return '|'
            if (char1 == '(' && char2 == ')') return '|'
            if (char2 == '(' && char1 == ')') return '|'
        }

        if (KLayoutOption.isLayoutOptionSelected(KLayoutOption.HORIZONTAL_BIG_X_SMUSHING.option, smushMode)) {
            if (char1 == '/' && char2 == '\\') return '|'
            if (char2 == '/' && char1 == '\\') return '|'
            /* Don't want the reverse of above to give 'X'. */
            if (char1 == '>' && char2 == '<') return 'X'
        }
        return ISO_ZERO
    }

    override fun toString(): String {
        val signature = StringBuilder()
        for (entry in figChars.entries) {
            signature.append(entry.key)
            signature.append(":\n")
            signature.append(entry.value)
            signature.append("\n")
        }
        return signature.toString()
    }

    enum class TextDirection {
        /**
         * Left to Right
         */
        LTR,

        /**
         * Right to Left
         */
        RTL;

        companion object {
            /**
             * Returns the print direction represented by a FIGfont header value. A value of
             * 0 means left-to-right, and 1 means right-to-left.
             *
             * @param headerValue The FIGfont print direction value. 0 means left-to-right, and 1
             *            means right-to-left.
             * @return The print direction represented by the value.
             */
            @Throws(IllegalArgumentException::class)
            fun ofHeaderValue(headerValue: Int): TextDirection {
                if (headerValue in values().indices) {
                    return values()[headerValue]
                } else {
                    throw IllegalArgumentException("Unrecognised header value: $headerValue")
                }
            }
        }
    }

    /**
     * FIGChar represents a single FIGlet character from a FIGfont.
     */
    class KFIGChar(val font: KFIGFont, val chars: String) {
        /**
         * Returns the KFIGChar sub-character at the requested column and row.
         *
         * @param column
         *            The column for which to return a sub-character.
         * @param row
         *            The row for which to return a sub-character.
         * @return The sub-character at the requested column and row.
         * @throws IndexOutOfBoundsException
         *             if the requested column and row does not exist within the
         *             KFIGChar data.
         */
        @Throws(IndexOutOfBoundsException::class)
        fun getCharAt(column: Int, row: Int): Char {
            if (row in 0 until height && column in 0 until width) {
                return chars[row * width + column]
            } else {
                throw IndexOutOfBoundsException("Row $row, Column $column, Height $height, Width $width")
            }
        }

        /**
         * Returns string of sub-characters that define a row of the KFIGChar.
         *
         * @param row
         *            The row for which to return the sub-character string.
         * @return The sub-characters that define the requested KFIGChar row.
         * @throws IndexOutOfBoundsException
         *             if the requested row does not exist within the KFIGChar data.
         */
        @Throws(IndexOutOfBoundsException::class)
        fun getRow(row: Int): String {
            if (row in 0 until height) {
                val start = row * width
                return chars.substring(start, start + width)
            } else {
                throw IndexOutOfBoundsException("Row $row, Height $height")
            }
        }

        /**
         * Returns the height of the KFIGChar.
         */
        val height: Int
            get() = font.height

        /**
         * Returns the width of the KFIGChar
         */
        val width: Int
            get() = chars.length / height

        override fun toString(): String {
            val signature = StringBuilder()
            for (j in 0 until height) {
                signature.append(chars.substring(j * width, j * width + width))
                signature.append("\n")
            }
            return signature.toString()
        }
    }

    class Builder {
        private var hardBlankChar: Char = ' '
        var height: Int = 0
        private var baseline: Int = 0
        private var maxLength: Int = 0
        private var oldLayout: KLayoutOption = KLayoutOption.HORIZONTAL_FITTING_BY_DEFAULT
        var commentLines: Int = 0
        private var textDirection: TextDirection = TextDirection.LTR
        private var fullLayout: KLayoutOption = KLayoutOption.HORIZONTAL_FITTING_BY_DEFAULT
        private var codeTagCount: Int = 0
        private var charData: MutableMap<Char, String> = mutableMapOf()

        fun hardBlankChar(char: Char) = apply { this.hardBlankChar = char }
        fun height(height: Int) = apply { this.height = height }
        fun baseline(baseline: Int) = apply { this.baseline = baseline }
        fun maxLength(maxLength: Int) = apply { this.maxLength = maxLength }
        fun oldLayout(oldLayout: KLayoutOption) = apply { this.oldLayout = oldLayout }
        fun commentLines(commentLines: Int) = apply { this.commentLines = commentLines }
        fun textDirection(textDirection: TextDirection) = apply { this.textDirection = textDirection }
        fun fullLayout(fullLayout: KLayoutOption) = apply { this.fullLayout = fullLayout }
        fun codeTagCount(codeTagCount: Int) = apply { this.codeTagCount = codeTagCount }
        fun charData(char: Char, data: String) = apply {
            this.charData[char] = data
        }

        fun build(): KFIGFont {
            val figFont = KFIGFont(
                hardBlankChar,
                height,
                baseline,
                maxLength,
                oldLayout,
                commentLines,
                textDirection,
                fullLayout,
                codeTagCount,
                mutableMapOf()
            )
            charData.forEach { (t, u) ->
                figFont.figChars[t] = KFIGChar(figFont, u)
            }
            return figFont
        }
    }

    companion object {

        /**
         * Loads a KFIGFont from an {@link InputStream}.
         *
         * @param inputStream
         *            The input stream containing the FIGfont data to load.
         * @return The loaded FigFont instance.
         * @throws IOException
         *             if there is a problem loading the stream data.
         */
        @Throws(IOException::class)
        fun loadFont(inputStream: InputStream): KFIGFont? {
            InputStreamReader(inputStream).use {
                return KFIGFontReader(it).readFont()
            }
        }
    }
}