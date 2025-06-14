/*
 * Copyright (C) 2021-2025 The AzhagiKeys Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.azhagi.azhagikeys.lib

import android.content.Context
import com.azhagi.azhagikeys.extensionManager
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.*

/**
 * Project-specific locale class wrapping [java.util.Locale]. The wrapping is
 * necessary to provide consistent language display names and tags across the
 * whole code base.
 *
 * This class would be ideal for Kotlin's value classes, though AndroidX.Room
 * does not like this at all, so this is a "normal" class.
 *
 * To construct a AzhagiLocale, use one of the many from() methods provided.
 *
 * @see java.util.Locale
 */
@Serializable(with = AzhagiLocale.Serializer::class)
class AzhagiLocale private constructor(val base: Locale) {
    companion object {
        /** Delimiter for a language tag. */
        private const val DELIMITER_LANGUAGE_TAG = '-'
        /** Delimiter for a locale tag. */
        private const val DELIMITER_LOCALE_TAG = '_'

        /** Delimiter regex to split language/locale tags. */
        private val DELIMITER_SPLITTER = """[${DELIMITER_LANGUAGE_TAG}${DELIMITER_LOCALE_TAG}]""".toRegex()

        /** Constant locale for ROOT */
        val ROOT = from("", "", "")

        /** Constant locale for ENGLISH */
        val ENGLISH = from("en", "", "")

        /**
         * Wraps a [java.util.Locale] and returns the [AzhagiLocale].
         *
         * @return The wrapped locale.
         */
        fun from(javaLocale: Locale) = AzhagiLocale(javaLocale)

        /**
         * Constructs a new [AzhagiLocale] with given [language].
         *
         * @param language A two-letter language code.
         *
         * @return A new [AzhagiLocale].
         */
        fun from(language: String) = from(Locale(language))

        /**
         * Constructs a new [AzhagiLocale] with given [language] and [country].
         *
         * @param language A two-letter language code.
         * @param country A two-letter country code.
         *
         * @return A new [AzhagiLocale].
         */
        fun from(language: String, country: String) = from(Locale(language, country))

        /**
         * Constructs a new [AzhagiLocale] with given [language], [country] and [variant].
         *
         * @param language A two-letter language code.
         * @param country A two-letter country code.
         * @param variant A two-letter variant code.
         *
         * @return A new [AzhagiLocale].
         */
        fun from(language: String, country: String, variant: String) = from(Locale(language, country, variant))

        /**
         * Constructs a new [AzhagiLocale] from given [str].
         *
         * @param str Either a language or locale tag in string form.
         *
         * @return A new [AzhagiLocale].
         */
        fun fromTag(str: String) = when {
            str.contains(DELIMITER_SPLITTER) -> {
                val lc = str.split(DELIMITER_SPLITTER)
                if (lc.size >= 3) {
                    from(lc[0], lc[1], lc[2])
                } else {
                    from(lc[0], lc[1])
                }
            }
            else -> from(str)
        }

        /**
         * Gets the current value of the default locale for this instance of
         * the Java Virtual Machine.
         *
         * @see java.util.Locale.getDefault
         */
        fun default() = AzhagiLocale(Locale.getDefault())

        /**
         * Returns a list of all installed locales.
         *
         * @see java.util.Locale.getAvailableLocales
         */
        fun installedSystemLocales(): List<AzhagiLocale> = Locale.getAvailableLocales().map { from(it) }

        /**
         * Returns a list of all installed locales and custom locales.
         *
         */
        fun extendedAvailableLocales(context: Context): List<AzhagiLocale> {
            val systemLocales = installedSystemLocales()
            val extensionManager by context.extensionManager()
            val systemLocalesSet = buildSet {
                for (locale in systemLocales) {
                    add(locale.localeTag())
                }
            }.toSet()
            val extraLocales = buildList {
                for (languagePackExtension in extensionManager.languagePacks.value ?: listOf()) {
                    for (languagePackItem in languagePackExtension.items) {
                        val locale = languagePackItem.locale
                        if (from(locale.language, locale.country).localeTag() in systemLocalesSet) {
                            add(locale.localeTag())
                        }
                    }
                }
            }.toSet()
            return systemLocales + extraLocales.map { fromTag(it) }
        }
    }

    /**
     * Builds a locale or language tag for this locale by using [delimiter].
     *
     * @param delimiter The delimiter to use between the components.
     *
     * @return The generated tag for this locale. May be an empty string if
     *  [language], [country] and [variant] are not specified.
     */
    private fun buildLocaleString(delimiter: Char) = buildString {
        val language = base.language
        val country = base.country
        val variant = base.variant
        append(language)
        if (language.isNotBlank() && country.isNotBlank()) {
            append(delimiter)
        }
        append(country)
        if (country.isNotBlank() && variant.isNotBlank()) {
            append(delimiter)
        }
        append(variant)
    }

    /**
     * Returns the language code of this locale.
     *
     * @see java.util.Locale.getLanguage
     */
    val language: String get() = base.language

    /**
     * Returns the country/region code for this locale.
     *
     * @see java.util.Locale.getCountry
     */
    val country: String get() = base.country

    /**
     * Returns the variant code for this locale.
     *
     * @see java.util.Locale.getVariant
     */
    val variant: String get() = base.variant

