package app.lifelog.data.search

/** Builds a safe FTS4 MATCH expression from free text typed by the user. */
internal object FtsQuery {
    private val whitespace = Regex("\\s+")
    private const val MAX_TERMS = 8

    /**
     * Every word must match as a prefix ("den" finds "Dentist"). Operator characters and FTS
     * keywords can't get through: terms keep only letters and digits and are lower-cased
     * (FTS keywords are upper case). Returns null when nothing searchable remains.
     */
    fun from(raw: String): String? =
        raw.split(whitespace)
            .map { term -> term.filter(Char::isLetterOrDigit).lowercase() }
            .filter { it.isNotEmpty() }
            .take(MAX_TERMS)
            .takeIf { it.isNotEmpty() }
            ?.joinToString(" ") { "$it*" }
}
