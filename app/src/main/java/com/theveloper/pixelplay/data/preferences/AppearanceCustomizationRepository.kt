package com.theveloper.pixelplay.data.preferences

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import androidx.compose.ui.text.font.FontFamily
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * Small, process-local appearance store used by the Android 10 port.
 * Font files are copied into app-private storage so SAF permissions are not
 * required again after the user selects a font.
 */
object AppearanceCustomizationRepository {
    private const val PREFS = "appearance_customization_v1"
    private const val KEY_UI_FONT = "ui_font_path"
    private const val KEY_LYRICS_FONT = "lyrics_font_path"
    private const val KEY_PAGE_SCALE = "page_scale"

    data class State(
        val uiFontPath: String? = null,
        val lyricsFontPath: String? = null,
        val pageScale: Float = 1f
    )

    private lateinit var context: Context
    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    fun initialize(appContext: Context) {
        context = appContext.applicationContext
        reload()
    }

    fun reload() {
        if (!::context.isInitialized) return
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        _state.value = State(
            uiFontPath = p.getString(KEY_UI_FONT, null)?.takeIf { File(it).isFile },
            lyricsFontPath = p.getString(KEY_LYRICS_FONT, null)?.takeIf { File(it).isFile },
            pageScale = p.getFloat(KEY_PAGE_SCALE, 1f).coerceIn(0.8f, 1.2f)
        )
    }

    fun setPageScale(scale: Float) {
        if (!::context.isInitialized) return
        val value = scale.coerceIn(0.8f, 1.2f)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putFloat(KEY_PAGE_SCALE, value).apply()
        _state.value = _state.value.copy(pageScale = value)
    }

    fun importFont(uri: Uri, lyrics: Boolean): Boolean {
        if (!::context.isInitialized) return false
        return runCatching {
            val resolver = context.contentResolver
            val extension = when (resolver.getType(uri)?.lowercase()) {
                "font/ttf" -> ".ttf"
                "font/otf" -> ".otf"
                else -> ".ttf"
            }
            val target = File(
                File(context.filesDir, "custom_fonts").also { it.mkdirs() },
                if (lyrics) "lyrics$extension" else "ui$extension"
            )
            resolver.openInputStream(uri).use { input ->
                requireNotNull(input)
                target.outputStream().use { output -> input.copyTo(output) }
            }
            // Validate before exposing it to Compose.
            Typeface.createFromFile(target)
            val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            p.edit().putString(if (lyrics) KEY_LYRICS_FONT else KEY_UI_FONT, target.absolutePath).apply()
            reload()
            true
        }.getOrElse { false }
    }

    fun clearFont(lyrics: Boolean) {
        if (!::context.isInitialized) return
        val key = if (lyrics) KEY_LYRICS_FONT else KEY_UI_FONT
        val path = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(key, null)
        path?.let { runCatching { File(it).delete() } }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(key).apply()
        reload()
    }

    fun uiTypeface(): Typeface? =
        state.value.uiFontPath?.let { runCatching { Typeface.createFromFile(it) }.getOrNull() }

    fun lyricsTypeface(): Typeface? =
        state.value.lyricsFontPath?.let { runCatching { Typeface.createFromFile(it) }.getOrNull() }
}
