package app.lifelog.feature.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

/**
 * In-app language choice (PLAN.md D8). Uses AppCompat's per-app locales, which map to the system
 * per-app language setting on Android 13+ and are stored by AppCompat on older versions
 * (see AppLocalesMetadataHolderService in the manifest). Changing it recreates the activity.
 */
object AppLanguages {

    /** Language tags with a translation in res/values-*, in the order shown to the user. */
    val supported: List<String> = listOf(
        "en", "de", "fr", "es", "it", "pt", "nl", "pl", "cs", "sv", "da", "nb", "fi",
        "ja", "ko", "zh-CN", "id", "hi",
    )

    /** The chosen language tag, or null when the app follows the system language. */
    fun current(): String? {
        val chosen = AppCompatDelegate.getApplicationLocales()
        if (chosen.isEmpty) return null
        val locale = chosen[0] ?: return null
        return supported.firstOrNull { matches(Locale.forLanguageTag(it), locale) }
    }

    fun set(tag: String?) {
        AppCompatDelegate.setApplicationLocales(
            if (tag == null) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag),
        )
    }

    /** The language's own name, e.g. "Deutsch", "français" → "Français", "日本語". */
    fun displayName(tag: String): String {
        // "zh-CN" would read "中文 (中国)"; the script name is what users look for.
        val locale = Locale.forLanguageTag(if (tag == "zh-CN") "zh-Hans" else tag)
        return locale.getDisplayName(locale).replaceFirstChar { it.titlecase(locale) }
    }

    private fun matches(supported: Locale, chosen: Locale): Boolean {
        // Android may report Indonesian as the legacy code "in".
        fun normalize(language: String) = if (language == "in") "id" else language
        if (normalize(supported.language) != normalize(chosen.language)) return false
        return supported.country.isEmpty() || supported.country == chosen.country
    }
}
