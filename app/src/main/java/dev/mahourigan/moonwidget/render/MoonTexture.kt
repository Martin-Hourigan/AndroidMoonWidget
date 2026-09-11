package dev.mahourigan.moonwidget.render

import dev.mahourigan.moonwidget.R
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory

/**
 * A real photograph of the Moon's near side, in shades of grey.
 *
 * Cropped tight to the disc and desaturated so a caller can multiply it by
 * whatever colour the current theme uses for the lit face — see
 * `ui/MoonShape.kt` and [MoonRenderer]. The shading — craters, maria, ray
 * systems — comes from the photograph; the hue is whichever colour it gets
 * tinted. Corners outside the disc are transparent, so it can be dropped
 * straight onto a disc of any radius.
 *
 * Full moon photograph by Gregory H. Revera (CC BY-SA 3.0), via Wikimedia
 * Commons: https://commons.wikimedia.org/wiki/File:FullMoon2010.jpg
 */
object MoonTexture {

    @Volatile
    private var cached: Bitmap? = null

    fun bitmap(context: Context): Bitmap {
        cached?.let { return it }
        synchronized(this) {
            cached?.let { return it }
            val loaded = BitmapFactory.decodeResource(
                context.applicationContext.resources,
                R.drawable.moon_texture,
            )
            cached = loaded
            return loaded
        }
    }
}