    /**
     * Returns a three-letter abbreviation of this locale's language.
     *
     * @see java.util.Locale.getISO3Language
     */
    val iso3Language: String get() = base.isO3Language

    /**
     * Returns a three-letter abbreviation of this locale's country.
     *
     * @see java.util.Locale.getISO3Country
     */
    val iso3Country: String get() = base.isO3Country

    /**
     * Returns true if this language has a capitalization concept, false otherwise.
     * TODO: this is absolutely not exhaustive and hard-coded, find solution based on ICU or system
     */
    val supportsCapitalization: Boolean
        get() = when (language) {
            "zh", "ko", "th", "bn", "hi" -> false
            else -> true
        }

    /**
     * Returns true if suggestions in this language should have spaces added after, false otherwise.
     * TODO: this is absolutely not exhaustive and hard-coded, find solution based on ICU or system
     */
    val supportsAutoSpace: Boolean
        get() = when (language) {
            "zh", "ko", "jp", "th" -> false
            else -> true
        }

    /**
     * Generates the language tag for this locale in the format `xx`,
     * `xx-YY` or `xx-YY-zzz` and returns it as a string.
     *
     * xx: Two-letter language code
     * YY: Two-letter country code
     * zzz: Three letter variant
     *
     * @return The language tag for this locale. May be an empty string if
     *  [language], [country] and [variant] are not specified.
     */
    fun languageTag(): String = buildLocaleString(DELIMITER_LANGUAGE_TAG)

    /**
     * Generates the locale tag for this locale in the format `xx`,
     * `xx_YY` or `xx_YY_zzz` and returns it as a string.
     *
     * xx: Two-letter language code
     * YY: Two-letter country code
     * zzz: Three letter variant
     *
     * @return The locale tag for this locale. May be an empty string if
     *  [language], [country] and [variant] are not specified.
     */
    fun localeTag(): String = buildLocaleString(DELIMITER_LOCALE_TAG)

    /**
     * Returns the name of this locale's language, localized to [locale].
     *
     * @see java.util.Locale.getDisplayLanguage
     */
    fun displayLanguage(locale: AzhagiLocale = default()): String {
        return base.getDisplayLanguage(locale.base).titlecase(locale)
    }

    /**
     * Returns the name of this locale's country, localized to [locale].
     *
     * @see java.util.Locale.getDisplayCountry
     */
    fun displayCountry(locale: AzhagiLocale = default()): String = base.getDisplayCountry(locale.base)

    /**
     * Returns a name for the locale's variant code that is appropriate for
     * display to the user.
     *
     * @see java.util.Locale.getDisplayVariant
     */
    fun displayVariant(locale: AzhagiLocale = default()): String = base.getDisplayVariant(locale.base)

    /**
     * Returns the display name for this locale, localized to [locale] in
     * the format `Language`, `Language (Country)` or `Language (Country) \[VARIANT]`.
     *
     * @param locale The locale to use for generating the display name for
     *  this locale, or [default] if otherwise.
     *
     * @return The display name for this locale. May be an empty string if
     *  [language], [country] and [variant] are not specified.
     */
    fun displayName(locale: AzhagiLocale = default()) = buildString {
        val languageName = displayLanguage(locale).ifBlank { base.language }
        val countryName = displayCountry(locale).ifBlank { base.country }
        val variantName = displayVariant(locale).ifBlank { base.variant }
        append(languageName)
        if (countryName.isNotBlank()) {
            if (languageName.isNotBlank()) {
                append(' ')
            }
            append('(')
            append(countryName)
            append(')')
        }
        if (variantName.isNotBlank()) {
            if (languageName.isNotBlank() || countryName.isNotBlank()) {
                append(' ')
            }
            append('[')
            append(variantName.uppercase())
            append(']')
        }
    }

    /**
     * Generate a debug string representing this locale. Not to be confused
     * with [java.util.Locale.toString], which produces a locale tag. If such
     * tag is needed, use [localeTag].
     *
     * @return The debug representation of this locale.
     */
    override fun toString() = "AzhagiLocale { l=${base.language} c=${base.country} v=${base.variant} }"

    /**
     * Equality check for this locale.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AzhagiLocale

        if (base != other.base) return false

        return true
    }

    /**
     * Returns the hash code for this locale.
     */
    override fun hashCode(): Int {
        return base.hashCode()
    }

    /**
     * The JSON (de)serializer for AzhagiLocale.
     */
    class Serializer : KSerializer<AzhagiLocale> {
        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("AzhagiLocale", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: AzhagiLocale) {
            encoder.encodeString(value.languageTag())
        }

        override fun deserialize(decoder: Decoder): AzhagiLocale {
            return fromTag(decoder.decodeString())
        }
    }
}

@Suppress("NOTHING_TO_INLINE")
inline fun String.lowercase(locale: AzhagiLocale): String = this.lowercase(locale.base)

@Suppress("NOTHING_TO_INLINE")
inline fun String.uppercase(locale: AzhagiLocale): String = this.uppercase(locale.base)

@Suppress("NOTHING_TO_INLINE")
inline fun String.titlecase(locale: AzhagiLocale = AzhagiLocale.ROOT): String {
    return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale.base) else it.toString() }
}

