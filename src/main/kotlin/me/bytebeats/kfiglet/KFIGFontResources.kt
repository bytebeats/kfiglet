package me.bytebeats.kfiglet

import java.io.IOException

/**
 * @author <a href="https://github.com/bytebeats">bytebeats</a>
 * @email <happychinapc@gmail.com>
 * @since 2020/8/2 19:48
 */
class KFIGFontResources private constructor() {
    companion object {
        const val STANDARD = "standard.flf"

        /**
         * Loads a {@link FigFont} from a resource name.
         *
         * @param resourceName
         *            The name of the resource from which to load a {@link FigFont}.
         * @return The {@link FigFont} loaded from the requested resource.
         * @throws IOException
         *             if there is problem loading a {@link FigFont} from the specified
         *             resource.
         */

        @Throws(IOException::class)
        fun loadFontResource(fontPath: String): KFIGFont? {
            KFIGFontResources::class.java.classLoader.getResourceAsStream(fontPath).use {
                return KFIGFont.loadFont(it)
            }
        }

        @JvmStatic
        fun main(args: Array<String>) {
            val renderer = loadFontResource(STANDARD)?.let { KFIGFontRenderer(it) }
            val text = "Hello, FIGlet!"
            println(text)
            renderer?.apply {
                println(renderText(text))
            }
        }
    }
}