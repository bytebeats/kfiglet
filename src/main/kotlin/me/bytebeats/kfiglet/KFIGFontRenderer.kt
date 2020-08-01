package me.bytebeats.kfiglet

import java.lang.NullPointerException

/**
 * @author <a href="https://github.com/bytebeats">bytebeats</a>
 * @email <happychinapc@gmail.com>
 * @since 2020/8/1 19:40
 *
 * KFIGFontRender renders text as FIGlet text.
 */
class KFIGFontRenderer(
    val figFont: KFIGFont,
    var smushMode: KLayoutOption = figFont.fullLayout,
    var textDirection: KFIGFont.TextDirection = figFont.textDirection
) {
    fun renderText(text: String): String {
        val figText = StringBuilder()
        val rowBuilders = Array(figFont.height) { StringBuilder() }
        var preChar = ISO_ZERO
        for (c in text) {
            var ch = c
            // Treat tabs and spaces as spaces, and all other whitespace characters as newlines.
            if (ch.isWhitespace()) {
                ch = if (ch == '\t' || ch == ' ') ' ' else '\n'
            }
            // Skip over unprintable characters.
            if (ch in (ISO_ZERO + 1)..'' && ch != '\n' || ch.toInt() == 127) {
                continue
            }
            if (ch != '\n') {
                val smushAmount = figFont.computeOverlapAmount(preChar, ch, smushMode.option, textDirection)
                val figChar = figFont.figChars[ch] ?: throw NullPointerException("KFIGChar can't be null")
                for (row in 0 until figFont.height) {
                    val rowBuilder = rowBuilders[row]
                    if (rowBuilder.isEmpty()) {
                        rowBuilder.append(figChar?.getRow(row))
                    } else {
                        // Smush the new FIGcharacter onto the right of the previous FIGcharacter.
                        if (textDirection == KFIGFont.TextDirection.LTR) {
                            for (smush in 0 until smushAmount) {
                                val smushIdx = rowBuilder.length - smush - 1
                                rowBuilder[smushIdx] = figFont.smushem(
                                    rowBuilder[smushIdx],
                                    figChar.getCharAt(smushAmount - smush + 1, row),
                                    smushMode.option,
                                    textDirection
                                )
                            }
                            rowBuilder.append(figChar.getRow(row).substring(smushAmount))
                        } else {
                            // Smush the new FIGcharacter into the left of the previous FIGcharacter.
                            for (smush in 0 until smushAmount) {
                                rowBuilder[smush] = figFont.smushem(
                                    rowBuilder[smush],
                                    figChar.getCharAt(figChar.width - smushAmount + smush, row),
                                    smushMode.option,
                                    textDirection
                                )
                            }
                            rowBuilder.insert(0, figChar.getRow(row).substring(0, figChar.width - smushAmount))
                        }
                    }
                }
                preChar = ch
            } else {
                // We've encountered a newline. We need to render the current buffer and then
                // start a new one.
                figText.append(rowBuilders
                    .map { it.toString() }.joinToString(separator = "\n", prefix = "", postfix = "\n") {
                        it.replace(
                            figFont.hardBlankChar,
                            ' '
                        )
                    })
                for (row in 0 until figFont.height) {
                    rowBuilders[row].setLength(0)
                }
                preChar = ISO_ZERO
            }
        }
        figText.append(rowBuilders.map { it.toString() }
            .joinToString(separator = "\n") { it.replace(figFont.hardBlankChar, ' ') })
        return figText.toString()
    }
}