package lt.marcomultiservices.kimba.core

enum class Lang { LT, DE, EN }

/** One piece of player-facing text in every supported language. */
class L(val lt: String, val de: String, val en: String) {
    operator fun get(lang: Lang): String = when (lang) {
        Lang.LT -> lt
        Lang.DE -> de
        Lang.EN -> en
    }
}
