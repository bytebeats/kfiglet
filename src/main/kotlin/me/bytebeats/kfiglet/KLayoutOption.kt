package me.bytebeats.kfiglet

/**
 * @author <a href="https://github.com/bytebeats">bytebeats</a>
 * @email <happychinapc@gmail.com>
 * @since 2020/8/1 11:02
 *
 * Character width and Character height to smush
 */
enum class KLayoutOption(val option: Int) {
    INVALID(-1),
    NIL(0),

    /**
     * Rule 1: EQUAL CHARACTER SMUSHING (option value 1)
     * <p>
     * Two sub-characters are smushed into a single sub-character if they are the
     * same. This rule does not smush hardblanks.
     * </p>
     */
    HORIZONTAL_EQUAL_CHARACTER_SMUSHING(1 shl 0),

    /**
     * Rule 2: UNDERSCORE SMUSHING (option value 2)
     * <p>
     * An underscore ("_") will be replaced by any of: "|", "/", "\", "[", "]", "{",
     * "}", "(", ")", "&lt;" or "&gt;".
     * </p>
     */
    HORIZONTAL_UNDERSCORE_SMUSHING(1 shl 1),

    /**
     * Rule 3: HIERARCHY SMUSHING (option value 4)
     * <p>
     * A hierarchy of six classes is used: "|", "/\", "[]", "{}", "()", and
     * "&lt;&gt;". When two smushing sub-characters are from different classes, the
     * one from the latter class will be used.
     * </p>
     */
    HORIZONTAL_HIERARCHY_SMUSHING(1 shl 2),

    /**
     * Rule 4: OPPOSITE PAIR SMUSHING (option value 8)
     * <p>
     * Smushes opposing brackets ("[]" or "]["), braces ("{}" or "}{") and
     * parentheses ("()" or ")(") together, replacing any such pair with a vertical
     * bar ("|").
     * </p>
     */
    HORIZONTAL_OPPOSITE_PAIR_SMUSHING(1 shl 3),

    /**
     * Rule 5: BIG X SMUSHING (option value 16)
     * <p>
     * Smushes "/\" into "|", "\/" into "Y", and "&gt;&lt;" into "X". Note that
     * "&lt;&gt;" is not smushed in any way by this rule. The name "BIG X" is
     * historical; originally all three pairs were smushed into "X".
     * </p>
     */
    HORIZONTAL_BIG_X_SMUSHING(1 shl 4),

    /**
     * Rule 6: HARDBLANK SMUSHING (option value 32)
     * <p>
     * Smushes two hardblanks together, replacing them with a single hardblank.
     * </p>
     */
    HORIZONTAL_HARDBLANK_SMUSHING(1 shl 5),

    /**
     * Moves FIG characters closer together until they touch. Typographers use the
     * term "kerning" for this phenomenon when applied to the horizontal axis, but
     * fitting also includes this as a vertical behavior, for which there is
     * apparently no established typographical term.
     */
    HORIZONTAL_FITTING_BY_DEFAULT(1 shl 6),

    /**
     * Moves FIG characters one step closer after they touch, so that they partially
     * occupy the same space. A FIG driver must decide what sub-character to display
     * at each junction. There are two ways of making these decisions: by controlled
     * smushing or by universal smushing.
     */
    HORIZONTAL_SMUSHING_BY_DEFAULT(1 shl 7),

    /**
     * Rule 1: EQUAL CHARACTER SMUSHING (option value 256)
     * <p>
     * Same as horizontal smushing rule 1.
     * </p>
     */
    VERTICAL_EQUAL_CHARACTER_SMUSHING(1 shl 8),

    /**
     * Rule 2: UNDERSCORE SMUSHING (option value 512)
     * <p>
     * Same as horizontal smushing rule 2.
     * </p>
     */
    VERTICAL_UNDERSCORE_SMUSHING(1 shl 9),

    /**
     * Rule 3: HIERARCHY SMUSHING (option value 1024)
     * <p>
     * Same as horizontal smushing rule 3.
     * </p>
     */
    VERTICAL_HIERARCHY_SMUSHING(1 shl 10),

    /**
     * Rule 4: HORIZONTAL LINE SMUSHING (option value 2048)
     * <p>
     * Smushes stacked pairs of "-" and "_", replacing them with a single "="
     * sub-character. It does not matter which is found above the other. Note that
     * vertical smushing rule 1 will smush IDENTICAL pairs of horizontal lines,
     * while this rule smushes horizontal lines consisting of DIFFERENT
     * sub-characters.
     * </p>
     */
    VERTICAL_HORIZONTAL_LINE_SMUSHING(1 shl 11),

    /**
     * Rule 5: VERTICAL LINE SUPERSMUSHING (option value 4096)
     * <p>
     * This one rule is different from all others, in that it "supersmushes"
     * vertical lines consisting of several vertical bars ("|"). This creates the
     * illusion that FIG characters have slid vertically against each other.
     * Supersmushing continues until any sub-characters other than "|" would have to
     * be smushed. Supersmushing can produce impressive results, but it is seldom
     * possible, since other sub-characters would usually have to be considered for
     * smushing as soon as any such stacked vertical lines are encountered.
     * </p>
     */
    VERTICAL_VERTICAL_LINE_SMUSHING(1 shl 12),

    /**
     * Moves FIG characters closer together until they touch. Typographers use the
     * term "kerning" for this phenomenon when applied to the horizontal axis, but
     * fitting also includes this as a vertical behavior, for which there is
     * apparently no established typographical term.
     */
    VERTICAL_FITTING_BY_DEFAULT(1 shl 13),

    /**
     * Moves FIG characters one step closer after they touch, so that they partially
     * occupy the same space. A FIG driver must decide what sub-character to display
     * at each junction. There are two ways of making these decisions: by controlled
     * smushing or by universal smushing.
     */
    VERTICAL_SMUSHING_BY_DEFAULT(1 shl 14);

    companion object {
        fun isLayoutOptionSelected(oldOption: Int, expectedOption: Int): Boolean =
            oldOption and expectedOption != 0

        /**
         * Converts an old layout value (Legal values -1 to 63) into the equivalent full
         * layout value.
         *
         * <dl>
         * <dt>-1</dt>
         * <dd>Full-width layout by default</dd>
         * <dt>0</dt>
         * <dd>Horizontal fitting (kerning) layout by default</dd>
         * <dt>1</dt>
         * <dd>Apply horizontal smushing rule 1 by default</dd>
         * <dt>2</dt>
         * <dd>Apply horizontal smushing rule 2 by default</dd>
         * <dt>4</dt>
         * <dd>Apply horizontal smushing rule 3 by default</dd>
         * <dt>8</dt>
         * <dd>Apply horizontal smushing rule 4 by default</dd>
         * <dt>16</dt>
         * <dd>Apply horizontal smushing rule 5 by default</dd>
         * <dt>32</dt>
         * <dd>Apply horizontal smushing rule 6 by default</dd>
         * </dl>
         *
         * @param old The old layout value to convert into a full layout value.
         * @return The full layout value.
         */
        fun fullLayoutOptionFromOld(old: KLayoutOption): KLayoutOption = when (old) {
            INVALID -> NIL
            NIL -> HORIZONTAL_FITTING_BY_DEFAULT
            else -> old
        }

        fun ofOption(option: Int): KLayoutOption {
            for (value in values()) {
                if (value.option == option) {
                    return value
                }
            }
            return NIL
        }
    }
}