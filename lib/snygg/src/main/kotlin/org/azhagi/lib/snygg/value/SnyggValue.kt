/*
 * Copyright (C) 2025 The AzhagiKeys Contributors
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

package org.azhagi.lib.snygg.value

/**
 * SnyggValue is the base interface for all possible property values a Snygg stylesheet can hold. In general, a Snygg
 * value can be one specific type of value (e.g. a color, a keyword describing a behavior, shape, etc.).
 *
 * At no point should a Snygg value be able to have to ways of representing the same value (except whitespace which can
 * be trimmed), to guarantee that a serialized value will always correspond to the same deserialized object. Should
 * there be the need to have two different representations, two different values must be defined and declared in the
 * `supportedValues` field of a property spec.
 */
interface SnyggValue {
    /**
     * Returns the associated encoder for this Snygg value, typically the Companion of the implementing value class.
     */
    fun encoder(): SnyggValueEncoder
}

/**
 * Checks if any given Snygg value indicates that it should be inherited from the parent rule.
 */
@Suppress("NOTHING_TO_INLINE")
inline fun SnyggValue.isInherit(): Boolean {
    return this is SnyggInheritValue
}

/**
 * Convenience function which returns the opposite of SnyggValue.isInherit(). See [isInherit] for more info.
 */
@Suppress("NOTHING_TO_INLINE")
inline fun SnyggValue.isNotInherit(): Boolean {
    return !isInherit()
}

/**
 * Checks if any given Snygg value indicates that it is undefined.
 */
@Suppress("NOTHING_TO_INLINE")
inline fun SnyggValue.isUndefined(): Boolean {
    return this is SnyggUndefinedValue
}

/**
 * Convenience function which returns the opposite of SnyggValue.isUndefined(). See [isUndefined] for more info.
 */
@Suppress("NOTHING_TO_INLINE")
inline fun SnyggValue.isNotUndefined(): Boolean {
    return !isUndefined()
}

/**
 * This value defines that a property value should be copied from the parent stylesheet.
 *
 * The inherit intent was defined explicitly by the creator of the stylesheet and thus should be kept internally in the
 * stylesheet rules. This value is kept in both ways during the serialization process and is shown in the UI for a
 * stylesheet editor.
 */
object SnyggInheritValue : SnyggValue, SnyggValueEncoder {
    const val Inherit = "inherit"

    override val spec = SnyggValueSpec {
        keywords(id = Inherit, keywords = listOf(Inherit))
    }

    override fun defaultValue() = this

    override fun serialize(v: SnyggValue) = runCatching<String> {
        return@runCatching Inherit
    }

    override fun deserialize(v: String) = runCatching<SnyggValue> {
        check(v.trim() == Inherit)
        return@runCatching SnyggInheritValue
    }

    override fun encoder() = this
}

/**
 * This value defines that a property value is undefined.
 *
 * The undefined intent was defined implicitly by the serialization process (when a required property was missing for a
 * rule). This value is thus **not** meant to be serialized, the serialization of this value is indeed prohibited and
 * an attempt to serialize this value will result in a serialization failure. Additionally, this value should be shown
 * as "Undefined" in a stylesheet editor UI.
 */
object SnyggUndefinedValue : SnyggValue, SnyggValueEncoder {
    private const val Undefined = "undefined"

    override val spec = SnyggValueSpec {
        keywords(id = Undefined, keywords = listOf(Undefined))
    }

    override fun defaultValue() = this

    override fun serialize(v: SnyggValue) = runCatching<String> {
        error("Undefined is not meant to be serialized")
    }

    override fun deserialize(v: String) = runCatching<SnyggValue> {
        error("Undefined is not meant to be deserialized")
    }

    override fun encoder() = this
}

/**
 * A simple yes value (useful for boolean style value).
 */
object SnyggYesValue : SnyggValue, SnyggValueEncoder {
    const val Yes = "yes"

    override val spec = SnyggValueSpec {
        keywords(id = Yes, keywords = listOf(Yes))
    }

    override fun defaultValue() = this

    override fun serialize(v: SnyggValue) = runCatching<String> {
        return@runCatching Yes
    }

    override fun deserialize(v: String) = runCatching<SnyggValue> {
        check(v.trim() == Yes)
        return@runCatching SnyggYesValue
    }

    override fun encoder() = this
}

/**
 * A simple no value (useful for boolean style value).
 */
object SnyggNoValue : SnyggValue, SnyggValueEncoder {
    const val No = "no"

    override val spec = SnyggValueSpec {
        keywords(id = No, keywords = listOf(No))
    }

    override fun defaultValue() = this

    override fun serialize(v: SnyggValue) = runCatching<String> {
        return@runCatching No
    }

    override fun deserialize(v: String) = runCatching<SnyggValue> {
        check(v.trim() == No)
        return@runCatching SnyggNoValue
    }

    override fun encoder() = this
}
