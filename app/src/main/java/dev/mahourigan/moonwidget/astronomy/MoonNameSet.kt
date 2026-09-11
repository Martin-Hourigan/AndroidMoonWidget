package dev.mahourigan.moonwidget.astronomy

/**
 * Which tradition the monthly moon names come from.
 *
 * No display text here — the names themselves are string resources, so they
 * stay translatable and this package stays free of Android dependencies.
 * See `ui/DisplayStrings.kt`.
 */
enum class MoonNameSet {
    /**
     * The Farmers' Almanac list: Wolf, Snow, Worm and so on.
     *
     * Commonly billed as "Native American", though it is really a mixture of
     * Algonquin, colonial English and European folk names, and the attribution
     * is contested. The app does not repeat that claim.
     */
    ALMANAC,

    /**
     * Anglo-Saxon month names recorded by Bede in *De temporum ratione* (725 CE).
     *
     * The best-attested set here. Bede reuses two names — Gēola for December
     * and January, Līða for June and July — so the "ærra/æftera" (former and
     * latter) prefixes are needed to keep twelve distinct labels.
     */
    OLD_ENGLISH,

    /**
     * The widely circulated "Celtic" list: Quiet Moon, Moon of Ice and so on.
     *
     * Popular, but no historical provenance could be found for it — no
     * manuscript source, and published versions disagree with each other. Most
     * likely a modern neopagan construction, and labelled as such in the UI
     * rather than presented as ancient.
     */
    CELTIC,

    /**
     * The "also known as" names that sit beside the Almanac list in most
     * references: Storm, Hunger, Sap, Egg and so on.
     *
     * A collection of well-attested alternates rather than one single
     * tradition.
     */
    ALTERNATIVE,
    ;

    companion object {
        val DEFAULT = ALMANAC

        fun fromNameOrDefault(name: String?): MoonNameSet =
            entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}
